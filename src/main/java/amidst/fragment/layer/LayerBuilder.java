package amidst.fragment.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import amidst.AmidstSettings;
import amidst.documentation.Immutable;
import amidst.fragment.Fragment;
import amidst.fragment.colorprovider.*;
import amidst.fragment.constructor.BiomeDataConstructor;
import amidst.fragment.constructor.EndIslandsConstructor;
import amidst.fragment.constructor.FragmentConstructor;
import amidst.fragment.constructor.ImageConstructor;
import amidst.fragment.drawer.AlphaUpdater;
import amidst.fragment.drawer.FragmentDrawer;
import amidst.fragment.drawer.GridDrawer;
import amidst.fragment.drawer.ImageDrawer;
import amidst.fragment.drawer.WorldIconDrawer;
import amidst.fragment.loader.AlphaInitializer;
import amidst.fragment.loader.BiomeDataLoader;
import amidst.fragment.loader.EndIslandsLoader;
import amidst.fragment.loader.FragmentLoader;
import amidst.fragment.loader.ImageLoader;
import amidst.fragment.loader.WorldIconLoader;
import amidst.gui.main.viewer.BiomeSelection;
import amidst.gui.main.viewer.Graphics2DAccelerationCounter;
import amidst.gui.main.viewer.WorldIconSelection;
import amidst.gui.main.viewer.Zoom;
import amidst.mojangapi.world.Dimension;
import amidst.mojangapi.world.World;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.settings.Setting;

@Immutable
public class LayerBuilder {
	private final Iterable<FragmentConstructor> constructors;

	public LayerBuilder() {
		this.constructors = createConstructors();
	}

	/**
	 * This also defines the construction order.
	 */
	private Iterable<FragmentConstructor> createConstructors() {
		return Collections.unmodifiableList(
				Arrays.asList(
						new BiomeDataConstructor(Resolution.QUARTER),
						new EndIslandsConstructor(),
						new ImageConstructor(Resolution.QUARTER, LayerIds.BACKGROUND),
						new ImageConstructor(Resolution.CHUNK, LayerIds.SLIME)));
	}

	public Iterable<FragmentConstructor> getConstructors() {
		return constructors;
	}

	public int getNumberOfLayers() {
		return LayerIds.NUMBER_OF_LAYERS;
	}

	public LayerManager create(
			AmidstSettings settings,
			World world,
			BiomeSelection biomeSelection,
			WorldIconSelection worldIconSelection,
			Zoom zoom,
			Graphics2DAccelerationCounter accelerationCounter) {
		List<LayerDeclaration> declarations = createDeclarations(settings, world.getEnabledLayers());
		return new LayerManager(
				declarations,
				new LayerLoader(
						createLoaders(declarations, world, biomeSelection, settings),
						LayerIds.NUMBER_OF_LAYERS),
				createDrawers(declarations, zoom, worldIconSelection, accelerationCounter));
	}

	private List<LayerDeclaration> createDeclarations(AmidstSettings settings, List<Integer> enabledLayers) {

		ArrayList<LayerDeclaration> declarations = new ArrayList<>(LayerIds.NUMBER_OF_LAYERS);
		Dimension[] ALL = LayerDeclaration.DIMENSION_ALL;
		Dimension[] OW_HELL = new Dimension[]{Dimension.OVERWORLD, Dimension.NETHER};

		List<LayerDeclarationParam> declareList = Arrays.asList(
				new LayerDeclarationParam(LayerIds.ALPHA, ALL, false, Setting.createImmutable(true)),
				new LayerDeclarationParam(LayerIds.BIOME_DATA, ALL, false, Setting.createImmutable(true)),
				new LayerDeclarationParam(LayerIds.END_ISLANDS, Dimension.END, false, Setting.createImmutable(true)),
				new LayerDeclarationParam(LayerIds.BACKGROUND, ALL, false, Setting.createImmutable(true)),
				new LayerDeclarationParam(LayerIds.SLIME, Dimension.OVERWORLD, false, settings.showSlimeChunks),
				new LayerDeclarationParam(LayerIds.GRID, ALL, true, settings.showGrid),
				new LayerDeclarationParam(LayerIds.SPAWN, Dimension.OVERWORLD, false, settings.showSpawn),
				new LayerDeclarationParam(LayerIds.STRONGHOLD, Dimension.OVERWORLD, false, settings.showStrongholds),
				new LayerDeclarationParam(LayerIds.PLAYER, ALL, false, settings.showPlayers),
				new LayerDeclarationParam(LayerIds.VILLAGE, Dimension.OVERWORLD, false, settings.showVillages),
				new LayerDeclarationParam(LayerIds.TEMPLE, Dimension.OVERWORLD, false, settings.showTemples),
				new LayerDeclarationParam(LayerIds.MINESHAFT, Dimension.OVERWORLD, false, settings.showMineshafts),
				new LayerDeclarationParam(LayerIds.OCEAN_MONUMENT, Dimension.OVERWORLD, false, settings.showOceanMonuments),
				new LayerDeclarationParam(LayerIds.WOODLAND_MANSION, Dimension.OVERWORLD, false, settings.showWoodlandMansions),
				new LayerDeclarationParam(LayerIds.OCEAN_FEATURES, Dimension.OVERWORLD, false, settings.showOceanFeatures),
				new LayerDeclarationParam(LayerIds.NETHER_FORTRESS, OW_HELL, false, new Setting[]{settings.showNetherFortresses_OW, settings.showNetherFortresses_Nether}),
				new LayerDeclarationParam(LayerIds.BASTION_REMNANT, OW_HELL, false, new Setting[]{settings.showBastionRemnant_OW, settings.showBastionRemnant_Nether}),
				new LayerDeclarationParam(LayerIds.RUINED_PORTALS, OW_HELL, false, new Setting[]{settings.showRuinedPortals_OW, settings.showRuinedPortals_Nether}),
				new LayerDeclarationParam(LayerIds.END_CITY, Dimension.END, false, settings.showEndCities),
				new LayerDeclarationParam(LayerIds.END_GATEWAY, Dimension.END, false, settings.showEndGateways)
		);

		for (LayerDeclarationParam param : declareList) {
			declarations.add(param.layerId, new LayerDeclaration(
					param.layerId,
					param.dimensions,
					param.drawUnloaded,
					enabledLayers.contains(param.layerId),
					param.visibleMap
			));
		}

		// @formatter:on
		return Collections.unmodifiableList(declarations);
	}

