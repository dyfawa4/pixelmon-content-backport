package local.pixelmon.backport;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Theme markers on gym templates distinguish equal gym trainers from ordinary equal NPCs. */
public final class GymCities {
    private GymCities(){}
    public static final String TYPE="BackportGymType";
    public static final Map<String,net.minecraft.world.gen.feature.StructureFeature<?,?>> FEATURES=new LinkedHashMap<>();
    public static void registerGeneration(){
        if(!FEATURES.isEmpty())return;
        for(Map.Entry<String,JsonElement> entry:DATA.getAsJsonObject("newThemes").entrySet()){
            String type=entry.getKey(),source=entry.getValue().getAsJsonObject().get("source").getAsString();
            net.minecraft.util.ResourceLocation poolId=new net.minecraft.util.ResourceLocation("pixelmon_backport","gyms/"+(type.equals("dark")?"natural/dark":type)+"/start_pool");
            net.minecraft.world.gen.feature.jigsaw.JigsawPattern pool=net.minecraft.world.gen.feature.jigsaw.JigsawPatternRegistry.func_244094_a(new net.minecraft.world.gen.feature.jigsaw.JigsawPattern(poolId,new net.minecraft.util.ResourceLocation("minecraft:empty"),Collections.emptyList()));
            int size=60;
            var config=new com.pixelmonmod.pixelmon.world.structure.type.GymConfig(()->pool,size,true);
            var feature=com.pixelmonmod.pixelmon.world.structure.PixelmonStructures.GYMS.get().func_236391_a_(config);
            net.minecraft.util.registry.WorldGenRegistries.func_243664_a(net.minecraft.util.registry.WorldGenRegistries.field_243654_f,new net.minecraft.util.ResourceLocation("pixelmon_backport",type+"_gym"),feature);
            FEATURES.put(type,feature);
        }
    }
    public static com.pixelmonmod.pixelmon.world.structure.type.GymConfig config(net.minecraft.util.registry.DynamicRegistries registries,com.pixelmonmod.pixelmon.world.structure.type.GymConfig original){
        net.minecraft.world.gen.feature.jigsaw.JigsawPattern start=original.func_242810_c().get();
        if(!start.func_214947_b().func_110624_b().equals("pixelmon_backport"))return original;
        net.minecraft.world.gen.feature.jigsaw.JigsawPattern loaded=registries.func_243612_b(net.minecraft.util.registry.Registry.field_243555_ax).func_82594_a(start.func_214947_b());
        return loaded==null || loaded.func_214946_c()==0?original:new com.pixelmonmod.pixelmon.world.structure.type.GymConfig(()->loaded,original.func_236534_a_(),original.doesTransformSurroundingLand());
    }
    public static final JsonObject DATA;
    static {
        try(InputStream in=GymCities.class.getClassLoader().getResourceAsStream("backport/gym_cities.json")){
            DATA=new JsonParser().parse(new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new IllegalStateException("Gym catalogue",e);}
    }
    public static Element theme(NPCTrainer npc){
        String stored=npc.getPersistentData().func_74779_i(TYPE);
        if(!stored.isEmpty())try{return Element.valueOf(stored.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){return null;}
        Pokemon first=npc.getPokemonStorage().get(0);if(first==null)return null;
        String name=npc.func_200200_C_().getString();
        for(JsonElement value:DATA.getAsJsonArray("nativeSignatures")){
            JsonObject entry=value.getAsJsonObject();
            if(entry.get("name").getAsString().equals(name) && entry.get("firstDex").getAsInt()==first.getSpecies().getDex()){
                String type=entry.get("type").getAsString();npc.getPersistentData().func_74778_a(TYPE,type);
                npc.getPersistentData().func_74778_a("BackportGymRole",entry.get("role").getAsString());return Element.valueOf(type.toUpperCase(Locale.ROOT));
            }
        }
        return npc.isGymLeader?first.getForm().getTypes().get(0):null;
    }
    public static void initialize(NPCTrainer npc){
        CompoundNBT data=npc.getPersistentData();
        Element theme=theme(npc);
        if(theme==Element.GROUND && "leader".equals(data.func_74779_i("BackportGymRole"))){
            net.minecraft.item.ItemStack[] rewards=npc.getWinnings().clone();boolean changed=false;
            for(int i=0;i<rewards.length;i++)if(rewards[i]!=null && !rewards[i].func_190926_b() && new net.minecraft.util.ResourceLocation("pixelmon","mine_badge").equals(rewards[i].func_77973_b().getRegistryName())){
                var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("pixelmon","jimen_badge"));
                if(item!=null){rewards[i]=new net.minecraft.item.ItemStack(item,Math.max(1,rewards[i].func_190916_E()));changed=true;}
            }
            if(changed)npc.updateDrops(rewards);
        }
        if(theme!=null && "leader".equals(data.func_74779_i("BackportGymRole")) && Arrays.stream(npc.getWinnings()).noneMatch(s->s!=null && !s.func_190926_b())){
            String type=theme.name().toLowerCase(Locale.ROOT);
            JsonElement badge=DATA.getAsJsonObject("nativeBadges").get(type);
            boolean authored=data.func_74767_n("BackportGymNew") && DATA.getAsJsonObject("newThemes").has(type);
            if(authored || badge!=null){
                net.minecraft.util.ResourceLocation id=authored?new net.minecraft.util.ResourceLocation("pixelmon_backport",type+"_gym_badge"):new net.minecraft.util.ResourceLocation("pixelmon",badge.getAsString());
                net.minecraft.item.Item item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(id);
                if(item!=null)npc.updateDrops(new net.minecraft.item.ItemStack[]{new net.minecraft.item.ItemStack(item)});
            }
        }
        if(!data.func_74767_n("BackportGymNew") || data.func_74767_n("BackportGymInitialized"))return;
        String type=data.func_74779_i(TYPE);JsonObject definition=DATA.getAsJsonObject("newThemes").getAsJsonObject(type);
        if(definition==null)return;
        List<Integer> candidates=new ArrayList<>();definition.getAsJsonArray("roster").forEach(v->candidates.add(v.getAsInt()));
        Collections.shuffle(candidates,new Random(npc.func_110124_au().getLeastSignificantBits()));
        Pokemon[] original=npc.getPokemonStorage().getAll();int slot=0;
        for(Pokemon previous:original)if(previous!=null){
            Pokemon pokemon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(candidates.get(slot%candidates.size())));
            pokemon.setLevelNum(previous.getPokemonLevel());pokemon.initialize();
            // Keep the source trainer's training values; species stats are recalculated natively.
            CompoundNBT training=new CompoundNBT();previous.getIVs().writeToNBT(training);previous.getEVs().writeToNBT(training);
            pokemon.getIVs().readFromNBT(training);pokemon.getEVs().readFromNBT(training);
            pokemon.getPersistentData().func_74757_a(NpcMoves.ACE,true);NpcMoves.configure(pokemon,npc.func_110124_au());
            pokemon.getStats().recalculateStats();pokemon.heal();npc.getPokemonStorage().set(slot++,pokemon);
        }
        data.func_74757_a("BackportGymInitialized",true);
    }
    public static final class Events {
        @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public void biome(net.minecraftforge.event.world.BiomeLoadingEvent event){
            if(event.getName()==null)return;
            for(Map.Entry<String,JsonElement> entry:DATA.getAsJsonObject("newThemes").entrySet()){
                boolean matches=false;for(JsonElement biome:entry.getValue().getAsJsonObject().getAsJsonArray("biomes"))matches|=biome.getAsString().equals(event.getName().toString());
                if(!matches)continue;
                var feature=FEATURES.get(entry.getKey());if(feature==null)throw new IllegalStateException("Gym features not registered");
                // One configuration per native city slot; the dark pool retains the grass candidate.
                event.getGeneration().getStructures().removeIf(s->s.get().field_236268_b_==com.pixelmonmod.pixelmon.world.structure.PixelmonStructures.GYMS.get());
                event.getGeneration().getStructures().add(()->feature);return;
            }
        }
        @SubscribeEvent public void join(EntityJoinWorldEvent event){
            if(!event.getWorld().field_72995_K && event.getEntity() instanceof NPCTrainer)initialize((NPCTrainer)event.getEntity());
        }
    }
}
