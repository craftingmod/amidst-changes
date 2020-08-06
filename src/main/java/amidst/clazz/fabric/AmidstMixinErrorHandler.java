package amidst.clazz.fabric;

import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import amidst.gui.crash.CrashWindow;
import amidst.logging.AmidstLogger;

public class AmidstMixinErrorHandler implements IMixinErrorHandler {

	@Override
	public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
		AmidstLogger.crash(th, "Error initializing mixin " + mixin.getClassName() + ": " + th.getMessage());
		CrashWindow.showAfterCrash();
		return action;
	}

	@Override
	public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
		AmidstLogger.crash(th, "Error transforming class " + targetClassName + " in mixin " + mixin.getClassName() + ": " + th.getMessage());
		CrashWindow.showAfterCrash();
		return action;
	}
	
}
