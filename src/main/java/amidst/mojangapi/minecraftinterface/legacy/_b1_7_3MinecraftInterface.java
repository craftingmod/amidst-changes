package amidst.mojangapi.minecraftinterface.legacy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import amidst.clazz.symbolic.SymbolicClass;
import amidst.clazz.symbolic.SymbolicObject;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.WorldType;

import static amidst.mojangapi.minecraftinterface.legacy._b1_7_3SymbolicNames.*;

public class _b1_7_3MinecraftInterface implements MinecraftInterface {
    public static final RecognisedVersion LAST_COMPATIBLE_VERSION = RecognisedVersion._b1_7_3;
    
	private final SymbolicClass worldClass;
	private final SymbolicClass worldChunkManagerClass;
	private final SymbolicClass biomeGenBaseClass;
	private final SymbolicClass iSaveHandlerClass;
    
    private final RecognisedVersion recognisedVersion;
    
    private Object worldChunkManager;
    
    private Method getBiomeAtMethod;
    
    public _b1_7_3MinecraftInterface(Map<String, SymbolicClass> symbolicClassMap, RecognisedVersion recognisedVersion) {
    	this.worldClass = symbolicClassMap.get(CLASS_WORLD);
    	this.worldChunkManagerClass = symbolicClassMap.get(CLASS_WORLD_CHUNK_MANAGER);
    	this.biomeGenBaseClass = symbolicClassMap.get(CLASS_BIOME_GEN_BASE);
    	this.iSaveHandlerClass = symbolicClassMap.get(CLASS_I_SAVE_HANDLER);
    	this.recognisedVersion = recognisedVersion;
    	
    }

	@Override
	public int[] getBiomeData(int x, int y, int width, int height, boolean useQuarterResolution)
			throws MinecraftInterfaceException {
		int[] array = new int[width * height];
		
		try {
			if (useQuarterResolution) {
				for(int i = 0; i < width; i++) {
					for(int j = 0; j < height; j++) {
						SymbolicObject biomeGen = new SymbolicObject(biomeGenBaseClass, getBiomeAtMethod.invoke(worldChunkManager, (x + i) >> 0, (y + j) >> 0));
						array[i + j * width] = getBiomeIdFromColor((int) biomeGen.getFieldValue(FIELD_BIOME_GEN_BASE_COLOR));
					}
				}
			} else {
				for(int i = 0; i < width; i++) {
					for(int j = 0; j < height; i++) {
						SymbolicObject biomeGen = new SymbolicObject(biomeGenBaseClass, getBiomeAtMethod.invoke(worldChunkManager, x + i, y + j));
						array[i + j * width] = getBiomeIdFromColor((int) biomeGen.getFieldValue(FIELD_BIOME_GEN_BASE_COLOR));
					}
				}
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new MinecraftInterfaceException("unable to get biome data", e);
		}
		return array;
	}
	
	public int getBiomeIdFromColor(int color) {
		switch(color) {
			case 0x8fa36:
				return 21; // Rainforest -> Jungle
			case 0x7f9b2:
				return 6; // Swampland
			case 0x9be023:
				return 18; // Seasonal Forest -> Forest Hills
			case 0x56621:
				return 4; // Forest
			case 0xd9e023:
				return 35; // Savanna
			case 0xa1ad20:
				return 35; // Shrubland is exactly the same as Savanna
			case 0x2eb153:
				return 5; // Taiga
			case 0xfa9418:
				return 2; // Desert
			case 0xffd910:
				return 1; // Plains
			case 0xffed93:
				return 130; // Ice Desert -> Desert M
			case 0x57ebf9:
				return 12; // Tundra -> Snowy Tundra
			case 0xff0000:
				return 8; // Hell -> Nether
			case 0x8080ff:
				return 9; // Sky -> The End
			default:
				return 0;
		}
	}

	@Override
	public void createWorld(long seed, WorldType worldType, String generatorOptions)
			throws MinecraftInterfaceException {
		try {
			Object world = worldClass.getConstructor(CONSTRUCTOR_WORLD).getRawConstructor().newInstance(createNullSaveHandler(), "", seed);
			worldChunkManager = worldChunkManagerClass.getConstructor(CONSTRUCTOR_WORLD_CHUNK_MANAGER).getRawConstructor().newInstance(world);
			getBiomeAtMethod = worldChunkManagerClass.getMethod(METHOD_WORLD_CHUNK_MANAGER_GET_BIOME_GEN_AT).getRawMethod();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new MinecraftInterfaceException("unable to create world", e);
		}
	}
	
	private Object createNullSaveHandler() {
		Class<?> nbsInterface = iSaveHandlerClass.getClazz();
		
		return Proxy.newProxyInstance(nbsInterface.getClassLoader(), new Class<?>[]{nbsInterface}, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				return null;
			}
		});
	}

	@Override
	public RecognisedVersion getRecognisedVersion() {
		return recognisedVersion;
	}
	
}
