package amidst.mojangapi.world.filter;

import java.util.Set;

import amidst.documentation.NotThreadSafe;
import amidst.mojangapi.world.Dimension;
import amidst.mojangapi.world.World;

@NotThreadSafe
public class WorldFilter_Biome extends WorldFilter {
	private final Set<Short> validBiomeIndexes;

	public WorldFilter_Biome(long worldFilterSize, Set<Short> validBiomeIndexes) {
		super(worldFilterSize);
		this.validBiomeIndexes = validBiomeIndexes;
	}

	@Override
	public boolean isValid(World world, Dimension dimen) {
		int size = (int) (this.quarterFilterSize * 2);
		return world.getOverworldBiomeDataOracle().getBiomeData(corner, dimen, size, size, true, data -> {
			for (int biome: data) {
				if (validBiomeIndexes.contains((short) biome)) {
					return true;
				}
			}
			return false;
		}, () -> false);
	}
}
