package local.pixelmon.backport.client;

import local.pixelmon.backport.JavaRuntimePolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.*;

/** Client only: replace world-entry menus, with lower-level load/connect guards. */
public final class JavaRuntimeGate {
    private static boolean locked;
    private JavaRuntimeGate(){}
    public static Screen filter(Screen requested){
        if(JavaRuntimePolicy.allowed() || requested instanceof JavaRuntimeScreen)return requested;
        if(locked || requested==null || requested instanceof MainMenuScreen || requested instanceof WorldSelectionScreen
            || requested instanceof CreateWorldScreen || requested instanceof MultiplayerScreen || requested instanceof ConnectingScreen){
            locked=true;return new JavaRuntimeScreen();
        }
        return requested;
    }
    public static boolean blockEntry(){
        if(JavaRuntimePolicy.allowed())return false;
        locked=true;Minecraft.func_71410_x().func_147108_a(new JavaRuntimeScreen());return true;
    }
}
