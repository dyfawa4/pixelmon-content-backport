package local.pixelmon.backport;

import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.items.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** A party-only, server-authoritative change of the native capture-ball field. */
public final class MovingAgreement extends PixelmonItem {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"moving_agreement"),()->"1","1"::equals,"1"::equals);
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Set<String> BLACKLIST=Set.of("park_ball","strange_ball","origin_ball");
    public MovingAgreement(){super(new Item.Properties().func_200916_a(Backport.CREATIVE_TAB));}
    public static void register(){
        CHANNEL.registerMessage(0,Request.class,Request::encode,Request::decode,Request::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,State.class,State::encode,State::decode,State::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events());
    }
    public static boolean allowed(PokeBall ball){return ball!=null && !BLACKLIST.contains(ball.getName());}
    public static List<PokeBall> choices(){List<PokeBall> balls=new ArrayList<>();for(PokeBall ball:PokeBallRegistry.getAll())if(allowed(ball))balls.add(ball);balls.sort(Comparator.comparing(PokeBall::getName));return balls;}
    public static String ball(Pokemon pokemon){return pokemon.getBall()==null?"":pokemon.getBall().getName();}
    private static PokeBall resolve(String name){for(PokeBall ball:choices())if(ball.getName().equals(name))return ball;return null;}
    public static boolean isBall(ItemStack stack,PokeBall ball){return !stack.func_190926_b() && stack.func_77973_b() instanceof PokeBallItem && PokeBallPart.getPokeBall(stack).map(b->b.getName().equals(ball.getName())).orElse(false);}
    public static int count(List<ItemStack> inventory,PokeBall ball){int count=0;for(ItemStack s:inventory)if(isBall(s,ball))count+=s.func_190916_E();return count;}
    private static boolean valid(ServerPlayerEntity player){return player.func_70089_S() && BattleRegistry.getBattle(player)==null;}
    private static int agreements(List<ItemStack> inventory){int count=0;for(ItemStack s:inventory)if(s.func_77973_b() instanceof MovingAgreement)count+=s.func_190916_E();return count;}
    @Override public ActionResult<ItemStack> func_77659_a(World world,PlayerEntity player,Hand hand){
        if(player instanceof ServerPlayerEntity){ServerPlayerEntity server=(ServerPlayerEntity)player;
            State opened=begin(server);if(opened!=null)send(server,opened);
            else player.func_146105_b(new TranslationTextComponent("pixelmon.backport.moving.battle"),true);
        }return ActionResult.func_226249_b_(player.func_184586_b(hand));
    }
    public static State begin(ServerPlayerEntity player){
        if(!valid(player))return null;try(PokeInventory inventory=new PokeInventory(player)){if(agreements(inventory.stacks)<1)return null;}
        Session session=new Session();SESSIONS.put(player.func_110124_au(),session);return state(player,session,0,true);
    }
    public static State submit(ServerPlayerEntity player,Request request){
        Session session=SESSIONS.get(player.func_110124_au());
        if(session==null||!session.token.equals(request.token)||session.revision!=request.revision)return null;
        session.revision++;boolean fresh=System.nanoTime()-session.accessed<300_000_000_000L;session.accessed=System.nanoTime();
        boolean changed=fresh&&change(player,request.pokemon,request.expected,request.selected);
        return state(player,session,changed?1:2,false);
    }
    @Override public void func_77624_a(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag){lines.add(new TranslationTextComponent("pixelmon.backport.moving.description").func_240699_a_(TextFormatting.GRAY));}
    @Override public ActionResultType func_111207_a(ItemStack stack,PlayerEntity player,net.minecraft.entity.LivingEntity entity,Hand hand){func_77659_a(player.field_70170_p,player,hand);return ActionResultType.SUCCESS;}
    /** All checks precede both costs. Even creative use requires and consumes the two items. */
    public static boolean change(ServerPlayerEntity player,UUID id,String expected,String selected){
        PokeBall target=resolve(selected);Pokemon pokemon=StorageProxy.getParty(player).get(id);
        if(!valid(player)||target==null||pokemon==null||pokemon.isEgg()||!player.func_110124_au().equals(pokemon.getOwnerPlayerUUID())||!ball(pokemon).equals(expected)||selected.equals(expected))return false;
        var entity=pokemon.getPixelmonEntity();if(entity.isPresent()&&entity.get().battleController!=null&&!entity.get().battleController.battleEnded)return false;
        try(PokeInventory inventory=new PokeInventory(player)){
            ItemStack agreement=ItemStack.field_190927_a,cost=ItemStack.field_190927_a;
            for(ItemStack stack:inventory.stacks){if(!stack.func_190926_b()&&stack.func_77973_b() instanceof MovingAgreement)agreement=stack;if(isBall(stack,target))cost=stack;}
            if(agreement.func_190926_b()||cost.func_190926_b())return false;
            pokemon.setBall(target);pokemon.markDirty(EnumUpdateType.Ball);
            cost.func_190918_g(1);agreement.func_190918_g(1);player.field_71071_by.func_70296_d();
            return true;
        }
    }
    private static State state(ServerPlayerEntity player,Session session,int feedback,boolean open){
        State state=new State();state.token=session.token;state.revision=session.revision;state.feedback=feedback;state.open=open;
        for(Pokemon p:StorageProxy.getParty(player).getAll())if(p!=null&&!p.isEgg()&&player.func_110124_au().equals(p.getOwnerPlayerUUID())){state.ids.add(p.getUUID());state.names.add(p.getDisplayName());state.current.add(ball(p));}
        try(PokeInventory inventory=new PokeInventory(player)){state.agreements=agreements(inventory.stacks);for(PokeBall ball:choices()){state.balls.add(ball.getName());state.counts.add(count(inventory.stacks,ball));}}
        return state;
    }
    private static void send(ServerPlayerEntity player,State state){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),state);}
    static final class Session {final UUID token=UUID.randomUUID();int revision;long accessed=System.nanoTime();}
    public static final class Events {@SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){SESSIONS.remove(event.getPlayer().func_110124_au());}}
    public static final class Request {
        public UUID token,pokemon;public int revision;public String expected,selected;
        public Request(UUID token,int revision,UUID pokemon,String expected,String selected){this.token=token;this.revision=revision;this.pokemon=pokemon;this.expected=expected;this.selected=selected;}
        static void encode(Request r,PacketBuffer b){b.func_179252_a(r.token);b.writeInt(r.revision);b.func_179252_a(r.pokemon);b.func_180714_a(r.expected);b.func_180714_a(r.selected);}
        static Request decode(PacketBuffer b){return new Request(b.func_179253_g(),b.readInt(),b.func_179253_g(),b.func_150789_c(128),b.func_150789_c(128));}
        static void handle(Request r,Supplier<NetworkEvent.Context> context){var c=context.get();c.enqueueWork(()->{
            ServerPlayerEntity p=c.getSender();if(p==null)return;State state=submit(p,r);if(state!=null)send(p,state);
        });c.setPacketHandled(true);}
    }
    public static final class State {
        public UUID token;public int revision,agreements,feedback;public boolean open;
        public final List<UUID> ids=new ArrayList<>();public final List<String> names=new ArrayList<>(),current=new ArrayList<>(),balls=new ArrayList<>();public final List<Integer> counts=new ArrayList<>();
        static void encode(State s,PacketBuffer b){b.func_179252_a(s.token);b.writeInt(s.revision);b.writeInt(s.agreements);b.writeByte(s.feedback);b.writeBoolean(s.open);b.writeByte(s.ids.size());for(int i=0;i<s.ids.size();i++){b.func_179252_a(s.ids.get(i));b.func_180714_a(s.names.get(i));b.func_180714_a(s.current.get(i));}b.writeInt(s.balls.size());for(int i=0;i<s.balls.size();i++){b.func_180714_a(s.balls.get(i));b.writeInt(s.counts.get(i));}}
        static State decode(PacketBuffer b){State s=new State();s.token=b.func_179253_g();s.revision=b.readInt();s.agreements=b.readInt();s.feedback=b.readUnsignedByte();s.open=b.readBoolean();int n=b.readUnsignedByte();if(n>6)throw new IllegalArgumentException("Invalid party");for(int i=0;i<n;i++){s.ids.add(b.func_179253_g());s.names.add(b.func_150789_c(512));s.current.add(b.func_150789_c(128));}n=b.readInt();if(n<0||n>512)throw new IllegalArgumentException("Invalid balls");for(int i=0;i<n;i++){s.balls.add(b.func_150789_c(128));s.counts.add(b.readInt());}return s;}
        static void handle(State s,Supplier<NetworkEvent.Context> context){var c=context.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->MovingAgreementScreen.receive(s)));c.setPacketHandled(true);}
    }
}
