package amidst.mojangapi.mocking;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import amidst.documentation.ThreadSafe;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.Dimension;
import amidst.mojangapi.world.WorldType;
import amidst.mojangapi.world.testworld.storage.json.BiomeDataJson;
import amidst.mojangapi.world.testworld.storage.json.WorldMetadataJson;

@ThreadSafe
public class FakeMinecraftInterface implements MinecraftInterface {
	private final WorldMetadataJson worldMetadataJson;
	private final BiomeDataJson quarterBiomeData;
	private final BiomeDataJson fullBiomeData;

	public FakeMinecraftInterface(
			WorldMetadataJson worldMetadataJson,
			BiomeDataJson quarterBiomeData,
			BiomeDataJson fullBiomeData) {
		this.worldMetadataJson = worldMetadataJson;
		this.quarterBiomeData = quarterBiomeData;
		this.fullBiomeData = fullBiomeData;
	}
	
	@Override
	public MinecraftInterface.WorldConfig createWorldConfig() throws MinecraftInterfaceException {
		return new WorldConfig();
	}

	@Override
	public RecognisedVersion getRecognisedVersion() {
		return worldMetadataJson.getRecognisedVersion();
	}
	
	private class WorldConfig implements MinecraftInterface.WorldConfig {

		@Override
		public Set<Dimension> supportedDimensions() {
			return EnumSet.allOf(Dimension.class);
		}

		@Override
		public MinecraftInterface.WorldAccessor createWorldAccessor(long seed, WorldType worldType, String generatorOptions)
				throws MinecraftInterfaceException {
			if (worldMetadataJson.getSeed() == seed && worldMetadataJson.getWorldType().equals(worldType)
					&& generatorOptions.isEmpty()) {
				return new WorldAccessor();
			} else {
				throw new MinecraftInterfaceException("the world has to match");
			}
		}
		
	}

	private class WorldAccessor implements MinecraftInterface.WorldAccessor {
		
		@Override
		public<T> T getBiomeData(Dimension dimension, int x, int y, int width, int height,
				boolean useQuarterResolution, Function<int[], T> biomeDataMapper)
				throws MinecraftInterfaceException {
			BiomeDataJson biomes = useQuarterResolution ? quarterBiomeData : fullBiomeData;
			return biomeDataMapper.apply(biomes.get(dimension, x, y, width, height));
		}
	}
}
