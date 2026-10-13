package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** A separate, versioned channel: no modification to native packet layouts. */
public final class TeraNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"tera"),()->"8","8"::equals,"8"::equals);
    private TeraNetwork(){}
    public static void raidStatus(TeraRaidParticipant raid){if(raid.bc.simulateMode)return;for(ServerPlayerEntity player:raid.bc.getPlayerEntities())CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new RaidStatus(raid.bc.battleIndex,(int)Math.max(0,300-raid.rules.elapsed/20),raid.charge(raid.bc.getParticipantForEntity(player)),raid.rules.segments(),raid.stars));}
    private static final Map<BattleController,Snapshot> LAST=Collections.synchronizedMap(new WeakHashMap<>());
    public static void register(){
        CHANNEL.registerMessage(0,Request.class,Request::encode,Request::decode,Request::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,Reply.class,Reply::encode,Reply::decode,Reply::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2,Snapshot.class,Snapshot::encode,Snapshot::decode,Snapshot::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3,RaidStatus.class,RaidStatus::encode,RaidStatus::decode,RaidStatus::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4,RaidBurst.class,RaidBurst::encode,RaidBurst::decode,RaidBurst::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static class Request {
        public final int battle,operation,type;public final UUID pokemon;
        public Request(int battle,UUID pokemon,int operation,int type){this.battle=battle;this.pokemon=pokemon;this.operation=operation;this.type=type;}
        static void encode(Request m,PacketBuffer b){b.writeInt(m.battle);b.func_179252_a(m.pokemon);b.writeByte(m.operation);b.writeByte(m.type);}
        static Request decode(PacketBuffer b){return new Request(b.readInt(),b.func_179253_g(),b.readByte(),b.readByte());}
        static void handle(Request m,Supplier<NetworkEvent.Context> supplied){NetworkEvent.Context c=supplied.get();c.enqueueWork(()->{
            ServerPlayerEntity player=c.getSender();if(player==null)return;
            if(m.operation==2 || m.operation==3){
                if(BattleRegistry.getBattle(player)!=null)return;
                List<Pokemon> party=new ArrayList<>();for(Pokemon p:StorageProxy.getParty(player).getAll())if(p!=null && !p.isEgg())party.add(p);
                boolean changed=false;if(m.operation==3)for(Pokemon p:party)if(p.getUUID().equals(m.pokemon)){changed=TeraItems.change(player,p,m.type);break;}
                Reply response=Reply.party(party,player);response.feedback=m.operation==3?(changed?1:2):0;
                CHANNEL.send(PacketDistributor.PLAYER.with(()->player),response);return;
            }
            if(m.operation!=0 && m.operation!=1 && m.operation!=4)return;
            BattleController b=BattleRegistry.getBattle(player);
            if(b==null || b.battleEnded || b.battleIndex!=m.battle)return;
            BattleParticipant participant=b.getParticipantForEntity(player);if(!(participant instanceof PlayerParticipant))return;
            PixelmonWrapper p=participant.getPokemonFromUUID(m.pokemon);
            if(p==null || !p.onBattlefield || !p.isAlive())return;
            if(m.operation==4 && p.wait)Tera.cancelSelection(p);
            if(m.operation==1 && p.wait && !p.isSwitching && !p.willTryFlee && p.willUseItemInStack==null && TeraItems.hasOrb(player))Tera.select(p);
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),Reply.battle(p,TeraItems.hasOrb(player)));
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),Snapshot.of(b).forPlayer(b,player));
        });c.setPacketHandled(true);}
    }
    public static void sync(BattleController b,boolean ended){
        if(b.simulateMode)return;Snapshot r=ended?new Snapshot(b.battleIndex):Snapshot.of(b);
        if(ended)LAST.remove(b);else LAST.put(b,r);
        Set<ServerPlayerEntity> audience=new LinkedHashSet<>(b.getPlayerEntities());
        for(Spectator spectator:b.spectators)audience.add(spectator.getEntity());
        for(ServerPlayerEntity player:audience)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),r.forPlayer(b,player));
    }
    public static void refresh(BattleController b){
        if(b.simulateMode || b.battleEnded)return;
        Snapshot now=Snapshot.of(b),old=LAST.get(b);
        if(old==null || !old.defense.equals(now.defense) || !old.active.equals(now.active) || !old.attackStats.equals(now.attackStats) || !old.burst.equals(now.burst) || !old.burstBalls.equals(now.burstBalls) || !old.burstElectricMoves.equals(now.burstElectricMoves))sync(b,false);
    }
    public static final class Snapshot {
        public final int battle;
        public final Map<UUID,List<Element>> defense=new LinkedHashMap<>();
        public final Map<UUID,Integer> active=new LinkedHashMap<>();
        // Compare the same stats used by the server's dynamic move effects.
        // Bit 0 includes stages (Tera Blast); bit 1 uses raw stats (Starstorm).
        public final Map<UUID,Integer> attackStats=new LinkedHashMap<>();
        // Battle-local, server-authoritative masks: 1 electric-move eligibility,
        // 2 wild capture eligibility, 4 target is the cap species (restricted balls),
        // 8 native grounded terrain eligibility, 16 the burst field is active (native terrain also benefits the other side).
        public final Map<UUID,Integer> burst=new LinkedHashMap<>();
        public final Map<UUID,List<String>> burstBalls=new LinkedHashMap<>();
        public final Map<UUID,List<String>> burstElectricMoves=new LinkedHashMap<>();
        public Snapshot forPlayer(BattleController b,ServerPlayerEntity player){
            Snapshot result=new Snapshot(battle);result.defense.putAll(defense);result.active.putAll(active);result.attackStats.putAll(attackStats);result.burst.putAll(burst);result.burstBalls.putAll(burstBalls);
            var owner=b.getParticipantForEntity(player);if(owner instanceof PlayerParticipant)for(var p:owner.allPokemon)if(burstElectricMoves.containsKey(p.getPokemonUUID()))result.burstElectricMoves.put(p.getPokemonUUID(),burstElectricMoves.get(p.getPokemonUUID()));return result;
        }
        public Snapshot(int battle){this.battle=battle;}
        public static Snapshot of(BattleController b){Snapshot r=new Snapshot(b.battleIndex);Map<BattleParticipant,List<String>> ballCache=new HashMap<>();for(BattleParticipant team:b.participants)for(PixelmonWrapper p:team.allPokemon)if(p.onBattlefield){r.defense.put(p.getPokemonUUID(),new ArrayList<>(p.type));if(Tera.active(p))r.active.put(p.getPokemonUUID(),Arrays.asList(Tera.TYPES).indexOf(Tera.state(p).type));int flags=p.getBattleStats().getStatWithMod(BattleStatsType.ATTACK)>p.getBattleStats().getStatWithMod(BattleStatsType.SPECIAL_ATTACK)?1:0;if(p.getStats().getAttack()>p.getStats().getSpecialAttack())flags|=2;r.attackStats.put(p.getPokemonUUID(),flags);
            BurstTerrain field=BurstTerrain.current(b);if(field!=null && p.isAlive()){if(team instanceof PlayerParticipant)r.burstElectricMoves.put(p.getPokemonUUID(),BurstMovePreview.electricMoves(p));int benefits=16|(team.team==field.team && p.hasType(Element.ELECTRIC)?1:0)|(field.benefitsGrounded(p)?8:0);
                if(team.team==field.team && team instanceof PlayerParticipant)for(BattleParticipant opponent:team.getOpponents())if(opponent instanceof WildPixelmonParticipant)for(PixelmonWrapper target:opponent.allPokemon)
                    if(OriginMercy.canCapture((PlayerParticipant)team,target)){benefits|=2;if(target.getSpecies().getDex()==CapSpecies.DEX)benefits|=4;}
                r.burst.put(p.getPokemonUUID(),benefits);
                if((benefits&2)!=0)r.burstBalls.put(p.getPokemonUUID(),ballCache.computeIfAbsent(team,t->BurstCapture.outlinedBalls((PlayerParticipant)t)));
            }
        }return r;}
        public static void encode(Snapshot r,PacketBuffer b){b.writeInt(r.battle);b.writeByte(r.defense.size());for(Map.Entry<UUID,List<Element>> e:r.defense.entrySet()){b.func_179252_a(e.getKey());b.writeByte(r.active.getOrDefault(e.getKey(),-1));b.writeByte(r.attackStats.getOrDefault(e.getKey(),0));b.writeByte(r.burst.getOrDefault(e.getKey(),0));b.writeByte(e.getValue().size());for(Element t:e.getValue())b.writeByte(t.ordinal());List<String> balls=r.burstBalls.getOrDefault(e.getKey(),Collections.emptyList());b.writeShort(balls.size());for(String ball:balls)b.func_180714_a(ball);List<String> electric=r.burstElectricMoves.get(e.getKey());b.writeByte(electric==null?255:electric.size());if(electric!=null)for(String move:electric)b.func_180714_a(move);}}
        public static Snapshot decode(PacketBuffer b){Snapshot r=new Snapshot(b.readInt());int n=b.readUnsignedByte();if(n>24)throw new IllegalArgumentException("Battle snapshot size");for(int i=0;i<n;i++){UUID id=b.func_179253_g();int active=b.readByte();if(active < -1 || active>=Tera.TYPES.length)throw new IllegalArgumentException("Active Tera type");int flags=b.readUnsignedByte();if(flags>3)throw new IllegalArgumentException("Attack stats flags");int benefits=b.readUnsignedByte();if(benefits>31)throw new IllegalArgumentException("Burst benefit flags");int count=b.readUnsignedByte();if(count>3)throw new IllegalArgumentException("Defense types");List<Element> types=new ArrayList<>();for(int j=0;j<count;j++){int type=b.readUnsignedByte();if(type>=Element.values().length)throw new IllegalArgumentException("Element");types.add(Element.values()[type]);}int ballCount=b.readUnsignedShort();if(ballCount<0 || ballCount>128)throw new IllegalArgumentException("Burst ball list");List<String> balls=new ArrayList<>();for(int j=0;j<ballCount;j++)balls.add(b.func_150789_c(128));if(ballCount!=0)r.burstBalls.put(id,balls);int moves=b.readUnsignedByte();if(moves!=255){if(moves>32)throw new IllegalArgumentException("Burst move preview size");List<String> electric=new ArrayList<>();for(int j=0;j<moves;j++)electric.add(b.func_150789_c(128));r.burstElectricMoves.put(id,electric);}r.defense.put(id,types);r.attackStats.put(id,flags);if(benefits!=0)r.burst.put(id,benefits);if(active>=0)r.active.put(id,active);}return r;}
        static void handle(Snapshot r,Supplier<NetworkEvent.Context> supplied){NetworkEvent.Context c=supplied.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->TeraClient.snapshot=r));c.setPacketHandled(true);}
    }
    public static final class RaidBurst {
        public final UUID raid;public final List<String> balls;
        public RaidBurst(UUID raid,List<String> balls){this.raid=raid;this.balls=balls;}
        static void encode(RaidBurst r,PacketBuffer b){b.func_179252_a(r.raid);b.writeShort(r.balls.size());for(String name:r.balls)b.func_180714_a(name);}
        static RaidBurst decode(PacketBuffer b){UUID raid=b.func_179253_g();int count=b.readUnsignedShort();if(count>128)throw new IllegalArgumentException("Raid burst balls");List<String> balls=new ArrayList<>();for(int i=0;i<count;i++)balls.add(b.func_150789_c(128));return new RaidBurst(raid,balls);}
        static void handle(RaidBurst r,Supplier<NetworkEvent.Context> supplied){NetworkEvent.Context c=supplied.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->TeraClient.raidBurst=r));c.setPacketHandled(true);}
    }
    public static class Reply {
        public int battle=-1;public UUID pokemon=new UUID(0,0);public boolean eligible,selected,active,used;public int type;
        public final List<UUID> ids=new ArrayList<>();public final List<ITextComponent> names=new ArrayList<>();public final List<Integer> types=new ArrayList<>();
        public final List<Integer> locked=new ArrayList<>();public final int[] shards=new int[19];public boolean anyOrb,chargedOrb,creative;public int feedback;
        static Reply battle(PixelmonWrapper p,boolean orb){Reply r=new Reply();r.battle=p.bc.battleIndex;r.pokemon=p.getPokemonUUID();r.eligible=orb&&p.wait&&!p.isSwitching&&!p.willTryFlee&&p.willUseItemInStack==null&&Tera.eligible(p);r.selected=Tera.selected(p);r.active=Tera.active(p);r.used=Tera.used(p.getParticipant());r.type=Arrays.asList(Tera.TYPES).indexOf(Tera.type(p.pokemon));return r;}
        static Reply party(List<Pokemon> party){Reply r=new Reply();for(Pokemon p:party){r.ids.add(p.getUUID());r.names.add(p.getFormattedDisplayName());r.types.add(Arrays.asList(Tera.TYPES).indexOf(Tera.type(p)));r.locked.add(p.getSpecies().getDex()==1017?1:p.getSpecies().getDex()==1024?2:0);}return r;}
        static Reply party(List<Pokemon> party,ServerPlayerEntity player){Reply r=party(party);r.anyOrb=TeraItems.hasAnyOrb(player);r.chargedOrb=TeraItems.hasOrb(player);r.creative=player.field_71075_bZ.field_75098_d;try(PokeInventory inventory=new PokeInventory(player)){for(net.minecraft.item.ItemStack stack:inventory.stacks)for(int type=0;type<19;type++)if(stack.func_77973_b()==Backport.REGISTERED.get(TeraItems.NAMES[type]+"_tera_shard").get())r.shards[type]=Math.min(1000000,r.shards[type]+stack.func_190916_E());}return r;}
        static void encode(Reply m,PacketBuffer b){b.writeInt(m.battle);b.func_179252_a(m.pokemon);b.writeBoolean(m.eligible);b.writeBoolean(m.selected);b.writeBoolean(m.active);b.writeBoolean(m.used);b.writeByte(m.type);b.writeByte(m.ids.size());for(int i=0;i<m.ids.size();i++){b.func_179252_a(m.ids.get(i));b.func_179256_a(m.names.get(i));b.writeByte(m.types.get(i));b.writeByte(m.locked.get(i));}b.writeBoolean(m.anyOrb);b.writeBoolean(m.chargedOrb);b.writeBoolean(m.creative);b.writeByte(m.feedback);for(int count:m.shards)b.writeInt(count);}
        static Reply decode(PacketBuffer b){Reply r=new Reply();r.battle=b.readInt();r.pokemon=b.func_179253_g();r.eligible=b.readBoolean();r.selected=b.readBoolean();r.active=b.readBoolean();r.used=b.readBoolean();r.type=b.readByte();int n=b.readUnsignedByte();if(n>6)throw new IllegalArgumentException("Party size");for(int i=0;i<n;i++){r.ids.add(b.func_179253_g());r.names.add(b.func_179258_d());int type=b.readUnsignedByte(),lock=b.readUnsignedByte();if(type>=19 || lock>2)throw new IllegalArgumentException("Tera party metadata");r.types.add(type);r.locked.add(lock);}r.anyOrb=b.readBoolean();r.chargedOrb=b.readBoolean();r.creative=b.readBoolean();r.feedback=b.readUnsignedByte();if(r.feedback>2)throw new IllegalArgumentException("Tera feedback");for(int i=0;i<19;i++){r.shards[i]=b.readInt();if(r.shards[i]<0 || r.shards[i]>1000000)throw new IllegalArgumentException("Shard count");}return r;}
        static void handle(Reply m,Supplier<NetworkEvent.Context> supplied){NetworkEvent.Context c=supplied.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->TeraClient.accept(m)));c.setPacketHandled(true);}
    }
    public static final class RaidStatus {
        public final int battle,seconds,charge,shield,stars;
        public RaidStatus(int battle,int seconds,int charge,int shield,int stars){this.battle=battle;this.seconds=seconds;this.charge=charge;this.shield=shield;this.stars=stars;}
        static void encode(RaidStatus r,PacketBuffer b){b.writeInt(r.battle);b.writeShort(r.seconds);b.writeByte(r.charge);b.writeByte(r.shield);b.writeByte(r.stars);}
        static RaidStatus decode(PacketBuffer b){int battle=b.readInt(),seconds=b.readUnsignedShort(),charge=b.readUnsignedByte(),shield=b.readUnsignedByte(),stars=b.readUnsignedByte();if(seconds>300 || charge>3 || shield>8 || stars<1 || stars>6)throw new IllegalArgumentException("Tera raid status");return new RaidStatus(battle,seconds,charge,shield,stars);}
        static void handle(RaidStatus r,Supplier<NetworkEvent.Context> supplied){NetworkEvent.Context c=supplied.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->TeraRaidClient.status=r));c.setPacketHandled(true);}
    }
}
