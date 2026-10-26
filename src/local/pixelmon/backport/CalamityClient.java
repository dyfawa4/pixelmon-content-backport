package local.pixelmon.backport;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.TranslationTextComponent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class CalamityClient {
    private static CalamityNetwork.State state;
    public static boolean has(java.util.UUID id){var b=ClientProxy.battleManager;return id!=null&&b!=null&&!b.battleEnded&&state!=null&&state.battle==b.battleControllerIndex&&state.active.contains(id);}
    public static boolean has(com.pixelmonmod.pixelmon.api.pokemon.Pokemon p){return p!=null&&(Calamity.active(p)||has(p.getUUID()));}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent e){state=null;Calamity.clearClient();}
    public static void accept(CalamityNetwork.State s){ClientBattleManager b=ClientProxy.battleManager;if(b==null||b.battleEnded||s.battle!=b.battleControllerIndex)return;state=s;if(s.resized!=null)b.addBattleMessage(new HealthSnapshot(s));}
    public static void party(CalamityPartySync.State s){for(var row:s.rows.entrySet()){var id=row.getKey();Calamity.client(id,s.forms.get(id),s.levels.getOrDefault(id,0),s.stats.get(id));}var party=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party;if(party==null)return;for(var p:party.getAll())if(p!=null&&s.rows.containsKey(p.getUUID())){int reduction=s.rows.get(p.getUUID());if(reduction>0)p.getPersistentData().func_74768_a(Calamity.TAG,reduction);else p.getPersistentData().func_82580_o(Calamity.TAG);}}
    public static void icon(MatrixStack m,float x,float y,float z,float size){
        var font=Minecraft.func_71410_x().field_71466_p;String label=new TranslationTextComponent("pixelmon_backport.calamity.badge").getString();
        m.func_227860_a_();try{m.func_227861_a_(x,y,z);m.func_227862_a_(size/24f,size/24f,1);
        for(int row=0;row<24;row++){int half=(int)Math.sqrt(144-Math.pow(row+.5-12,2));AbstractGui.func_238467_a_(m,12-half,row,12+half,row+1,0xffa348d0);}
        float scale=Math.min(16f/Math.max(1,font.func_78256_a(label)),16f/9f);m.func_227861_a_((24-font.func_78256_a(label)*scale)/2,(24-9*scale)/2,1);m.func_227862_a_(scale,scale,1);font.func_238421_b_(m,label,0,0,0xffffff);
        }finally{m.func_227865_b_();}
    }
    /** Stay in the native battle task queue: outstanding damage animations must finish first. */
    private static final class HealthSnapshot extends com.pixelmonmod.pixelmon.battles.tasks.BattleTaskPacket {
        final CalamityNetwork.State snapshot;
        HealthSnapshot(CalamityNetwork.State s){snapshot=s;}
        @Override public java.util.UUID getPokemonID(){return snapshot.resized;}
        @Override public void encode(net.minecraft.network.PacketBuffer ignored){throw new UnsupportedOperationException("Client-only task");}
        @Override public void decode(net.minecraft.network.PacketBuffer ignored){throw new UnsupportedOperationException("Client-only task");}
        @Override public boolean process(ClientBattleManager b){if(!b.battleEnded&&b.battleControllerIndex==snapshot.battle){PixelmonClientData p=b.getPokemon(snapshot.resized);if(p!=null){p.maxHealth=snapshot.max;p.health.set(snapshot.hp);}}return false;}
    }
}
