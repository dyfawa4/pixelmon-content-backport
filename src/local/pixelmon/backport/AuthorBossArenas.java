package local.pixelmon.backport;
import java.util.*;
import java.lang.ref.WeakReference;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.api.events.BeatTrainerEvent;
import com.pixelmonmod.pixelmon.api.events.npc.NPCEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.world.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraft.world.server.ServerWorld;

/** Only a challenged arena is protected. State lives on its boss and survives world saves. */
public final class AuthorBossArenas {
    private static final Map<ServerWorld,List<WeakReference<Entity>>> WATCHED=new WeakHashMap<>();
    private static final String HOME="AuthorArenaHome",ACTIVE="AuthorArenaActive",OWNER="AuthorArenaOwner";
    public static void tag(Entity entity,BlockPos center){entity.getPersistentData().func_74772_a(HOME,center.func_218275_a());}
    public static BlockPos center(Entity entity){return entity.getPersistentData().func_74764_b(HOME)?BlockPos.func_218283_e(entity.getPersistentData().func_74763_f(HOME)):entity.func_233580_cy_();}
    public static boolean inside(Entity boss,BlockPos pos){BlockPos c=center(boss);return Math.abs(pos.func_177958_n()-c.func_177958_n())<=6&&Math.abs(pos.func_177952_p()-c.func_177952_p())<=6&&pos.func_177956_o()>=c.func_177956_o()-1&&pos.func_177956_o()<=c.func_177956_o()+8;}
    public static boolean authored(Entity e){return e instanceof AuthorCombatBoss||e instanceof NPCTrainer&&AuthorBossTeams.is((NPCTrainer)e);}
    public static void watch(Entity entity){
        if(!(entity.field_70170_p instanceof ServerWorld)||!authored(entity))return;
        var list=WATCHED.computeIfAbsent((ServerWorld)entity.field_70170_p,w->new ArrayList<>());
        if(list.stream().noneMatch(r->r.get()==entity))list.add(new WeakReference<>(entity));
    }
    public static void active(Entity boss,ServerPlayerEntity player){
        watch(boss);boss.getPersistentData().func_74757_a(ACTIVE,true);boss.getPersistentData().func_74778_a(OWNER,player.func_110124_au().toString());gate(boss,true);
    }
    public static void open(Entity boss){boss.getPersistentData().func_74757_a(ACTIVE,false);boss.getPersistentData().func_82580_o(OWNER);gate(boss,false);}
    private static void gate(Entity boss,boolean close){
        if(boss.field_70170_p.field_72995_K||!boss.getPersistentData().func_74764_b(HOME))return;
        BlockPos c=center(boss);
        for(int x=-1;x<=1;x++)for(int y=1;y<=3;y++){
            BlockPos p=c.func_177982_a(x,y,6);
            if(!boss.field_70170_p.func_175667_e(p))continue;
            var current=boss.field_70170_p.func_180495_p(p);
            if(close){if(current.func_196958_f()||current.func_177230_c()==Blocks.field_150411_aY)boss.field_70170_p.func_180501_a(p,Blocks.field_150411_aY.func_176223_P(),3);}
            else if(current.func_177230_c()==Blocks.field_150411_aY)boss.field_70170_p.func_180501_a(p,Blocks.field_150350_a.func_176223_P(),3);
        }
    }
    public static boolean protectedAt(net.minecraft.world.World world,BlockPos pos){
        if(!(world instanceof ServerWorld))return false;
        var list=WATCHED.get(world);if(list==null)return false;
        return list.stream().map(WeakReference::get).anyMatch(e->e!=null&&!e.field_70128_L&&e.getPersistentData().func_74767_n(ACTIVE)&&inside(e,pos));
    }
    @SubscribeEvent public void join(EntityJoinWorldEvent event){
        Entity e=event.getEntity();if(event.getWorld().field_72995_K||!authored(e))return;
        if(e instanceof AuthorCombatBoss){((AuthorCombatBoss)e).reset();return;}
        if(e.getPersistentData().func_74764_b(HOME)){
            var world=(ServerWorld)event.getWorld();AuthorRoomIndex.get(world).record(center(e),e instanceof AuthorCombatBoss);
            String material=e.getPersistentData().func_74779_i(AuthorRoomPalette.TAG);
            if(!AuthorRoomPalette.wall(material)){material=AuthorBossFeature.material(world,center(e),e.getPersistentData().func_74779_i("AuthorBossTheme"));e.getPersistentData().func_74778_a(AuthorRoomPalette.TAG,material);}
            AuthorBossFeature.upgrade(world,center(e),false,material);
            if(!e.getPersistentData().func_74767_n(AuthorRoomFurnishings.TAG)){
                AuthorRoomFurnishings.place(world,center(e));
                e.getPersistentData().func_74757_a(AuthorRoomFurnishings.TAG,true);
            }
        }
        watch(e);open(e);if(e instanceof NPCTrainer)((NPCTrainer)e).healAllPokemon();
    }
    @SubscribeEvent public void leave(EntityLeaveWorldEvent event){
        Entity e=event.getEntity();if(e.field_70170_p.field_72995_K||!authored(e))return;
        open(e);
        if(e instanceof NPCTrainer){NPCTrainer npc=(NPCTrainer)e;if(!npc.getPersistentData().func_74767_n("AuthorBossRewarded")&&npc.battleController!=null&&!npc.battleController.battleEnded)npc.battleController.endBattleWithoutXP();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void start(NPCEvent.StartBattle event){
        var participant=event.getNPCParticipant();if(event.isCanceled()||participant==null||!AuthorBossTeams.is(participant.trainer)||!(event.player instanceof ServerPlayerEntity))return;
        NPCTrainer npc=participant.trainer;
        String owner=npc.getPersistentData().func_74779_i(OWNER);
        if(npc.getPersistentData().func_74767_n(ACTIVE)&&!owner.isEmpty()&&!owner.equals(event.player.func_110124_au().toString())){event.setCanceled(true);return;}
        active(npc,(ServerPlayerEntity)event.player);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void beat(BeatTrainerEvent event){
        NPCTrainer npc=event.trainer;if(!AuthorBossTeams.is(npc)||npc.field_70128_L||npc.getPersistentData().func_74767_n("AuthorBossRewarded"))return;
        if(!npc.getPersistentData().func_74779_i(OWNER).equals(event.player.func_110124_au().toString()))return;
        npc.getPersistentData().func_74757_a("AuthorBossRewarded",true);open(npc);
        npc.setEncounterMode(com.pixelmonmod.pixelmon.enums.EnumEncounterMode.Once);
        npc.winMoney=0;npc.updateDrops(new net.minecraft.item.ItemStack[0]);
        AuthorBossRewards.trainer(event.player,npc);
        AuthorVictory.earned(event.player);
        // Native Once encounter processing removes this same trainer after the event.
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void end(NPCEvent.EndBattle event){
        var participant=event.getNPCParticipant();if(participant==null||!AuthorBossTeams.is(participant.trainer))return;
        // This event precedes TrainerParticipant.endBattle -> NPCTrainer.loseBattle.
        // Healing here changes the native win/loss decision; clearing OWNER drops the victory reward.
        NPCTrainer npc=participant.trainer;npc.getPersistentData().func_74757_a(ACTIVE,false);gate(npc,false);
    }
    @SubscribeEvent public void breakBlock(BlockEvent.BreakEvent event){if(protectedAt(event.getWorld() instanceof net.minecraft.world.World?(net.minecraft.world.World)event.getWorld():null,event.getPos()))event.setCanceled(true);}
    @SubscribeEvent public void place(BlockEvent.EntityPlaceEvent event){if(protectedAt(event.getWorld() instanceof net.minecraft.world.World?(net.minecraft.world.World)event.getWorld():null,event.getPos()))event.setCanceled(true);}
    @SubscribeEvent public void multiPlace(BlockEvent.EntityMultiPlaceEvent event){
        for(var snapshot:event.getReplacedBlockSnapshots())if(protectedAt(event.getWorld() instanceof net.minecraft.world.World?(net.minecraft.world.World)event.getWorld():null,snapshot.getPos())){event.setCanceled(true);break;}
    }
    @SubscribeEvent public void bucket(FillBucketEvent event){if(event.getTarget()!=null&&event.getTarget().func_216346_c()==net.minecraft.util.math.RayTraceResult.Type.BLOCK&&protectedAt(event.getWorld(),((net.minecraft.util.math.BlockRayTraceResult)event.getTarget()).func_216350_a()))event.setCanceled(true);}
    @SubscribeEvent public void explode(ExplosionEvent.Detonate event){event.getAffectedBlocks().removeIf(p->protectedAt(event.getWorld(),p));}
    @SubscribeEvent public void piston(PistonEvent.Pre event){
        net.minecraft.world.World world=event.getWorld() instanceof net.minecraft.world.World?(net.minecraft.world.World)event.getWorld():null;
        if(protectedAt(world,event.getPos())||protectedAt(world,event.getFaceOffsetPos())){event.setCanceled(true);return;}
        var helper=event.getStructureHelper();if(helper==null||!helper.func_177253_a())return;
        for(BlockPos p:helper.func_177254_c())if(protectedAt(world,p)||protectedAt(world,p.func_177972_a(event.getDirection()))){event.setCanceled(true);return;}
        for(BlockPos p:helper.func_177252_d())if(protectedAt(world,p)){event.setCanceled(true);return;}
    }
    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.world instanceof ServerWorld)||event.world.func_82737_E()%20!=0)return;
        ServerWorld world=(ServerWorld)event.world;var list=WATCHED.get(world);if(list==null)return;
        list.removeIf(r->r.get()==null||r.get().field_70128_L);
        for(var ref:list){Entity e=ref.get();if(!(e instanceof NPCTrainer)||!e.getPersistentData().func_74767_n(ACTIVE))continue;
            NPCTrainer npc=(NPCTrainer)e;ServerPlayerEntity player=null;
            try{player=world.func_73046_m().func_184103_al().func_177451_a(UUID.fromString(npc.getPersistentData().func_74779_i(OWNER)));}catch(IllegalArgumentException ignored){}
            if(player==null||!player.func_70089_S()||player.field_70170_p!=world||!inside(npc,player.func_233580_cy_())||npc.battleController==null){
                if(npc.battleController!=null&&!npc.battleController.battleEnded)npc.battleController.endBattleWithoutXP();
                open(npc);if(!npc.field_70128_L)npc.healAllPokemon();
            }
        }
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event){if(event.getWorld() instanceof ServerWorld)WATCHED.remove(event.getWorld());}
}
