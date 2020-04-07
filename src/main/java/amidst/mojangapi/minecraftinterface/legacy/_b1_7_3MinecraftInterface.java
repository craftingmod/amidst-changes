package amidst.mojangapi.minecraftinterface.legacy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Random;

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
	private final SymbolicClass chunkProviderGeneratorClass;
    
    private final RecognisedVersion recognisedVersion;
    
    private Object worldChunkManager;
    
    private Object chunkProviderGenerator;
    
    private Method getBiomeAtMethod;
    
    private Method produceNoiseMethod;
    
    private Field randomField;
    
    private Field biomeColorField;
    
    private Field tempField;
    
    public _b1_7_3MinecraftInterface(Map<String, SymbolicClass> symbolicClassMap, RecognisedVersion recognisedVersion) {
    	this.worldClass = symbolicClassMap.get(CLASS_WORLD);
    	this.worldChunkManagerClass = symbolicClassMap.get(CLASS_WORLD_CHUNK_MANAGER);
    	this.biomeGenBaseClass = symbolicClassMap.get(CLASS_BIOME_GEN_BASE);
    	this.iSaveHandlerClass = symbolicClassMap.get(CLASS_I_SAVE_HANDLER);
    	this.chunkProviderGeneratorClass = symbolicClassMap.get(CLASS_CHUNK_PROVIDER_GENERATOR);
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
                	byte[] chunk = provideChunk((x + x0) >> 4, (y + y0) >> 4);
                	int num = 0;
                    for (int i = 0; i < w; i++) {
                        for (int j = 0; j < h; j++) {
                        	
                        	int id = 0;
                        	byte block = chunk[i + j * 16];
                        	
	                        if (block == 9 /* water block ID */) {
	                    		id = ocean;
	                    	} else if (block == 79 /* ice block ID */) {
                        		id = frozenOcean;
                        	}  else {
                        		id = getBiomeIdFromColor((int) biomeColorField.get(biomes[num]));
                        	}
                        	
                            array[(x0 + i) + (y0 + j) * width] = id;
                            num++;
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
			chunkProviderGenerator = chunkProviderGeneratorClass.getConstructor(CONSTRUCTOR_CHUNK_PROVIDER_GENERATOR).getRawConstructor().newInstance(world, seed);
			randomField = chunkProviderGeneratorClass.getField(FIELD_CHUNK_PROVIDER_GENERATOR_RANDOM).getRawField();
			biomeColorField = biomeGenBaseClass.getField(FIELD_BIOME_GEN_BASE_COLOR).getRawField();
			tempField = worldChunkManagerClass.getField(FIELD_WORLD_CHUNK_MANAGER_TEMPERATURE).getRawField();
			produceNoiseMethod = chunkProviderGeneratorClass.getMethod(METHOD_CHUNK_PROVIDER_GENERATOR_PRODUCE_NOISE).getRawMethod();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new MinecraftInterfaceException("unable to create world", e);
		}
	}
	
	public byte[] provideChunk(int chunkX, int chunkY) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Random rand = (Random) randomField.get(chunkProviderGenerator);
        rand.setSeed((long)chunkX * 0x4f9939f508L + (long)chunkY * 0x1ef1565bd5L);
        double[] temperatures = (double[]) tempField.get(worldChunkManager);
        byte[] blockBytes = new byte[16 * 16];
        generateTerrain(chunkX, chunkY, blockBytes, temperatures);
        return blockBytes;
	}
	
	private Object createNullSaveHandler() {
		// we create a SaveHandler that returns null for any invocation of its methods, so we don't have to create temporary files anywhere
		Class<?> nbsInterface = iSaveHandlerClass.getClazz();
		return Proxy.newProxyInstance(nbsInterface.getClassLoader(), new Class<?>[]{ nbsInterface }, (p,m,a) -> null);
	}
	
	private double[] noiseDoubleArray;
	
	public void generateTerrain(int i, int j, byte blocks[], double ad[]) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		byte byte0 = 4;
		byte byte1 = 64;
		int k = byte0 + 1;
		byte byte2 = 17;
		int l = byte0 + 1;
		noiseDoubleArray = (double[]) produceNoiseMethod.invoke(chunkProviderGenerator, noiseDoubleArray, i * byte0, 0, j * byte0, k, byte2, l);
		
		for (int i1 = 0; i1 < byte0; i1++) {
			for (int j1 = 0; j1 < byte0; j1++) {
				double d = 0.125D;
				double d1 = noiseDoubleArray[((i1 + 0) * l + (j1 + 0)) * byte2 + (7 + 0)];
				double d2 = noiseDoubleArray[((i1 + 0) * l + (j1 + 1)) * byte2 + (7 + 0)];
				double d3 = noiseDoubleArray[((i1 + 1) * l + (j1 + 0)) * byte2 + (7 + 0)];
				double d4 = noiseDoubleArray[((i1 + 1) * l + (j1 + 1)) * byte2 + (7 + 0)];
				double d5 = (noiseDoubleArray[((i1 + 0) * l + (j1 + 0)) * byte2 + (7 + 1)] - d1) * d;
				double d6 = (noiseDoubleArray[((i1 + 0) * l + (j1 + 1)) * byte2 + (7 + 1)] - d2) * d;
				double d7 = (noiseDoubleArray[((i1 + 1) * l + (j1 + 0)) * byte2 + (7 + 1)] - d3) * d;
				double d8 = (noiseDoubleArray[((i1 + 1) * l + (j1 + 1)) * byte2 + (7 + 1)] - d4) * d;
				
				double d9 = 0.25D;
				double d10 = d1;
				double d11 = d2;
				double d12 = (d3 - d1) * d9;
				double d13 = (d4 - d2) * d9;
				for (int i2 = 0; i2 < 4; i2++) {
					double d14 = 0.25D;
					double d15 = d10;
					double d16 = (d11 - d10) * d14;
					for (int k2 = 0; k2 < 4; k2++) {
						int j2 = (i1 * 4 + i2) * 16 + (j1 * 4 + k2);
						double d17 = ad[j2];
						int l2 = 0;
						if (d17 < 0.5D && 63 >= byte1 - 1) {
							l2 = 4; // 3 = ice
						} else {
							l2 = 3; // 2 = water
						}
						if (d15 > 0.0D) {
							l2 = 1; // 1 = stone
						}
						blocks[j2] = (byte) l2;
						d15 += d16;
					}
					
					d10 += d12;
					d11 += d13;
				}
				
				d1 += d5;
				d2 += d6;
				d3 += d7;
				d4 += d8;
				
			}
			
		}
		
	}
	
	@Override
	public RecognisedVersion getRecognisedVersion() {
		return recognisedVersion;
	}
	
}
