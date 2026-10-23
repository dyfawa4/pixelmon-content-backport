package local.pixelmon.backport;
import java.util.UUID;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import net.minecraft.client.gui.AbstractGui;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class LightningOathClient {
 private static LightningOathNetwork.State state;
 private LightningOathClient(){}
 public static void accept(LightningOathNetwork.State value){var manager=ClientProxy.battleManager;if(manager!=null&&!manager.battleEnded&&manager.battleControllerIndex==value.battle)state=value;}
 public static boolean active(UUID id){var manager=ClientProxy.battleManager;return id!=null&&manager!=null&&!manager.battleEnded&&state!=null&&state.battle==manager.battleControllerIndex&&state.turns.getOrDefault(id,0)>0;}
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){state=null;}
 /** Round yellow status badge and white down arrow, independent of other badges. */
 public static void icon(MatrixStack matrix,float x,float y,float z,float size){matrix.func_227860_a_();try{matrix.func_227861_a_(x,y,z);matrix.func_227862_a_(size/24f,size/24f,1);for(int row=0;row<24;row++){int half=(int)Math.sqrt(144-Math.pow(row+.5-12,2));AbstractGui.func_238467_a_(matrix,12-half,row,12+half,row+1,0xffe0b525);}AbstractGui.func_238467_a_(matrix,10,5,14,14,0xffffffff);for(int row=0;row<6;row++)AbstractGui.func_238467_a_(matrix,6+row,13+row,18-row,14+row,0xffffffff);}finally{matrix.func_227865_b_();}}
}
