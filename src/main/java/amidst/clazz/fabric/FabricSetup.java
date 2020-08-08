package amidst.clazz.fabric;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import amidst.logging.AmidstLogger;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.game.GameProviders;
import net.fabricmc.loader.launch.common.FabricLauncher;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.common.FabricMixinBootstrap;
import net.fabricmc.loader.launch.knot.Knot;

public enum FabricSetup {
	;
	private static final EnvType ENVIRONMENT_TYPE = EnvType.CLIENT;
	private static final boolean DEVELOPMENT = false;
	private static final boolean DEBUG_LOGGING = true;
	private static final boolean COMPATABILITY_CLASSLOADER = false;
	@SuppressWarnings("unused")
	private static final String MAPPINGS_NAMESPACE = "official";
	
	public static Object[] initAndGetObjects(URLClassLoader ucl, Path clientJarPath, String... args) throws Throwable {
		
		if (DEBUG_LOGGING) Configurator.setAllLevels(LogManager.ROOT_LOGGER_NAME, Level.DEBUG);
		
		List<GameProvider> providers = GameProviders.create();
		GameProvider provider = null;
		
		for (GameProvider p : providers) {
			if (p.locateGame(ENVIRONMENT_TYPE, ucl)) {
				provider = p;
				break;
			}
		}
		
		if (provider != null) {
			AmidstLogger.info("[FabricSetup] Loading for game " + provider.getGameName() + " " + provider.getRawGameVersion());
		} else {
			AmidstLogger.error("[FabricSetup] Could not find valid game provider!");
			for (GameProvider p : providers) {
				AmidstLogger.error("- " + p.getGameName()+ " " + p.getRawGameVersion());
			}
			throw new RuntimeException("Could not find valid game provider!");
		}
		
		provider.acceptArguments(args);
		
		// Reflect classloader instance
		String classLoaderName = COMPATABILITY_CLASSLOADER || provider.requiresUrlClassLoader() ? "net.fabricmc.loader.launch.knot.KnotCompatibilityClassLoader" : "net.fabricmc.loader.launch.knot.KnotClassLoader";
		Constructor<?> constructor = Class.forName(classLoaderName).getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		ClassLoader classLoader = (ClassLoader) constructor.newInstance(DEVELOPMENT, ENVIRONMENT_TYPE, provider);
		
		setProperties(new HashMap<>());
		
		Knot knot = createKnotInstance(ENVIRONMENT_TYPE, clientJarPath.toFile()); // DON'T CALL INIT ON THIS!!!
		
		setKnotVars(knot, classLoader, DEVELOPMENT, provider);
		
		// Merge classloader into new KnotClassLoader
		try {		    
			for (URL url : ucl.getURLs()) { // given class loader
				String urlString = url.toString().toLowerCase();
				if (!isInSystemClassPath(url) && !urlString.contains("fabric") && !urlString.contains("mixin") && !urlString.contains("asm") && !urlString.contains("log4j")) {
					knot.propose(url);
				} else {
					AmidstLogger.debug("Rejected URL: " + url);
				}
			}
			
		} catch (Throwable e) {
			AmidstLogger.error("Unable to add URLs to classpath");
		}
		
		if (provider.isObfuscated()) {
			for (Path path : provider.getGameContextJars()) {
				deobfuscate(
					provider.getGameId(),
					provider.getNormalizedGameVersion(),
					provider.getLaunchDirectory(),
					path,
					knot
				);
			}
		}
		
		Path intermediaryJarPath = provider.getLaunchDirectory().resolve(".fabric" + File.separatorChar + "remappedJars" + File.separatorChar + provider.getGameId() + "-" + provider.getRawGameVersion() + File.separatorChar + knot.getTargetNamespace() + "-" + provider.getRawGameVersion() + ".jar").toAbsolutePath();
		
		// Locate entrypoints before switching class loaders
		provider.getEntrypointTransformer().locateEntrypoints(knot);
		
		// This doesn't actually switch the classloader, this only does something if
		// getContextClassLoader() gets called on the same thread somewhere else
		Thread.currentThread().setContextClassLoader(classLoader);
		
		@SuppressWarnings("deprecation")
		FabricLoader loader = FabricLoader.INSTANCE;
		loader.setGameProvider(provider);
		loader.load();
		loader.freeze();
		
		loader.getAccessWidener().loadFromMods();

		MixinBootstrap.init();
		
		MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
		if (DEBUG_LOGGING) env.setOption(MixinEnvironment.Option.DEBUG_VERBOSE, true);
		
		FabricMixinBootstrap.init(ENVIRONMENT_TYPE, loader);
		finishMixinBootstrapping();
		
		initializeTransformers(classLoader);
		
		EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
		loadEntrypoint(provider, ENVIRONMENT_TYPE, classLoader); // This is done to imitate what fabric loader would be doing at
																 // this stage. After invoking the preLaunch entrypoint, it loads
																 // the main Minecraft class and invokes the main, where the main
																 // entrypoint would have been invoked naturally.
		
		EntrypointUtils.invoke("main", ModInitializer.class, ModInitializer::onInitialize);
		//EntrypointUtils.invoke("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
		//EntrypointUtils.invoke("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
		
		return new Object[] { classLoader, intermediaryJarPath };
	}
	
