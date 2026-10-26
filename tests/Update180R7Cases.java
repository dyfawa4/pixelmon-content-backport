package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.items.PokeBallItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TranslationTextComponent;
public final class Update180R7Cases {
    static volatile MovingAgreement.State preview;static UUID pokemonId;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static PokeBall ball(String id){return PokeBallRegistry.getPokeBall(id).getValueUnsafe();}
    static void prepare(ServerPlayerEntity p,String ball,int agreements,int count){p.field_71071_by.func_174888_l();p.field_71071_by.func_70299_a(0,new ItemStack(Backport.item("moving_agreement"),agreements));p.field_71071_by.func_70299_a(1,PokeBallItem.of(ball(ball),count));}
    public static void run(ServerPlayerEntity p)throws Exception {
        var party=StorageProxy.getParty(p);var original=party.getAll().clone();var inventory=new CompoundNBT();inventory.func_218657_a("Inventory",p.field_71071_by.func_70442_a(new net.minecraft.nbt.ListNBT()));
        try{
            p.field_71071_by.func_174888_l();var mon=Update180Cases.mon(25,"Thunderbolt","Quick Attack");mon.setBall(ball("poke_ball"));party.set(0,mon);pokemonId=mon.getUUID();var ability=mon.getAbility().getName();var iv=mon.getIVs().getArray().clone();var moves=mon.getMoveset().toString();
            prepare(p,"master_ball",3,2);var state=MovingAgreement.begin(p);check(state!=null&&state.open&&state.ids.contains(mon.getUUID()),"Open owned party");
            for(String blocked:List.of("park_ball","strange_ball","origin_ball")){check(!state.balls.contains(blocked),"Hidden blacklist "+blocked);check(!MovingAgreement.change(p,mon.getUUID(),"poke_ball",blocked),"Server rejects forged blacklist "+blocked);}
            var request=new MovingAgreement.Request(state.token,state.revision,mon.getUUID(),"poke_ball","master_ball");var result=MovingAgreement.submit(p,request);check(result!=null&&result.feedback==1&&mon.getBall().getName().equals("master_ball"),"Master ball NBT changes native field");
            check(p.field_71071_by.func_70301_a(0).func_190916_E()==2&&p.field_71071_by.func_70301_a(1).func_190916_E()==1,"Exactly one agreement and one ball consumed");check(MovingAgreement.submit(p,request)==null,"Repeated packet rejected");check(p.field_71071_by.func_70301_a(0).func_190916_E()==2,"Repeat no extra cost");
            for(var s:p.field_71071_by.field_70462_a)check(!MovingAgreement.isBall(s,ball("poke_ball")),"Old ball is not returned");
            check(!MovingAgreement.change(p,mon.getUUID(),"master_ball","master_ball"),"Same type no consumption");check(!MovingAgreement.change(p,mon.getUUID(),"poke_ball","ultra_ball"),"Stale old ball rejected");
            var restored=PokemonFactory.create(mon.writeToNBT(new CompoundNBT()));check(restored.getBall().getName().equals("master_ball"),"Native Pokemon NBT persists changed ball");check(mon.getAbility().getName().equals(ability)&&Arrays.equals(iv,mon.getIVs().getArray())&&moves.equals(mon.getMoveset().toString()),"Moves IVs ability preserved");
            prepare(p,"ultra_ball",1,0);check(!MovingAgreement.change(p,mon.getUUID(),"master_ball","ultra_ball")&&p.field_71071_by.func_70301_a(0).func_190916_E()==1,"No ball rejects without consuming agreement");prepare(p,"ultra_ball",0,1);check(!MovingAgreement.change(p,mon.getUUID(),"master_ball","ultra_ball")&&p.field_71071_by.func_70301_a(1).func_190916_E()==1,"No agreement rejects without consuming ball");
            prepare(p,"ultra_ball",2,2);var egg=Update180Cases.mon(133,"Tackle").makeEgg();party.set(1,egg);check(!MovingAgreement.change(p,egg.getUUID(),MovingAgreement.ball(egg),"ultra_ball"),"Egg rejected");
            var stranger=FakePlayerFactory.get(p.func_71121_q(),new GameProfile(UUID.fromString("013e6334-6666-4666-8666-000000001887"),"MovingStranger"));prepare(stranger,"ultra_ball",2,2);check(!MovingAgreement.change(stranger,mon.getUUID(),"master_ball","ultra_ball"),"Other owner rejected");
            var bad=new MovingAgreement.Request(UUID.randomUUID(),result.revision,mon.getUUID(),"master_ball","ultra_ball");check(MovingAgreement.submit(p,bad)==null,"Forged session rejected");var fresh=MovingAgreement.begin(p);check(MovingAgreement.submit(p,new MovingAgreement.Request(state.token,result.revision,mon.getUUID(),"master_ball","ultra_ball"))==null,"Old screen session invalidated");
            int variants=0;for(PokeBall b:MovingAgreement.choices()){
                mon.setBall(ball(b.getName().equals("poke_ball")?"ultra_ball":"poke_ball"));prepare(p,b.getName(),1,1);check(MovingAgreement.isBall(p.field_71071_by.func_70301_a(1),b),"Real ball item variant "+b.getName());
                check(MovingAgreement.change(p,mon.getUUID(),MovingAgreement.ball(mon),b.getName()),"Switch variant "+b.getName());check(p.field_71071_by.func_70301_a(0).func_190926_b()&&p.field_71071_by.func_70301_a(1).func_190926_b(),"Last two items consumed "+b.getName());variants++;
            }
            // Leave one actual server snapshot for the native client screen/render test.
            mon.setBall(ball("poke_ball"));prepare(p,"ultra_ball",4,3);p.field_71071_by.func_70299_a(2,PokeBallItem.of(ball("master_ball"),1));preview=MovingAgreement.begin(p);
            var encoded=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());var encode=MovingAgreement.State.class.getDeclaredMethod("encode",MovingAgreement.State.class,net.minecraft.network.PacketBuffer.class);var decode=MovingAgreement.State.class.getDeclaredMethod("decode",net.minecraft.network.PacketBuffer.class);encode.setAccessible(true);decode.setAccessible(true);encode.invoke(null,preview,encoded);var wire=(MovingAgreement.State)decode.invoke(null,encoded);encoded.release();check(wire.balls.equals(preview.balls)&&wire.ids.equals(preview.ids)&&wire.current.equals(preview.current)&&wire.counts.equals(preview.counts),"Native packet buffer roundtrip");
            System.out.println("R7_MOVING_PASS "+variants+" native ball variants, exact two costs, no return, blacklist, ownership, eggs, missing inventory, stale/repeated/forged sessions, NBT and packet roundtrip");
        }finally{if(preview==null){for(int i=0;i<6;i++)party.set(i,original[i]);p.field_71071_by.func_70443_b(inventory.func_150295_c("Inventory",10));}}
    }
    public static void client(int age)throws Exception {
        var mc=Minecraft.func_71410_x();if(age==35){var screen=new MovingAgreementScreen(preview);mc.func_147108_a(screen);int absent=preview.counts.indexOf(0),present=preview.balls.indexOf("ultra_ball");check(absent>=0&&!screen.canSelect(absent),"Missing ball disabled");check(screen.canSelect(present),"Owned ball selectable");screen.candidate=present;check(screen.canChange(),"Valid change enabled");screen.data.agreements=0;check(!screen.canChange(),"No agreement disables confirm");screen.data.agreements=4;screen.func_231160_c_();String description=new TranslationTextComponent("pixelmon.backport.moving.description").getString();check(description.equals("可以切换已有宝可梦居住的精灵球，使用后该道具和精灵球会消耗"),"Exact final Chinese description");
            for(String item:List.of("moving_agreement","shrinking_lamp","enlarging_lamp")){var model=mc.func_175599_af().func_184393_a(new ItemStack(Backport.item(item)),mc.field_71441_e,mc.field_71439_g);check(!model.func_177554_e().func_195668_m().func_110623_a().contains("missingno"),"Baked model "+item);}
            for(int[] size:new int[][]{{320,240},{427,267},{640,400},{1280,800}}){screen.func_231158_b_(mc,size[0],size[1]);screen.page=present/20;screen.candidate=-1;screen.func_231160_c_();click(screen,size[0],size[1],198+(present%20%4)*72+30,56+(present%20/4)*39+15);check(screen.candidate==present,"Matching input transform "+Arrays.toString(size));if(screen.pages()>1){screen.page=0;screen.func_231160_c_();click(screen,size[0],size[1],464,264);check(screen.page==1,"Next page input");}}mc.func_147108_a(screen);screen.page=0;screen.func_231160_c_();
            System.out.println("R7_CLIENT_PASS actual screen, four GUI scales and paging, empty inventory disabled, valid confirmation, exact tooltip, three baked item models");}
        if(age==55)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-r7-moving-agreement.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
        if(age==60){var screen=(MovingAgreementScreen)mc.field_71462_r;int w=mc.func_228018_at_().func_198107_o(),h=mc.func_228018_at_().func_198087_p();click(screen,w,h,408,313);}
        if(age==80){var screen=(MovingAgreementScreen)mc.field_71462_r;check(screen.data.feedback==1&&screen.data.agreements==3,"Real network confirmation reply and cost");check(screen.data.current.get(screen.data.ids.indexOf(pokemonId)).equals("ultra_ball"),"Server snapshot changed to selected ball");var client=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party.get(pokemonId);check(client!=null&&client.getBall().getName().equals("ultra_ball"),"Native party ball sync reaches client");System.out.println("R7_NETWORK_PASS actual scaled confirm click, server debit, reply, native party ball synchronization");mc.func_147108_a(new Update180ArtScreen());}if(age==90)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,"qa-r7-items.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});if(age>100)mc.func_71400_g();
    }
    static void click(MovingAgreementScreen screen,int width,int height,float x,float y){float scale=Math.max(.1f,Math.min(1.25f,Math.min((width-24f)/500,(height-24f)/334)));screen.func_231044_a_((width-500*scale)/2+x*scale,(height-334*scale)/2+y*scale,0);}
}