	/**
	 * This also defines the loading and reloading order.
	 */
	private Iterable<FragmentLoader> createLoaders(
			List<LayerDeclaration> declarations,
			World world,
			BiomeSelection biomeSelection,
			AmidstSettings settings) {
		// @formatter:off
		return Collections.unmodifiableList(Arrays.asList(
				new AlphaInitializer( declarations.get(LayerIds.ALPHA),           settings.fragmentFading),
				new BiomeDataLoader(  declarations.get(LayerIds.BIOME_DATA),      world.getOverworldBiomeDataOracle(), world.getNetherBiomeDataOracle()),
				new EndIslandsLoader( declarations.get(LayerIds.END_ISLANDS),     world.getEndIslandOracle()),
				new ImageLoader(	  declarations.get(LayerIds.BACKGROUND),      Resolution.QUARTER, new BackgroundColorProvider(new BiomeColorProvider(biomeSelection, settings.biomeProfileSelection), new NetherColorProvider(biomeSelection, settings.biomeProfileSelection), new TheEndColorProvider())),
				new ImageLoader(      declarations.get(LayerIds.SLIME),           Resolution.CHUNK,   new SlimeColorProvider(world.getSlimeChunkOracle())),
				new WorldIconLoader<>(declarations.get(LayerIds.SPAWN),           world.getSpawnProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.STRONGHOLD),      world.getStrongholdProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.PLAYER),          world.getPlayerProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.VILLAGE),         world.getVillageProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.TEMPLE),          world.getTempleProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.MINESHAFT),       world.getMineshaftProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.OCEAN_MONUMENT),  world.getOceanMonumentProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.WOODLAND_MANSION),world.getWoodlandMansionProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.OCEAN_FEATURES),  world.getOceanFeaturesProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.NETHER_FORTRESS), world.getNetherFortressProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.BASTION_REMNANT), world.getBastionRemnantProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.RUINED_PORTALS),  world.getRuinedPortalProducer()),
				new WorldIconLoader<>(declarations.get(LayerIds.END_CITY),        world.getEndCityProducer(), Fragment::getLargeEndIslands),
				new WorldIconLoader<>(declarations.get(LayerIds.END_GATEWAY),     world.getEndGatewayProducer(), Fragment::getEndIslands)
		));
		// @formatter:on
	}

	/**
	 * This also defines the rendering order.
	 */
	private Iterable<FragmentDrawer> createDrawers(
			List<LayerDeclaration> declarations,
			Zoom zoom,
			WorldIconSelection worldIconSelection,
			Graphics2DAccelerationCounter accelerationCounter) {
		// @formatter:off
		return Collections.unmodifiableList(Arrays.asList(
				new AlphaUpdater(   declarations.get(LayerIds.ALPHA)),
				new ImageDrawer(    declarations.get(LayerIds.BACKGROUND),      Resolution.QUARTER, accelerationCounter),
				new ImageDrawer(    declarations.get(LayerIds.SLIME),           Resolution.CHUNK,   accelerationCounter),
				new GridDrawer(     declarations.get(LayerIds.GRID),            zoom),
				new WorldIconDrawer(declarations.get(LayerIds.SPAWN),           zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.STRONGHOLD),      zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.PLAYER),          zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.VILLAGE),         zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.TEMPLE),          zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.MINESHAFT),       zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.OCEAN_MONUMENT),  zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.WOODLAND_MANSION),zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.OCEAN_FEATURES),  zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.NETHER_FORTRESS), zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.BASTION_REMNANT), zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.RUINED_PORTALS),  zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.END_CITY),        zoom, worldIconSelection),
				new WorldIconDrawer(declarations.get(LayerIds.END_GATEWAY),     zoom, worldIconSelection)
		));
		// @formatter:on
	}

	/**
	 * Simple class to store layout element info
	 */
	private static class LayerDeclarationParam {
		public int layerId;
		public Dimension[] dimensions;
		public boolean drawUnloaded;
		public Setting<Boolean>[] visibleMap;
		public LayerDeclarationParam(
			int layerId,
			Dimension[] dimensions,
			boolean drawUnloaded,
			Setting<Boolean>[] visibleMap
		) {
			this.layerId = layerId;
			this.dimensions = dimensions;
			this.drawUnloaded = drawUnloaded;
			this.visibleMap = visibleMap;
		}
		public LayerDeclarationParam(
			int layerId,
			Dimension[] dimensions,
			boolean drawUnloaded,
			Setting<Boolean> isVisibleSetting
		) {
			this(layerId, dimensions, drawUnloaded, new Setting[]{isVisibleSetting});
		}
		public LayerDeclarationParam(
				int layerId,
				Dimension dimension,
				boolean drawUnloaded,
				Setting<Boolean> isVisibleSetting
		) {
			this(layerId, new Dimension[]{dimension}, drawUnloaded, isVisibleSetting);
		}
	}
}
