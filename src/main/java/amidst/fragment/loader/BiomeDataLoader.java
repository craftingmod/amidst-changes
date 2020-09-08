package amidst.fragment.loader;

import amidst.documentation.AmidstThread;
import amidst.documentation.CalledOnlyBy;
import amidst.documentation.NotThreadSafe;
import amidst.fragment.Fragment;
import amidst.fragment.layer.LayerDeclaration;
import amidst.logging.AmidstLogger;
import amidst.mojangapi.world.Dimension;
import amidst.mojangapi.world.oracle.BiomeDataOracle;

import java.util.Optional;

@NotThreadSafe
public class BiomeDataLoader extends FragmentLoader {
	private final BiomeDataOracle owBiomeDataOracle;
	private final boolean hasNether;
	private BiomeDataOracle netherBiomeDataOracle;

	public BiomeDataLoader(LayerDeclaration declaration, BiomeDataOracle OWBiomeDataOracle, Optional<BiomeDataOracle> NetherBiomeDataOracle) {
		super(declaration);
		this.owBiomeDataOracle = OWBiomeDataOracle;
		this.hasNether = NetherBiomeDataOracle.isPresent();
		if (hasNether) {
			this.netherBiomeDataOracle = NetherBiomeDataOracle.get();
		}
	}

	@CalledOnlyBy(AmidstThread.FRAGMENT_LOADER)
	@Override
	public void load(Dimension dimension, Fragment fragment) {
		doLoad(fragment, dimension);
	}

	@CalledOnlyBy(AmidstThread.FRAGMENT_LOADER)
	@Override
	public void reload(Dimension dimension, Fragment fragment) {
		doLoad(fragment, dimension);
	}

	@CalledOnlyBy(AmidstThread.FRAGMENT_LOADER)
	private void doLoad(Fragment fragment, Dimension dimension) {
		if (dimension == Dimension.OVERWORLD) {
			fragment.populateBiomeData(owBiomeDataOracle, Dimension.OVERWORLD);
		} else if (dimension == Dimension.NETHER) {
			if (hasNether) {
				fragment.populateBiomeData(netherBiomeDataOracle, dimension);
			}
			// else: do nothing
		} else {
			// just do nothing
			// fragment.populateBiomeData(owBiomeDataOracle, Dimension.OVERWORLD);
		}
	}
}
