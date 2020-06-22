package amidst.mojangapi.minecraftinterface.local;

import amidst.documentation.Immutable;

@Immutable
public enum SymbolicNames {
	;

    public static final String CLASS_REGISTRY = "Registry";
    public static final String FIELD_REGISTRY_META_REGISTRY = "metaRegistry";
    public static final String METHOD_REGISTRY_GET_BY_KEY = "getByKey";
    public static final String METHOD_REGISTRY_GET_ID = "getId";

    public static final String CLASS_REGISTRY_KEY = "RegistryKey";
    public static final String CONSTRUCTOR_REGISTRY_KEY = "<init>";
    
	public static final String CLASS_UTIL = "Util";
	public static final String FIELD_UTIL_SERVER_EXECUTOR = "SERVER_EXECUTOR";

	public static final String CLASS_NOISE_BIOME_PROVIDER = "NoiseBiomeProvider";
	public static final String METHOD_NOISE_BIOME_PROVIDER_GET_BIOME = "getBiome";
	
	public static final String CLASS_OVERWORLD_BIOME_PROVIDER = "OverworldBiomeProvider";
	public static final String CONSTRUCTOR_OVERWORLD_BIOME_PROVIDER = "<init>";
	
	public static final String CLASS_MULTI_NOISE_BIOME_PROVIDER = "MultiNoiseBiomeProvider";
	public static final String METHOD_MULTI_NOISE_BIOME_PROVIDER_PRESET_NETHER = "presetNether";

	public static final String CLASS_BIOME_ZOOMER = "BiomeZoomer";
	public static final String METHOD_BIOME_ZOOMER_GET_BIOME = "getBiome";
}
