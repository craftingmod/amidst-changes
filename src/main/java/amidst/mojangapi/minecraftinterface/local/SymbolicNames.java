package amidst.mojangapi.minecraftinterface.local;

import amidst.documentation.Immutable;

@Immutable
public enum SymbolicNames {
	;

    public static final String CLASS_REGISTRY = "Registry";

	public static final String CLASS_UTIL = "Util";

	public static final String CLASS_BIOME_LAYERS = "BiomeLayers";
	public static final String METHOD_BIOME_LAYERS_BUILD = "build";
	
	public static final String CLASS_BIOME_LAYER_SAMPLER = "BiomeLayerSampler";
	public static final String FIELD_BIOME_LAYER_SAMPLER_SAMPLER = "sampler";
	
	public static final String CLASS_CACHING_LAYER_SAMPLER = "CachingLayerSampler";
	public static final String FIELD_CACHING_LAYER_SAMPLER_LAYER_OPERATOR = "operator";
	
	public static final String CLASS_LAYER_OPERATOR = "LayerOperator";
	public static final String METHOD_LAYER_OPERATOR_APPLY = "apply";
	
	public static final String CLASS_LAYER_SAMPLER = "LayerSampler";
	public static final String METHOD_LAYER_SAMPLER_SAMPLE = "sample";
	
	public static final String CLASS_LAYER_FACTORY = "LayerFactory";
	public static final String METHOD_LAYER_FACTORY_MAKE = "make";
	
	public static final String CLASS_LAYER_SAMPLE_CONTEXT = "LayerSampleContext";
	
	public static final String CLASS_PERLIN_NOISE_SAMPLER = "PerlinNoiseSampler";
	public static final String CONSTRUCTOR_PERLIN_NOISE_SAMPLER = "<init>";
}
