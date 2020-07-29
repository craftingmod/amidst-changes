package amidst.mojangapi.minecraftinterface.local;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.LongFunction;

import amidst.clazz.symbolic.SymbolicClass;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.WorldType;
import amidst.util.ArrayCache;

public class LocalMinecraftInterface implements MinecraftInterface {
    private boolean isInitialized = false;
    private boolean areExecutorsStopped = false;
	private final RecognisedVersion recognisedVersion;

	private final SymbolicClass utilClass;
	private final SymbolicClass biomeLayersClass;
	private final SymbolicClass layerOperatorClass;
	private final SymbolicClass layerSamplerClass;
	private final SymbolicClass layerFactoryClass;
	private final SymbolicClass layerSampleContextClass;
	private final SymbolicClass perlinNoiseSamplerClass;
	
	private MethodHandle layerSamplerSampleMethod;
	private MethodHandle layerOperatorApplyMethod;

    /**
     * An array used to return biome data
     */
    private final ArrayCache<int[]> dataArray = ArrayCache.makeIntArrayCache(256);

	public LocalMinecraftInterface(Map<String, SymbolicClass> symbolicClassMap, RecognisedVersion recognisedVersion) {
		this.recognisedVersion = recognisedVersion;
        this.utilClass = symbolicClassMap.get(SymbolicNames.CLASS_UTIL);
        this.biomeLayersClass = symbolicClassMap.get(SymbolicNames.CLASS_BIOME_LAYERS);
        this.layerOperatorClass = symbolicClassMap.get(SymbolicNames.CLASS_LAYER_OPERATOR);
        this.layerSamplerClass = symbolicClassMap.get(SymbolicNames.CLASS_LAYER_SAMPLER);
        this.layerFactoryClass = symbolicClassMap.get(SymbolicNames.CLASS_LAYER_FACTORY);
        this.layerSampleContextClass = symbolicClassMap.get(SymbolicNames.CLASS_LAYER_SAMPLE_CONTEXT);
        this.perlinNoiseSamplerClass = symbolicClassMap.get(SymbolicNames.CLASS_PERLIN_NOISE_SAMPLER);
	}

	@Override
	public synchronized MinecraftInterface.World createWorld(long seed, WorldType worldType, String generatorOptions)
			throws MinecraftInterfaceException {
		
		initializeIfNeeded();
		
	    try {
	    	Object builtLayerSampler = createBuiltLayerSampler(seed, worldType, generatorOptions, 4096);
            long seedForBiomeZoomer = makeSeedForBiomeZoomer(seed);
            return new World(builtLayerSampler, seedForBiomeZoomer);

        } catch(RuntimeException | IllegalAccessException | InvocationTargetException e) {
            throw new MinecraftInterfaceException("unable to create world", e);
        }
	}
	
	private synchronized void initializeIfNeeded() throws MinecraftInterfaceException {
	    if (!isInitialized) {
		    try {
		    	layerSamplerSampleMethod = getMethodHandle(layerSamplerClass, SymbolicNames.METHOD_LAYER_SAMPLER_SAMPLE);
		    	layerOperatorApplyMethod = getMethodHandle(layerOperatorClass, SymbolicNames.METHOD_LAYER_OPERATOR_APPLY);
		    	
		    	// hack to allow invokeExact
		    	layerSamplerSampleMethod = layerSamplerSampleMethod.asType(layerSamplerSampleMethod.type().changeParameterType(0, Object.class));
		    	layerOperatorApplyMethod = layerOperatorApplyMethod.asType(layerOperatorApplyMethod.type().changeParameterType(0, Object.class));
	        } catch(IllegalArgumentException | IllegalAccessException e) {
	            throw new MinecraftInterfaceException("unable to initialize the MinecraftInterface", e);
	        }
	
		    isInitialized = true;
	    }
	}

	private static long makeSeedForBiomeZoomer(long seed) throws MinecraftInterfaceException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
			buf.putLong(seed);
			byte[] bytes = digest.digest(buf.array());

