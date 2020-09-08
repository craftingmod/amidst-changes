package amidst.fragment.layer;

import amidst.documentation.*;
import amidst.mojangapi.world.Dimension;
import amidst.settings.Setting;

@NotThreadSafe
public class LayerDeclaration {
	public static final Dimension[] DIMENSION_ALL = {Dimension.OVERWORLD, Dimension.NETHER, Dimension.END};
	private final int layerId;
	private final Dimension[] dimensions;
	private final boolean isDrawUnloaded;
	private final boolean isSupportedInCurrentVersion;
	private final Setting<Boolean>[] isVisibleSettings;

	private volatile boolean isVisible;

	/**
	 * @param dimensions Cannot be null for all dimension.
	 * Put all dimensions to array.
	 *
	 * isVisibleSettings' index is same as dimensions array.
	 */
	public LayerDeclaration(
			int layerId,
			@NotNull Dimension[] dimensions,
			boolean drawUnloaded,
			boolean isSupportedInCurrentVersion,
			Setting<Boolean>[] isVisibleSettings) {
		this.layerId = layerId;
		this.dimensions = dimensions;
		this.isDrawUnloaded = drawUnloaded;
		this.isSupportedInCurrentVersion = isSupportedInCurrentVersion;
		this.isVisibleSettings = isVisibleSettings;
	}

	public int getLayerId() {
		return layerId;
	}

	public boolean isDrawUnloaded() {
		return isDrawUnloaded;
	}

	public boolean isVisible() {
		return isVisible;
	}

	/**
	 * Updates the isVisible and isEnabled fields to the current setting values.
	 * Returns whether the layer becomes visible.
	 */
	@CalledOnlyBy(AmidstThread.FRAGMENT_LOADER)
	public boolean update(Dimension dimension) {
		boolean isVisible = checkIsEnabled(dimension, true);
		boolean reload = isVisible != this.isVisible;
		this.isVisible = isVisible;
		return reload;
	}

	@CalledByAny
	public boolean calculateIsEnabled(Dimension dimen) {
		return checkIsEnabled(dimen, false);
	}

	private boolean checkIsEnabled(Dimension dimension, boolean checkConfig) {
		if (isMatchingVersion()) {
			for (int i = 0; i < dimensions.length; i += 1) {
				Dimension dimen = dimensions[i];
				if (dimen.equals(dimension)) {
					if (checkConfig) {
						if (i < isVisibleSettings.length) {
							return isVisibleSettings[i].get();
						} else {
							return isVisibleSettings[0].get();
						}
					} else {
						return true;
					}
				}
			}
		}
		return false;
	}

	@CalledByAny
	private boolean isMatchingVersion() {
		return isSupportedInCurrentVersion;
	}
}
