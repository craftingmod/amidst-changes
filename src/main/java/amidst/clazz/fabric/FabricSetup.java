package amidst.clazz.fabric;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import amidst.logging.AmidstLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.entrypoint.minecraft.hooks.EntrypointUtils;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.game.GameProviders;
import net.fabricmc.loader.launch.common.FabricLauncher;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.fabricmc.loader.launch.common.FabricMixinBootstrap;
import net.fabricmc.loader.launch.common.MappingConfiguration;
import net.fabricmc.loader.launch.knot.Knot;
import net.fabricmc.loader.metadata.EntrypointMetadata;

public enum FabricSetup {
	;
	private static final EnvType ENVIRONMENT_TYPE = EnvType.CLIENT;
	private static final boolean DEVELOPMENT = false;
	private static final boolean DEBUG_LOGGING = true;
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
		Constructor<?> constructor = Class.forName("net.fabricmc.loader.launch.knot.KnotClassLoader").getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		ClassLoader classLoader = (ClassLoader) constructor.newInstance(DEVELOPMENT, ENVIRONMENT_TYPE, provider);
		
		// Merge classloaders into new KnotClassLoader
		try {
		    Method method = Class.forName("net.fabricmc.loader.launch.knot.KnotClassLoaderInterface").getDeclaredMethod("addURL", URL.class);
		    method.setAccessible(true);
		    
			for (URL url : ucl.getURLs()) { // given class loader
				method.invoke(classLoader, url);
			}
			
			for (URL url : ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()) { // app class loader
				String urlString = url.toString().toLowerCase();
				if (!urlString.contains("fabric") && !urlString.contains("mixin")) { // this makes sure that when we call EntrypointUtils.invoke we don't get a ClassCastException
					method.invoke(classLoader, url);
				}
			}
		} catch (Throwable e) {
			AmidstLogger.error("Unable to add URLs to classpath");
		}
		
		setProperties(new HashMap<>());
		
		Knot knot = createKnotInstance(ENVIRONMENT_TYPE, clientJarPath.toFile()); // DON'T CALL INIT ON THIS!!!
		
		setKnotVars(knot, classLoader, DEVELOPMENT, provider);
		
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
		
		// Locate entrypoints before switching class loaders
		provider.getEntrypointTransformer().locateEntrypoints(knot);
		
		// This doesn't actually switch the classloader, this only does something if
		// getContextClassLoader() gets called on the same thread somewhere else
		Thread.currentThread().setContextClassLoader(classLoader);
		
		@SuppressWarnings("deprecation")
		FabricLoader loader = FabricLoader.INSTANCE;
//		setMappingsNamespace(MAPPINGS_NAMESPACE, loader);
		loader.setGameProvider(provider);
		loader.load();
		loader.freeze();
		addDummyToEntrypointStorage(loader);
		
		loader.getAccessWidener().loadFromMods();
		
		Mixins.registerErrorHandlerClass("amidst.clazz.fabric.AmidstMixinErrorHandler");

		MixinBootstrap.init();
		
		MixinEnvironment env = MixinEnvironment.getDefaultEnvironment();
		
		if (DEBUG_LOGGING) env.setOption(MixinEnvironment.Option.DEBUG_VERBOSE, true);
//		System.setProperty("mixin.env.disableRefMap", "true");
//		env.setOption(MixinEnvironment.Option.DISABLE_REFMAP, true);
//		env.setOption(MixinEnvironment.Option.REFMAP_REMAP, true);
//		System.setProperty("mixin.env.remapRefMap", "true");
		
//		TinyRemapper remapper = TinyRemapper.newRemapper()
//									.withMappings(
//										TinyRemapperMappingsHelper.create(
//											FabricLauncherBase.getLauncher().getMappingConfiguration().getMappings(),
//											MAPPINGS_NAMESPACE,
//											knot.getTargetNamespace()
//										)
//									)
//									.rebuildSourceFilenames(true)
//									.ignoreFieldDesc(true)
//									.build();
		
		Path intermediaryJarPath = loader.getGameDir().resolve(".fabric" + File.separatorChar + "remappedJars" + File.separatorChar + provider.getGameId() + "-" + provider.getRawGameVersion() + File.separatorChar + knot.getTargetNamespace() + "-" + provider.getRawGameVersion() + ".jar").toAbsolutePath();
//		remapper.readInputs(intermediaryJarPath);
		
//		env.getRemappers().add(new RemapperAdapter(remapper.getRemapper()) {});
		
//		env.setObfuscationContext(MAPPINGS_NAMESPACE);
		
		FabricMixinBootstrap.init(ENVIRONMENT_TYPE, loader);
		finishMixinBootstrapping();
		
		initializeTransformers(classLoader);
		