	private static void setProperties(Map<String, Object> properties) throws Throwable {
		Method m = FabricLauncherBase.class.getDeclaredMethod("setProperties", Map.class);
		m.setAccessible(true);
		m.invoke(null, properties);
	}
	
	private static void initializeTransformers(ClassLoader classLoader) throws Throwable {
		Method m1 = classLoader.getClass().getDeclaredMethod("getDelegate");
		m1.setAccessible(true);
		Object delegate = m1.invoke(classLoader);
		
		Method m2 = delegate.getClass().getDeclaredMethod("initializeTransformers");
		m2.setAccessible(true);
		m2.invoke(delegate);
	}
	
	private static Knot createKnotInstance(EnvType type, File gameJarFile) throws Throwable {
		Constructor<?> constructor = Knot.class.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		return (Knot) constructor.newInstance(type, gameJarFile);
	}
	
	private static void deobfuscate(String gameId, String gameVersion, Path gameDir, Path jarFile, FabricLauncher launcher) throws Throwable {
		Method m = FabricLauncherBase.class.getDeclaredMethod("deobfuscate", String.class, String.class, Path.class, Path.class, FabricLauncher.class);
		m.setAccessible(true);
		m.invoke(null, gameId, gameVersion, gameDir, jarFile, launcher);
	}
	
	private static void finishMixinBootstrapping() throws Throwable{
		Method m = FabricLauncherBase.class.getDeclaredMethod("finishMixinBootstrapping");
		m.setAccessible(true);
		m.invoke(null);
	}
	
	private static void setKnotVars(Knot knot, ClassLoader classLoader, boolean isDevelopment, GameProvider provider) throws Throwable {
		Field f1 = Knot.class.getDeclaredField("classLoader");
		Field f2 = Knot.class.getDeclaredField("isDevelopment");
		Field f3 = Knot.class.getDeclaredField("provider");
		
		f1.setAccessible(true);
		f2.setAccessible(true);
		f3.setAccessible(true);
		
		f1.set(knot, classLoader);
		f2.setBoolean(knot, isDevelopment);
		f3.set(knot, provider);
	}
	
	private static void loadEntrypoint(GameProvider provider, EnvType envType, ClassLoader loader) throws Throwable {
		String targetClass = provider.getEntrypoint();
		
		if (envType == EnvType.CLIENT && targetClass.contains("Applet")) {
			targetClass = "net.fabricmc.loader.entrypoint.applet.AppletMain";
		}
		
		if(DEBUG_LOGGING) AmidstLogger.debug("Loading GameProvider entrypoint: " + targetClass);
		loader.loadClass(targetClass);
	}
	
	private static boolean isInSystemClassPath(URL url) {
		for (URL classpathUrl : ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()) {
			if (classpathUrl.sameFile(url)) {
				return true;
			}
		}
		return false;
	}
	
}