			long result = 0;
			for (int i = 0; i < 8; i++) {
				result |= (bytes[i] & 0xffL) << (i*8L);
			}
			return result;
		} catch (NoSuchAlgorithmException e) {
			throw new MinecraftInterfaceException("unable to hash seed for biome zoomer", e);
		}
	}

	private Object createBuiltLayerSampler(long seed, WorldType worldType, String generatorOptions, int cacheSize)
            throws IllegalAccessException, InvocationTargetException {
		final boolean oldGen = false;
		
		Object layerFactory = biomeLayersClass.getMethod(SymbolicNames.METHOD_BIOME_LAYERS_BUILD).getRawMethod().invoke(
									  null, // static method, so this should be null
									  oldGen,
									  worldType.equals(WorldType.LARGE_BIOMES) ? 6 : 4, // biome size
									  4, // river size
									  (LongFunction<?>) (salt) -> createLayerSampleContext(cacheSize, seed, salt) // context creation from salt
							  );
		
		stopAllExecutorsIfNeeded();
		
		return layerFactoryClass.getMethod(SymbolicNames.METHOD_LAYER_FACTORY_MAKE).getRawMethod().invoke(layerFactory);
	}
	
	private Object createLayerSampleContext(int cacheSize, long seed, long salt) {
		Class<?> lscInterface = layerSampleContextClass.getClazz();
		
		try {
			return Proxy.newProxyInstance(lscInterface.getClassLoader(), new Class<?>[]{ lscInterface }, new InvocationHandler() {
				private final Object perlinNoiseSampler;
				private final long worldSeed;
				private long localSeed;
				
				{   // fake constructor
					this.worldSeed = addSalt(seed, salt);
					this.perlinNoiseSampler = perlinNoiseSamplerClass.getConstructor(SymbolicNames.CONSTRUCTOR_PERLIN_NOISE_SAMPLER).getRawConstructor().newInstance(new Random(seed));
				}
				
				@Override
			    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					Class<?>[] parameterTypes = method.getParameterTypes();
					
					if (areParameterTypesEqual(parameterTypes, long.class, long.class)) {
						long x = (long) args[0];
						long y = (long) args[1];
						
						long l = this.worldSeed;
						l = mixSeed(l, x);
						l = mixSeed(l, y);
						l = mixSeed(l, x);
						l = mixSeed(l, y);
						this.localSeed = l;
						return null;
						
					} else if (areParameterTypesEqual(parameterTypes, int.class)) {
						// the argument is the bound, have to do some casting trickery to make it work
					    return nextInt(((Integer) args[0]).longValue());
					    
					} else if (areParameterTypesEqual(parameterTypes, int.class, int.class)) {
					    return nextInt(2) == 0 ? args[0] : args[1];
						
					} else if (areParameterTypesEqual(parameterTypes, int.class, int.class, int.class, int.class)) {
						int i = nextInt(4);
						if (i == 0) {
							return args[0];
						} else if (i == 1) {
							return args[1];
						} else {
							return i == 2 ? args[2] : args[3];
						}
						
					} else if (args != null && layerOperatorClass.getClazz().isInstance(args[0])) {
						return createRawLayerSampler(cacheSize, args[0]);
						
					} else if (method.getReturnType().equals(perlinNoiseSamplerClass.getClazz())) {
						return perlinNoiseSampler;
						
					} else {
						return null;
						
					}
			    }
				
				private boolean areParameterTypesEqual(Class<?>[] methodParamTypes, Class<?>... checkedParamTypes) {
					if (checkedParamTypes.length != methodParamTypes.length) {
						return false;
					}
					
					for (int i = 0; i < methodParamTypes.length; i++) {
						if (!methodParamTypes[i].equals(checkedParamTypes[i])) {
							return false;
						}
					}
					
					return true;
				}
				
				private long addSalt(long seed, long salt) {
					long l = mixSeed(salt, salt);
					l = mixSeed(l, salt);
					l = mixSeed(l, salt);
					long m = mixSeed(seed, l);
					m = mixSeed(m, l);
					m = mixSeed(m, l);
					return m;
				}
				
				private int nextInt(long bound) {
				    int i = (int) Math.floorMod(this.localSeed >> 24, bound);
				    this.localSeed = mixSeed(this.localSeed, this.worldSeed);
				    return i;
				}
			});
		} catch (IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException("Unable to create LayerSampleContext", e);
		}
	}
	
	private Object createRawLayerSampler(int cacheSize, Object layerOperator) {
		Class<?> lsInterface = layerSamplerClass.getClazz();
		
		return Proxy.newProxyInstance(lsInterface.getClassLoader(), new Class<?>[]{ lsInterface }, new InvocationHandler() {
			private final int capacity;
			private final int mask;
			
			private final long[] keys;
			private final int[] values;
			
			{   // fake constructor
				this.capacity = smallestEncompassingPowerOfTwo(cacheSize);
				this.mask = this.capacity - 1;
				
				this.keys = new long[this.capacity];
				Arrays.fill(this.keys, Long.MIN_VALUE);
				this.values = new int[this.capacity];
			}
			
			@Override
	        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				int x = (int) args[0];
				int z = (int) args[1];
				
				long key = key(x, z);
				int idx = hash(key) & this.mask;
				
				// if the entry here has a key that matches ours, we have a cache hit
				if (this.keys[idx] == key) {
					return this.values[idx];
				}
				
				// cache miss: sample the operator and put the result into our cache entry
				int sampled = (int) layerOperatorApplyMethod.invokeExact(layerOperator, x, z);
				this.keys[idx] = key;
				this.values[idx] = sampled;
				
				return sampled;
	        }
			
			private int hash(long key) {
				long h = key * 0x9E3779B97F4A7C15L;
				h ^= h >>> 32;
				return (int) (h ^ (h >>> 16));
			}
			
			private long key(int x, int z) {
				return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
			}
			
			private int smallestEncompassingPowerOfTwo(int value) {
				int i = value - 1;
				i |= i >> 1;
				i |= i >> 2;
				i |= i >> 4;
				i |= i >> 8;
				i |= i >> 16;
				return i + 1;
			}
		});
	}
	
	private void stopAllExecutorsIfNeeded() throws IllegalArgumentException, IllegalAccessException {
		if (!areExecutorsStopped) {
			Class<?> clazz = utilClass.getClazz();
			for (Field field: clazz.getDeclaredFields()) {
				if ((field.getModifiers() & Modifier.STATIC) > 0 && field.getType().equals(ExecutorService.class)) {
					field.setAccessible(true);
					ExecutorService exec = (ExecutorService) field.get(null);
					exec.shutdownNow();
				}
			}
			areExecutorsStopped = true;
		}
	}

	private MethodHandle getMethodHandle(SymbolicClass symbolicClass, String method) throws IllegalAccessException {
	    Method rawMethod = symbolicClass.getMethod(method).getRawMethod();
	    return MethodHandles.lookup().unreflect(rawMethod);
	}
	
	private static long mixSeed(long seed, long salt) {
		seed *= seed * 6364136223846793005L + 1442695040888963407L;
		seed += salt;
		return seed;
	}
	
	@Override
	public RecognisedVersion getRecognisedVersion() {
		return recognisedVersion;
	}


	private class World implements MinecraftInterface.World {
		
	    private Object builtLayerSampler;

	    /**
	     * The seed used by the BiomeZoomer during interpolation.
	     * It is derived from the world seed.
	     */
		private long seedForBiomeZoomer;

	    private World(Object builtLayerSampler, long seedForBiomeZoomer) {
	    	this.builtLayerSampler = builtLayerSampler;
	    	this.seedForBiomeZoomer = seedForBiomeZoomer;
	    }

		@Override
		public<T> T getBiomeData(int x, int y, int width, int height,
				boolean useQuarterResolution, Function<int[], T> biomeDataMapper)
				throws MinecraftInterfaceException {

			int size = width * height;
		    return dataArray.withArrayFaillible(size, data -> {
			    try {
			    	if(size == 1) {
			    		data[0] = getBiomeIdAt(x, y, useQuarterResolution);
			    		return biomeDataMapper.apply(data);
			    	}

		    	    for (int i = 0; i < width; i++) {
		                for (int j = 0; j < height; j++) {
		                    int idx = i + j * width;
		                    data[idx] = getBiomeIdAt(x + i, y + j, useQuarterResolution);
		                }
		            }
			    } catch (Throwable e) {
			        throw new MinecraftInterfaceException("unable to get biome data", e);
			    }

			    return biomeDataMapper.apply(data);
		    });
		}

		private int getBiomeIdAt(int x, int z, boolean useQuarterResolution) throws Throwable {
		    if(useQuarterResolution) {
		        return (int) layerSamplerSampleMethod.invokeExact(builtLayerSampler, x, z);
		    } else {
		        return getFullResBiome(seedForBiomeZoomer, x, z);
		    }
		}
		
	    public int getFullResBiome(long seed, int x, int z) throws Throwable {
	        int x1 = x - 2;
	        int y1 = -2;
	        int z1 = z - 2;

	        int x2 = x1 >> 2;
	        int y2 = y1 >> 2;
	        int z2 = z1 >> 2;

	        double x3 = (double) (x1 & 3) / 4.0D;
	        double y3 = (double) (y1 & 3) / 4.0D;
	        double z3 = (double) (z1 & 3) / 4.0D;

	        int retX = Integer.MIN_VALUE;
	        int retZ = Integer.MIN_VALUE;

	        // This code would normally allocate an array to store each iteration's results, then scan back over it
	        // to determine the closest one. We can avoid the unnecessary step and simply keep track of the nearest one.
	        double minDist = Double.POSITIVE_INFINITY;

	        for (int i = 0; i < 8; i++) {
	            // Block sample positions
	            int bX;
	            int bY;
	            int bZ;

	            // Sample positions
	            double sX;
	            double sY;
	            double sZ;

	            if ((i & 0b100) == 0) {
	                bX = x2;
	                sX = x3;
	            } else {
	                bX = x2 + 1;
	                sX = x3 - 1.0D;
	            }

	            if ((i & 0b010) == 0) {
	                bY = y2;
	                sY = y3;
	            } else {
	                bY = y2 + 1;
	                sY = y3 - 1.0D;
	            }

	            if ((i & 0b001) == 0) {
	                bZ = z2;
	                sZ = z3;
	            } else {
	                bZ = z2 + 1;
	                sZ = z3 - 1.0D;
	            }

	            double dist = calcSquaredDistance(seed, bX, bY, bZ, sX, sY, sZ);

	            if (minDist > dist) {
	                minDist = dist;

	                retX = bX;
	                retZ = bZ;
	            }
	        }

			return (int) layerSamplerSampleMethod.invokeExact(builtLayerSampler, retX, retZ);
		}
		
		private double calcSquaredDistance(long seed, int x, int y, int z, double xFraction, double yFraction,
				double zFraction) {
			long l = mixSeed(seed, (long) x);
			l = mixSeed(l, (long) y);
			l = mixSeed(l, (long) z);
			l = mixSeed(l, (long) x);
			l = mixSeed(l, (long) y);
			l = mixSeed(l, (long) z);
			double d = distribute(l);
			l = mixSeed(l, seed);
			double e = distribute(l);
			l = mixSeed(l, seed);
			double f = distribute(l);
			return square(zFraction + f) + square(yFraction + e) + square(xFraction + d);
		}
		
	    private double distribute(long seed) {
	        return (((seed >> 24) & 1023L) - 512) * 0.00087890625; // * 0.9 / 1024.0d
	    }
		
		private double square(double d) {
			return d * d;
		}
	}
}