		// We have to load a dummy class with a dummy entrypoint before anything
		// else so the ClassLoader doesn't recurse and cause a LinkageError during
		// mixin initialization
		EntrypointUtils.invoke("dummy", Class.forName("amidst.clazz.fabric.FabricSetup$DummyEntrypoint", true, classLoader), d -> {});
		
		EntrypointUtils.invoke("preLaunch", PreLaunchEntrypoint.class, PreLaunchEntrypoint::onPreLaunch);
		EntrypointUtils.invoke("main", ModInitializer.class, ModInitializer::onInitialize);
		EntrypointUtils.invoke("client", ClientModInitializer.class, ClientModInitializer::onInitializeClient);
		
		//MixinBootstrap.getPlatform().inject();
		
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
	
	@SuppressWarnings("unchecked")
	private static void addDummyToEntrypointStorage(FabricLoader loader) throws Throwable {
		EntrypointMetadata dummyMetadata = new EntrypointMetadata() {
			public String getAdapter() { return "default"; }
			public String getValue() { return "amidst.clazz.fabric.FabricSetup$DummyClass"; }
		};
		
		Field f1 = FabricLoader.class.getDeclaredField("entrypointStorage");
		f1.setAccessible(true);
		Object entrypointStorage = f1.get(loader);
		
		Class<?> esClass = Class.forName("net.fabricmc.loader.EntrypointStorage");
		Method m1 = esClass.getDeclaredMethod("getOrCreateEntries", String.class);
		m1.setAccessible(true);
		List<Object> entryList = (List<Object>) m1.invoke(entrypointStorage, "dummy");
		
		Field f2 = FabricLoader.class.getDeclaredField("adapterMap");
		f2.setAccessible(true);
		Map<String, LanguageAdapter> adapterMap = (Map<String, LanguageAdapter>) f2.get(loader);
		
		Class<?> neClass = Class.forName("net.fabricmc.loader.EntrypointStorage$NewEntry");
		Constructor<?> c1 = neClass.getDeclaredConstructor(ModContainer.class, LanguageAdapter.class, String.class);
		c1.setAccessible(true);
		entryList.add((Object) c1.newInstance(null, adapterMap.get(dummyMetadata.getAdapter()), dummyMetadata.getValue()));
	}
	
	private static void modifyMappingConfiguration() throws Throwable {
		MappingConfiguration.class.getDeclaredField("checkedMappings");
	}
	
	@SuppressWarnings("unused")
	private static void setMappingsNamespace(String namespace, FabricLoader loader) throws Throwable { // RUN THIS ONLY AFTER KNOT HAS BEEN CREATED SO GETLAUNCHER RETURNS CORRECTLY
		Constructor<?> c1 = Class.forName("net.fabricmc.loader.FabricMappingResolver").getDeclaredConstructor(Supplier.class, String.class);
		c1.setAccessible(true);
		MappingResolver newInstance = (MappingResolver) c1.newInstance((Supplier<?>) FabricLauncherBase.getLauncher().getMappingConfiguration()::getMappings, namespace);
		
		Field f1 = FabricLoader.class.getDeclaredField("mappingResolver");
		f1.setAccessible(true);
		f1.set(loader, newInstance);
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	private static String getOfficialMappings(FabricLoader loader, GameProvider provider) throws Throwable {
		Path mappingsFile = loader.getGameDir().resolve(".mixin.out" + File.separatorChar + "mappings" + File.separatorChar + "official-" + provider.getRawGameVersion() + ".txt");
		String mappingsFileString = mappingsFile.toAbsolutePath().toString();
		
		if (Files.exists(mappingsFile)) {
			AmidstLogger.info("Official mappings found at " + mappingsFileString);
			
		} else {
			Files.createDirectories(mappingsFile.getParent());
			AmidstLogger.info("Downloading official mappings...");
			String versionManifest = IOUtils.toString(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"), (Charset) null);
			JSONObject manifestRoot = (JSONObject) JSONValue.parse(versionManifest);
			JSONArray versions = (JSONArray) manifestRoot.get("versions");
			
			JSONObject correctVersion = null;
			for (JSONObject version : (List<JSONObject>) versions) {
				if (((String) version.get("id")).equals(provider.getRawGameVersion())) {
					correctVersion = version;
					break;
				}
			}
			
			String versionMeta = IOUtils.toString(new URL((String) correctVersion.get("url")), (Charset) null);
			JSONObject metaRoot = (JSONObject) JSONValue.parse(versionMeta);
			String mappingsUrl = (String) ((JSONObject) ((JSONObject) metaRoot.get("downloads")).get("client_mappings")).get("url");
			
			ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(mappingsUrl).openStream());
			try (FileOutputStream fileOutputStream = new FileOutputStream(mappingsFileString)) {
				fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
			}
			
			AmidstLogger.info("Official mappings saved to " + mappingsFileString);
			
		}
		return mappingsFileString;
	}
	
	
	
	public static interface DummyEntrypoint {}
	public static class DummyClass implements DummyEntrypoint {}
	
}
