package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.config.PixelmonServerConfig;

/** Refresh the native render cache without copying local gameplay settings over server values. */
public final class ConfigLiveUpdate {
    private ConfigLiveUpdate(){}
    public static void graphics(){PixelmonServerConfig.renderWildLevels=PixelmonConfigProxy.getGraphics().isRenderWildLevels();}
}
