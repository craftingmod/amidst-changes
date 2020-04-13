package amidst.mojangapi.minecraftinterface.legacy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import amidst.clazz.symbolic.SymbolicClass;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.WorldType;

import static amidst.mojangapi.world.biome.Biome.*;

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
    
    private Field biomeColorField;
    
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
            final int chunkSize = 16;
            for (int x0 = 0; x0 < width; x0 += chunkSize) {
                int w = Math.min(chunkSize, width - x0);

                for (int y0 = 0; y0 < height; y0 += chunkSize) {
                    int h = Math.min(chunkSize, height - y0);
                    
                	Object[] biomes = (Object[]) getBiomeAtMethod.invoke(worldChunkManager, (x + x0), (y + y0), chunkSize, chunkSize);
                	int num = 0;
                    for (int i = 0; i < w; i++) {
                        for (int j = 0; j < h; j++) {
                        	array[(x0 + i) + (y0 + j) * width] = getBiomeIdFromColor((int) biomeColorField.get(biomes[num++]));
                        }
                    }
                }
            }
			/*if (useQuarterResolution) {
				for(int i = 0; i < width; i++) {
					for(int j = 0; j < height; j++) {
						SymbolicObject biomeGen = new SymbolicObject(biomeGenBaseClass, getBiomeAtMethod.invoke(worldChunkManager, -(x + i) >> 1, (y + j) >> 1));
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
			}*/
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new MinecraftInterfaceException("unable to get biome data", e);
		}
		return array;
	}
	
	private int getBiomeIdFromColor(int color) {
		switch(color) {
			case 0x8fa36:
				return rainforestOld;
			case 0x7f9b2:
				return swamplandOld;
			case 0x9be023:
				return seasonalForestOld;
			case 0x56621:
				return forestOld;
			case 0xd9e023:
				return savannaOld;
			case 0xa1ad20:
				return shrublandOld;
			case 0x2eb153:
				return taigaOld;
			case 0xfa9418:
				return desertOld;
			case 0xffd910:
				return plainsOld;
			case 0xffed93:
				return iceDesertOld;
			case 0x57ebf9:
				return tundraOld;
			case 0xff0000:
				return hellOld;
			case 0x8080ff:
				return skyOld;
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
			getBiomeAtMethod = worldChunkManagerClass.getMethod(METHOD_WORLD_CHUNK_MANAGER_GET_BIOMES).getRawMethod();
			biomeColorField = biomeGenBaseClass.getField(FIELD_BIOME_GEN_BASE_COLOR).getRawField();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new MinecraftInterfaceException("unable to create world", e);
		}
	}
	
	private Object createNullSaveHandler() {
		// we create a SaveHandler that returns null for any invocation of its methods, so we don't have to create temporary files anywhere
		Class<?> nbsInterface = iSaveHandlerClass.getClazz();
		return Proxy.newProxyInstance(nbsInterface.getClassLoader(), new Class<?>[]{ nbsInterface }, (p,m,a) -> null);
	}
	
	@Override
	public RecognisedVersion getRecognisedVersion() {
		return recognisedVersion;
	}
	
}
