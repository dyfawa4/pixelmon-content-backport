package qa;

import java.nio.file.*;
import java.util.*;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.boss.dragon.*;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.enchantment.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.*;
import net.minecraft.item.crafting.RepairItemRecipe;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

/** Disposable native world only. Never loaded by production builds. */
@Mod("backport_divine190_probe") @Mod.EventBusSubscriber(modid="backport_divine190_probe")
public final class DivineSword190Probe {
    static final String WORLD="backport-divine-sword190";
    static boolean opened,hidden,ran;static volatile boolean failed;static volatile int stage;static int wait,age;
    static int witherDeaths,witherStars,dragonXp;static boolean attributed=true;static ServerPlayerEntity player;static EnderDragonEntity dying;
    static final Set<UUID> watched=new HashSet<>();
    static final Set<UUID> swordKills=new HashSet<>(),deathsSeen=new HashSet<>();
    static boolean enabled(){return WORLD.equals(System.getProperty("backport.manual.world"));}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void log(String value){System.out.println(value);try{Files.writeString(Path.of("divine190-result.txt"),value+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static ItemStack sword(){return new ItemStack(Backport.item("divine_sword"));}
    static Entity spawn(ServerWorld world,String id,double x){world.func_175726_f(new BlockPos(x,90,0));var e=ForgeRegistries.ENTITIES.getValue(new ResourceLocation(id)).func_200721_a(world);e.func_70012_b(x,90,0,0,0);watched.add(e.func_110124_au());check(world.func_217376_c(e),"Native entity joined private world: "+id);return e;}
    /** Allow only our disposable fixtures through Pixelmon's vanilla-spawn filter. */
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST,receiveCanceled=true)
    public static void fixtureJoin(net.minecraftforge.event.entity.EntityJoinWorldEvent event){
        if(enabled()&&watched.contains(event.getEntity().func_110124_au()))event.setCanceled(false);
        if(enabled()&&!event.getWorld().field_72995_K&&event.getEntity() instanceof net.minecraft.entity.item.ExperienceOrbEntity&&Math.abs(event.getEntity().func_226277_ct_())<8&&Math.abs(event.getEntity().func_226281_cx_())<8)dragonXp+=((net.minecraft.entity.item.ExperienceOrbEntity)event.getEntity()).field_70530_e;
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){
        if(!enabled()||!swordKills.contains(event.getEntityLiving().func_110124_au())||!deathsSeen.add(event.getEntityLiving().func_110124_au()))return;
        if(event.getSource().func_76346_g()!=player)log("DIVINE190_CREDIT_DEBUG entity="+event.getEntityLiving().func_200600_R().getRegistryName()+" source="+event.getSource()+" actual="+event.getSource().func_76346_g());
        attributed&=event.getSource().func_76346_g()==player;
        if(event.getEntityLiving() instanceof WitherEntity)witherDeaths++;
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event){
        if(!enabled()||!swordKills.contains(event.getEntityLiving().func_110124_au()))return;
        for(var e:event.getDrops()){log("DIVINE190_DROP_DEBUG "+e.func_92059_d());if(e.func_92059_d().func_77973_b().getRegistryName().toString().equals("minecraft:nether_star"))witherStars+=e.func_92059_d().func_190916_E();}
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END)return;
        var server=ServerLifecycleHooks.getCurrentServer();
        if(server==null||server.func_184103_al().func_181057_v().isEmpty()||!server.func_240776_a_(net.minecraft.world.storage.FolderName.field_237253_i_).normalize().getFileName().toString().equals(WORLD))return;
        if(Files.exists(Path.of("sword-model-only"))){if(!ran){ran=true;log("DIVINE190_MODEL_PREVIEW display-only, gameplay checks skipped");stage=2;}return;}
        try{
            if(!ran){if(++wait<100)return;ran=true;player=server.func_184103_al().func_181057_v().get(0);run(player);stage=1;wait=0;}
            else if(stage==1){
                if(++wait<300)return;
                check(dying.field_70128_L,"Dragon native death animation completed and removed normally; hp="+dying.func_110143_aJ()+" deathTicks="+dying.field_70995_bG+" age="+dying.field_70173_aa+" pos="+dying.func_233580_cy_());
                check(witherDeaths>=2&&witherStars>=2&&attributed,"Wither deaths="+witherDeaths+", native nether stars="+witherStars+", player damage credit="+attributed);
                check(dragonXp>0,"Native dragon death XP spawned");
                log("DIVINE190_NATIVE_PASS wither spawn immunity, dragon body/parts, 3-use break, no other attacks, native player-credit/death/nether-star drops; repair and enchant rejection");stage=2;
            }
        }catch(Throwable t){t.printStackTrace();log("DIVINE190_FAIL "+t);failed=true;stage=2;}
    }
    static void run(ServerPlayerEntity p){
        var world=p.func_71121_q();p.func_184102_h().func_195571_aL().func_197059_a(p.func_195051_bN().func_197033_a(4).func_197031_a(),"gamerule doMobLoot true");p.func_71033_a(GameType.SURVIVAL);p.func_70606_j(p.func_110138_aP());
        var s=sword();Item item=s.func_77973_b();p.func_184611_a(Hand.MAIN_HAND,s);
        check(item instanceof DivineSword&&!(item instanceof SwordItem)&&s.func_77958_k()==3&&s.func_77976_d()==1,"Independent item, damage3, unstackable");
        check(!s.func_77956_u()&&!item.isRepairable(s)&&!item.isBookEnchantable(s,new ItemStack(Items.field_151134_bR))&&!item.canApplyAtEnchantingTable(s,Enchantments.field_185296_A),"Reject repair, books and enchant table");
        var illegal=sword();illegal.func_77966_a(Enchantments.field_185296_A,1);illegal.func_77966_a(ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft:unbreaking")),3);illegal.func_196082_o().func_74757_a("Unbreakable",true);illegal.func_196082_o().func_74768_a(DivineSword.SPENT,2);illegal.func_196085_b(0);DivineSword.sanitize(illegal);
        check(illegal.func_77952_i()==2&&!illegal.func_77978_p().func_74764_b("Enchantments")&&!illegal.func_77978_p().func_74764_b("Unbreakable"),"Injected mending/unbreaking stripped, spent count cannot be repaired");
        var crafting=new CraftingInventory(new Container(null,-1){public boolean func_75145_c(net.minecraft.entity.player.PlayerEntity who){return false;}},2,1);var a=sword();a.func_196085_b(2);crafting.func_70299_a(0,a);crafting.func_70299_a(1,a.func_77946_l());
        check(!new RepairItemRecipe(new ResourceLocation("minecraft:repair_item")).func_77569_a(crafting,world),"Native two-item crafting repair rejected");
        var anvil=new net.minecraftforge.event.AnvilUpdateEvent(a,new ItemStack(Items.field_151134_bR),"",0,p);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(anvil);check(anvil.isCanceled(),"Native anvil event cancels repair/enchant/rename");
        var pig=spawn(world,"minecraft:pig",20);float health=((LivingEntity)pig).func_110143_aJ();p.func_71059_n(pig);check(((LivingEntity)pig).func_110143_aJ()==health&&DivineSword.spent(s)==0,"Ordinary mob cannot be attacked");
        var frame=spawn(world,"minecraft:armor_stand",22);p.func_71059_n(frame);check(frame.func_70089_S()&&DivineSword.spent(s)==0,"Other entity cannot be attacked");
        var first=(WitherEntity)spawn(world,"minecraft:wither",24);swordKills.add(first.func_110124_au());first.func_82215_s(200);first.func_195064_c(new net.minecraft.potion.EffectInstance(net.minecraft.potion.Effects.field_76429_m,1000,4));first.func_110149_m(64);p.func_71059_n(first);
        check(!first.func_70089_S()&&DivineSword.spent(s)==1&&s.func_77952_i()==1,"Spawn-protected armored/resistant Wither killed once, 1 use");
        dying=(EnderDragonEntity)spawn(world,"minecraft:ender_dragon",0);swordKills.add(dying.func_110124_au());dying.func_184670_cT().func_188758_a(PhaseType.field_188741_a);
        var body=dying.func_213404_dT()[2];p.func_71059_n(body);
        check((!dying.func_70089_S()||dying.func_184670_cT().func_188756_a().func_188652_i()==PhaseType.field_188750_j)&&DivineSword.spent(s)==2,"Body hit lethal, exactly1 use; hp="+dying.func_110143_aJ()+" phase="+dying.func_184670_cT().func_188756_a().func_188652_i()+" spent="+DivineSword.spent(s));
        p.func_71059_n(body);check(DivineSword.spent(s)==2,"Repeated dying dragon hit consumes nothing");
        var third=(WitherEntity)spawn(world,"minecraft:wither",26);swordKills.add(third.func_110124_au());p.func_71059_n(third);check(!third.func_70089_S()&&s.func_190926_b(),"Third success breaks sword");
        var fourth=(WitherEntity)spawn(world,"minecraft:wither",28);check(!DivineSword.strike(p,fourth,s)&&fourth.func_70089_S(),"Exhausted sword has no fourth use");fourth.func_70106_y();
        for(int i=0;i<8;i++){
            var blade=sword();p.func_184611_a(Hand.MAIN_HAND,blade);var d=(EnderDragonEntity)spawn(world,"minecraft:ender_dragon",40+i*16);d.func_184670_cT().func_188758_a(PhaseType.field_188751_k);p.func_71059_n(d.func_213404_dT()[i]);
            check((!d.func_70089_S()||d.func_184670_cT().func_188756_a().func_188652_i()==PhaseType.field_188750_j)&&DivineSword.spent(blade)==1,"Dragon part "+i+" lethal; hp="+d.func_110143_aJ()+" spent="+DivineSword.spent(blade));d.func_70106_y();
        }
        p.func_184611_a(Hand.MAIN_HAND,sword());p.func_71033_a(GameType.CREATIVE);p.func_200619_a(world,0,92,12,180,0);
        log("DIVINE190_SERVER_PASS native entities, all8 dragon parts, immune Wither, anti-repair and3 successful uses");
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent event){
        if(!enabled()||event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();
        mc.field_71474_y.field_82881_y=false;
        if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}
        if(!opened&&mc.field_71462_r instanceof MainMenuScreen){opened=true;mc.func_238191_a_(WORLD);}
        if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen)mc.func_147108_a(null);}
        if(stage==2){
            age++;
            if(age==5&&!failed)mc.func_147108_a(new Preview());
            if(age==22&&!failed)net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,Files.exists(Path.of("sword-model-only"))?"divine-sword190-model-r2.png":"divine-sword190.png",mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),message->{});
            if(age>40)mc.func_71400_g();
        }
    }
    static final class Preview extends net.minecraft.client.gui.screen.Screen {
        Preview(){super(new net.minecraft.util.text.StringTextComponent("无敌神剑"));}
        @Override public boolean func_231177_au__(){return false;}
        @Override public void func_230430_a_(com.mojang.blaze3d.matrix.MatrixStack matrices,int x,int y,float partial){
            func_238467_a_(matrices,0,0,field_230708_k_,field_230709_l_,0xff141b30);
            func_238472_a_(matrices,field_230712_o_,new net.minecraft.util.text.StringTextComponent("无敌神剑"),field_230708_k_/2,32,0xffead2ff);
            float scale=Math.max(3,Math.min(10,(field_230709_l_-110)/16f));
            com.mojang.blaze3d.systems.RenderSystem.pushMatrix();com.mojang.blaze3d.systems.RenderSystem.scalef(scale,scale,1);
            field_230707_j_.func_180450_b(sword(),(int)(field_230708_k_/(2*scale)-8),(int)(field_230709_l_/(2*scale)-8));
            com.mojang.blaze3d.systems.RenderSystem.popMatrix();
            func_238472_a_(matrices,field_230712_o_,new net.minecraft.util.text.StringTextComponent("仅末影龙 / 凋零 · 3次 · 无法附魔或修复"),field_230708_k_/2,field_230709_l_-26,0xffb7d7ee);
        }
    }
}
