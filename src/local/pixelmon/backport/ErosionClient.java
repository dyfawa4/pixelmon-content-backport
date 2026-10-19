package local.pixelmon.backport;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.TranslationTextComponent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class ErosionClient {
    private static ErosionNetwork.State state;
    private ErosionClient(){}
    public static boolean bonus(java.util.UUID id){var b=ClientProxy.battleManager;return b!=null&&!b.battleEnded&&state!=null&&state.battle==b.battleControllerIndex&&id!=null&&id.equals(state.bonus);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent e){state=null;}
    public static void accept(ErosionNetwork.State s){
        ClientBattleManager b=ClientProxy.battleManager;if(b==null||b.battleEnded||s.battle!=b.battleControllerIndex)return;
        state=s;if(s.resized!=null){PixelmonClientData p=b.getPokemon(s.resized);if(p!=null){p.maxHealth=s.max;p.health.set(s.hp);}}
    }
    public static void badge(MatrixStack m,PixelmonClientData p,float x,float y,float z,float size){
        ClientBattleManager b=ClientProxy.battleManager;if(p==null||state==null||b==null||b.battleEnded||state.battle!=b.battleControllerIndex||!state.turns.containsKey(p.pokemonUUID))return;
        // Match the native round, flat-color status icons; never consume the main status slot.
        var font=Minecraft.func_71410_x().field_71466_p;String label=new TranslationTextComponent("pixelmon_backport.erosion.badge").getString();
        m.func_227860_a_();
        try{
            m.func_227861_a_(x,y,z);m.func_227862_a_(size/24f,size/24f,1);
            for(int row=0;row<24;row++){int half=(int)Math.sqrt(144-Math.pow(row+0.5-12,2));AbstractGui.func_238467_a_(m,12-half,row,12+half,row+1,0xff3989df);}
            float scale=Math.min(16f/Math.max(1,font.func_78256_a(label)),16f/9f);m.func_227861_a_((24-font.func_78256_a(label)*scale)/2,(24-9*scale)/2,1);m.func_227862_a_(scale,scale,1);font.func_238421_b_(m,label,0,0,0xffffff);
        }finally{m.func_227865_b_();}
    }
    public static void opponent(MatrixStack m,PixelmonClientData p,int x,int y,float z,boolean raid){
        if(p==null)return;
        if(raid){badge(m,p,x+198,y+12,z,20);return;}
        float left=x+54+com.pixelmonmod.pixelmon.client.gui.ScreenHelper.getStringWidth(p.getDisplayName())+(p.getGender()!=com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.NONE?7:0)+(com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.pokedex!=null&&com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.pokedex.hasCaught(p.species)?9:0)+(p.status!=-1?12:0);
        badge(m,p,left<=x+117?left:x+142,left<=x+117?y+4:y+32,z,10.5f);
    }
    public static void ally(MatrixStack m,PixelmonClientData p,int x,int y,float z){
        if(p==null)return;
        float left=x+14+com.pixelmonmod.pixelmon.client.gui.ScreenHelper.getStringWidth(p.getDisplayName())+(p.getGender()!=com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.NONE?7:0)+(p.status!=-1?12:0);
        badge(m,p,left<=x+95?left:x+13,left<=x+95?y+12.5f:y+40,z,10.5f);
    }
}
