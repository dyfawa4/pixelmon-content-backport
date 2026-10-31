package qa;

import java.util.*;
import java.nio.file.*;
import java.lang.reflect.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.events.*;
import com.pixelmonmod.pixelmon.api.events.raids.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.*;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import com.pixelmonmod.pixelmon.items.PokeBallPart;
import net.minecraft.item.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class Radiant190Cases {
    static int assertions,palettes,captures;
    static boolean viewed;
    static void render3d(com.mojang.blaze3d.matrix.MatrixStack ms,com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall b,int x){
        var mc=net.minecraft.client.Minecraft.func_71410_x();var entity=new com.pixelmonmod.pixelmon.entities.pokeballs.EmptyPokeBallEntity(mc.field_71441_e);
        com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallEntity.SYNC_POKEBALL_TYPE.set(entity,b);entity.field_70173_aa=5;entity.clientRotation=0;entity.setAnimation(com.pixelmonmod.pixelmon.client.models.smd.AnimationType.IDLE);
        var buf=net.minecraft.client.renderer.IRenderTypeBuffer.func_228455_a_(net.minecraft.client.renderer.Tessellator.func_178181_a().func_178180_c());
        ms.func_227860_a_();ms.func_227861_a_(x,70,200);ms.func_227862_a_(162f,-162f,162f);ms.func_227863_a_(new net.minecraft.util.math.vector.Quaternion(20,135,0,true));
        new com.pixelmonmod.pixelmon.client.render.entity.renderers.PokeBallRenderer(mc.func_175598_ae()).render(entity,0,0,ms,buf,15728880);buf.func_228461_a_();ms.func_227865_b_();
    }
    static void view(){if(viewed)return;viewed=true;var mc=net.minecraft.client.Minecraft.func_71410_x();if(mc.field_71439_g==null)return;mc.func_147108_a(new net.minecraft.client.gui.screen.Screen(new net.minecraft.util.text.StringTextComponent("Radiant Ball native assets")){
        @Override public void func_230430_a_(com.mojang.blaze3d.matrix.MatrixStack ms,int mx,int my,float partial){
            func_238467_a_(ms,0,0,field_230708_k_,field_230709_l_,0xff17202c);var b=PokeBallRegistry.getPokeBall("radiant_ball").getValueUnsafe();var old=PokeBallRegistry.getPokeBall("master_ball").getValueUnsafe();
            mc.func_175599_af().func_180450_b(b.getBallItem(),65,70);mc.func_175599_af().func_180450_b(old.getBallItem(),115,70);mc.func_175599_af().func_180450_b(new ItemStack(Backport.item("glow_scale_dust")),165,70);
            mc.func_110434_K().func_110577_a(b.getGUISprite());func_238463_a_(ms,60,110,0,0,64,64,64,64);mc.func_110434_K().func_110577_a(old.getGUISprite());func_238463_a_(ms,150,110,0,0,64,64,64,64);
            mc.field_71466_p.func_238421_b_(ms,"Radiant / Master / Glow Scale Dust",60,40,0xffffffff);
            render3d(ms,b,290);render3d(ms,old,430);super.func_230430_a_(ms,mx,my,partial);
        }
    });}

    static void check(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
    static Pokemon mon(int dex){return BossBalance190Probe.mon(dex,50,"Tackle");}
    static ItemStack item(String id){return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
    static int dust(ServerPlayerEntity p){int n=0;for(int i=0;i<36;i++){var s=p.field_71071_by.func_70301_a(i);if(s.func_77973_b() instanceof GlowScaleDust)n+=s.func_190916_E();}return n;}
    static void call(Object o,Class<?> type,String name)throws Exception{var m=type.getDeclaredMethod(name);m.setAccessible(true);m.invoke(o);}
    static void field(Object o,Class<?> type,String name,Object value)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    public static final class Cancel {
        @SubscribeEvent public void capture(CaptureEvent.SuccessfulCapture event){event.setCanceled(true);}
        @SubscribeEvent public void raid(CaptureEvent.SuccessfulRaidCapture event){event.setCanceled(true);}
    }
    public static void run(ServerPlayerEntity player)throws Exception{
        if(Files.exists(Path.of("radiant190-relog-ready"))){
            var party=StorageProxy.getParty(player);check(!party.get(0).isShiny()&&party.get(0).getBall().getName().equals("radiant_ball"),"Actual restart preserves nonshiny moved identity");check(party.get(1).isShiny()&&party.get(1).getBall().getName().equals("radiant_ball"),"Actual restart preserves shiny moved identity");check(Arrays.stream(StorageProxy.getPCForPlayer(player).getAll()).anyMatch(p->p!=null&&p.isShiny()&&p.getBall().getName().equals("radiant_ball")&&p.getPersistentData().func_74767_n("BackportRadiantCaptured")),"Actual restart preserves captured shiny in PC");Files.writeString(Path.of("radiant190-relog-results.json"),"{\"pass\":true,\"actualRestart\":true}");BossBalance190Probe.log("RADIANT190_RELOG_PASS actual party/PC load");return;
        }
        var ball=PokeBallRegistry.getPokeBall("radiant_ball").getValueUnsafe();
        check(ball!=null&&ball.getLogic() instanceof RadiantBallLogic&&ball.isGuaranteedCatch(),"Native data reload registers guaranteed ball and custom logic");
        check(MovingAgreement.choices().stream().anyMatch(b->b.getName().equals("radiant_ball")),"MovingAgreement includes new ball");
        check(PokeBallPart.getPokeBall(ball.getBallItem()).get().getName().equals("radiant_ball"),"Shared native item resolves custom identity");
        check(ball.getModel().equals("pixelmon:models/pokeballs/masterball.pqc")&&ball.getGUISprite().toString().equals("pixelmon_backport:textures/gui/overlay/radiant_ball.png"),"Native model and sidebar resources");
        var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,null);
        for(int i=0;i<36;i++)player.field_71071_by.func_70299_a(i,ItemStack.field_190927_a);
        recipe(player,ball);
        var karp=mon(129);for(var gender:com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.values()){if(gender==com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.NONE)continue;karp.setGender(gender);for(var palette:karp.getGenderProperties().getPalettes()){
            karp.setPalette(palette);boolean expected=palette.getName().equalsIgnoreCase("shiny");
            check(GlowScaleDust.qualifies(karp)==expected,"Magikarp palette eligibility "+palette.getName());
            loot(player,karp,expected);palettes++;
        }}
        for(int dex:new int[]{1,25,658,718,890})for(boolean shiny:new boolean[]{false,true}){var p=mon(dex);p.setShiny(shiny);loot(player,p,shiny);}
        var fancy=mon(25);if(fancy.hasPalette("christmas")){fancy.setPalette("christmas");check(!GlowScaleDust.qualifies(fancy),"Nonshiny decoration not broadly misclassified");}
        var nodrops=mon(129);nodrops.setShiny(true);nodrops.addFlag("nodrops");check(!GlowScaleDust.qualifies(nodrops),"Native noDrops respected");
        var owned=mon(129);owned.setShiny(true);party.set(0,owned);loot(player,owned,false);party.set(0,null);
        // The real native field calculation and success/storage sequence, without replacing any probability.
        for(int dex:new int[]{1,129,25})capture(player,ball,dex,false,false);
        capture(player,ball,1,true,false);capture(player,ball,129,false,true);raidCapture(player,ball,true,false);raidCapture(player,ball,true,true);raidCapture(player,ball,false,false);
        for(boolean shiny:new boolean[]{false,true}){
            var p=mon(129);p.setShiny(shiny);p.setBall(PokeBallRegistry.getPokeBall("poke_ball").getValueUnsafe());party.set(0,p);
            player.field_71071_by.func_70299_a(0,ball.getBallItem(2));player.field_71071_by.func_70299_a(1,new ItemStack(Backport.item("moving_agreement"),2));
            check(MovingAgreement.change(player,p.getUUID(),"poke_ball","radiant_ball"),"Native moving changes identity");
            check(p.isShiny()==shiny&&player.field_71071_by.func_70301_a(0).func_190916_E()==1&&player.field_71071_by.func_70301_a(1).func_190916_E()==1,"Moving preserves shiny and costs one of each");
            var restored=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));check(restored.isShiny()==shiny&&restored.getBall().getName().equals("radiant_ball"),"Moved ball identity and palette survive NBT");
            check(!MovingAgreement.change(player,p.getUUID(),"poke_ball","radiant_ball"),"Stale moving request cannot repeat costs");
        }
        capGuards(player,ball);
        var boss=mon(129);boss.setShiny(true);var bossEntity=new PixelmonEntity(player.field_70170_p,boss);bossEntity.setBossTier(com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry.getBossTierOrNotBoss("rare"));bossEntity.getPokemon().setShiny(true);check(bossEntity.getPokemon().isShiny(),"Boss fixture has actual shiny palette after native tier coloring");int beforeBoss=dust(player);DropItemQueryList.removeQuery(player);bossEntity.dropBossItems(player);DropItemQueryList.takeAllItems(player);check(dust(player)==beforeBoss,"Native boss reward claim excludes powder");bossEntity.func_70106_y();
        var cap=mon(CapSpecies.DEX);check(CapBallPreference.forbidden(ball,cap)&&!CapBallPreference.forbidden(ball,mon(25)),"Cap forbidden; ordinary Pikachu unaffected");
        var raidMon=mon(129);raidMon.setShiny(true);var rp=new com.pixelmonmod.pixelmon.battles.raids.RaidData.RaidPlayer(0,player,mon(1));
        var drops=new RaidDropsEvent(null,rp,null,raidMon,new ArrayList<>(),true);Pixelmon.EVENT_BUS.post(drops);
        check(drops.getDrops().stream().filter(s->s.func_77973_b() instanceof GlowScaleDust).mapToInt(ItemStack::func_190916_E).sum()==0,"Raid Boss reward excludes powder");
        for(int n=0;n<2;n++){var p=mon(129);p.setShiny(n==1);p.setBall(ball);party.set(n,p);}
        Files.writeString(Path.of("radiant190-relog-ready"),"native save/restart expected");
        String result="{\"assertions\":"+assertions+",\"magikarpPalettes\":"+palettes+",\"nativeCaptures\":"+captures+",\"pass\":true}";
        Files.writeString(Path.of("radiant190-results.json"),result);BossBalance190Probe.log("RADIANT190_PASS "+result);
    }
    static void capGuards(ServerPlayerEntity player,PokeBall ball)throws Exception{
        var p=mon(CapSpecies.DEX);var target=new PixelmonEntity(player.field_70170_p,p);target.func_70107_b(player.func_226277_ct_()+2,player.func_226278_cu_(),player.func_226281_cx_());player.func_71121_q().func_217376_c(target);
        var thrown=new EmptyPokeBallEntity(player.field_70170_p,player,ball,true);
        var box=new net.minecraft.util.math.AxisAlignedBB(player.func_233580_cy_()).func_186662_g(16);java.util.function.IntSupplier refunds=()->player.func_71121_q().func_217357_a(net.minecraft.entity.item.ItemEntity.class,box).stream().filter(e->PokeBallPart.getPokeBall(e.func_92059_d()).map(b->b.getName().equals("radiant_ball")).orElse(false)).mapToInt(e->e.func_92059_d().func_190916_E()).sum();int before=refunds.getAsInt();
        var impact=EmptyPokeBallEntity.class.getDeclaredMethod("func_70227_a",net.minecraft.util.math.RayTraceResult.class);impact.setAccessible(true);impact.invoke(thrown,new net.minecraft.util.math.EntityRayTraceResult(target));check(refunds.getAsInt()==before+1&&!p.isShiny()&&!target.hasOwner(),"Native field Cap rejection refunds exactly one ball");
        var party=StorageProxy.getParty(player);for(int n=0;n<6;n++)party.set(n,n==0?mon(143):null);var human=new com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant(player,party.get(0));var wild=new com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant(target);
        var bc=new com.pixelmonmod.pixelmon.battles.controller.BattleController(new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{human},new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{wild},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());bc.sendMessages=false;call(bc,bc.getClass(),"initBattle");
        try{var input=ball.getBallItem(2);player.field_71071_by.func_70299_a(0,input);var event=new com.pixelmonmod.pixelmon.api.events.battles.BagItemEvent.SelectItem.Pre(bc,"PokeBall",player,input,input);Pixelmon.EVENT_BUS.post(event);check(event.isCanceled()&&input.func_190916_E()==2&&!p.isShiny(),"Native battle selection rejects radiant Cap capture without consumption");}finally{bc.battleEnded=true;target.battleController=null;target.func_70106_y();}
    }
    static void raidCapture(ServerPlayerEntity player,PokeBall ball,boolean allowed,boolean cancel)throws Exception{
        var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,null);var p=mon(129);p.setShiny(false);
        var rp=new com.pixelmonmod.pixelmon.battles.raids.RaidData.RaidPlayer(0,player,mon(1));rp.possibleCatch=p;rp.canCatch=true;
        var raid=new com.pixelmonmod.pixelmon.battles.raids.RaidData(0,1,p.getSpecies());field(raid,raid.getClass(),"canUseMaster",allowed);raid.getPlayers().add(rp);player.field_71071_by.func_70299_a(0,ball.getBallItem(2));
        var listener=new Cancel();if(cancel)Pixelmon.EVENT_BUS.register(listener);try{raid.processAction(10,0,ball.getBallItem(),player);}finally{if(cancel)Pixelmon.EVENT_BUS.unregister(listener);}
        check(p.isShiny()==(allowed&&!cancel),"Native raid success/cancel/master-restriction shiny policy");check((party.get(p.getUUID())!=null)==(allowed&&!cancel),"Native raid actual storage");
    }
    static void recipe(ServerPlayerEntity player,PokeBall ball){
        var manager=player.func_71121_q().func_73046_m().func_199529_aN();
        var recipe=manager.func_215367_a(new ResourceLocation(Content.NS,"radiant_ball")).orElseThrow(()->new AssertionError("Recipe missing"));
        var inventory=new net.minecraft.inventory.CraftingInventory(new net.minecraft.inventory.container.Container(null,-1){public boolean func_75145_c(net.minecraft.entity.player.PlayerEntity p){return true;}},3,3);
        String[] ids={"minecraft:shroomlight","pixelmon_backport:glow_scale_dust","minecraft:glowstone","pixelmon:shiny_stone","pixelmon:poke_ball","pixelmon:luminous_moss","pixelmon:bright_powder","pixelmon:gold_bottle_cap","pixelmon:light_clay"};
        for(int i=0;i<9;i++)inventory.func_70299_a(i,i==4?MasterBallIngredient.modern():item(ids[i]));
        @SuppressWarnings("unchecked") var shaped=(net.minecraft.item.crafting.ICraftingRecipe)recipe;
        check(shaped.func_77569_a(inventory,player.field_70170_p),"Final nine ingredient shaped recipe matches");
        check(PokeBallPart.getPokeBall(shaped.func_77572_b(inventory)).get().getName().equals(ball.getName()),"Craft result has radiant NBT");
        inventory.func_70299_a(4,PokeBallRegistry.getPokeBall("poke_ball").getValueUnsafe().getBallItem());check(!shaped.func_77569_a(inventory,player.field_70170_p),"Ordinary shared ball is rejected");
        check(new MasterBallIngredient().test(item("pixelmon:master_ball")),"Legacy master item accepted");
    }
    static void loot(ServerPlayerEntity player,Pokemon p,boolean expected){
        int before=dust(player);DropItemQueryList.removeQuery(player);var e=new PixelmonEntity(player.field_70170_p,p);
        e.dropNormalItems(player);DropItemQueryList.takeAllItems(player);check(dust(player)-before==(expected?1:0),"Native normal drop and real claim "+p.getSpecies().getDex()+"/"+p.getPalette().getName());e.func_70106_y();
    }
    static void capture(ServerPlayerEntity player,PokeBall ball,int dex,boolean full,boolean cancel)throws Exception{
        var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,full?mon(4):null);
        var p=mon(dex);p.setShiny(false);p.getPersistentData().func_74757_a("BackportRadiantCaptured",true);var target=new PixelmonEntity(player.field_70170_p,p);target.func_70107_b(player.func_226277_ct_()+2,player.func_226278_cu_(),player.func_226281_cx_());player.func_71121_q().func_217376_c(target);
        var thrown=new EmptyPokeBallEntity(player.field_70170_p,player,ball,true);field(thrown,PokeBallEntity.class,"pixelmon",target);
        var calc=PokeBallEntity.class.getDeclaredMethod("doCaptureCalc",PixelmonEntity.class);calc.setAccessible(true);calc.invoke(thrown,target);
        var can=PokeBallEntity.class.getDeclaredField("canCatch");can.setAccessible(true);check(can.getBoolean(thrown),"Native guaranteed calculation succeeds");check(!p.isShiny(),"Calculation does not convert yet");
        var listener=new Cancel();if(cancel)Pixelmon.EVENT_BUS.register(listener);
        try{call(thrown,EmptyPokeBallEntity.class,"catchPokemon");}finally{if(cancel)Pixelmon.EVENT_BUS.unregister(listener);}
        if(cancel){check(!p.isShiny()&&p.getOwnerPlayer()==null,"Canceled successful event leaves wild color and owner unchanged");}
        else{call(thrown,EmptyPokeBallEntity.class,"storeCapture");check(p.isShiny()&&p.getBall().getName().equals("radiant_ball"),"Real successful store converts to shiny");var restored=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));check(restored.isShiny()&&restored.getBall().getName().equals("radiant_ball"),"Captured palette and ball persist");check(full?StorageProxy.getPCForPlayer(player).getPosition(p)!=null:party.get(p.getUUID())!=null,"Native party/PC insertion");captures++;}
        thrown.func_70106_y();target.func_70106_y();
    }
}

