import java.io.*;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.*;
import java.util.jar.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.battles.raids.WeightedItemStacks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.util.ResourceLocation;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTier;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.fml.RegistryObject;
import net.minecraft.item.Item;

/** Verify loaded native pools, not only generated JSON files. */
public final class WorldContentVerification {
    private static void require(boolean value,String label){if(!value)throw new AssertionError(label);}
    private static JsonElement resource(String path)throws Exception{
        try(InputStream in=Content.class.getClassLoader().getResourceAsStream(path)){
            require(in!=null,"Missing resource "+path);
            return new JsonParser().parse(new InputStreamReader(in,StandardCharsets.UTF_8));
        }
    }
    private static boolean has(WeightedItemStacks pool,String item)throws Exception{
        require(pool!=null,"Missing reward pool "+item);
        Field field=WeightedItemStacks.class.getDeclaredField("stacks");field.setAccessible(true);
        for(Object row:(List<?>)field.get(pool)){
            ItemStack s=(ItemStack)((Tuple<?,?>)row).func_76340_b();
            if(!s.func_190926_b() && item.equals(s.func_77973_b().getRegistryName().toString()))return true;
        }
        return false;
    }
    public static void run()throws Exception{
        net.minecraft.util.NonNullList<ItemStack> creative=net.minecraft.util.NonNullList.func_191196_a();((BackportCreativeTab)Backport.CREATIVE_TAB).func_78018_a(creative);
        require(creative.size()==Backport.REGISTERED.size()+8,"Dedicated creative tab incomplete");
        for(RegistryObject<Item> registered:Backport.REGISTERED.values())require(creative.stream().anyMatch(s->s.func_77973_b()==Backport.item(registered.getId().func_110623_a())),"Creative tab missing "+registered.getId());
        require(creative.stream().filter(s->!(s.func_77973_b() instanceof DisposableTeraDisc) && TechnicalMoveItem.getMove(s)!=null).count()==8,"Creative tab missing native NBT TMs");
        String[] standard={"dipplin","poltchageist","sinistcha","archaludon","hydrapple","backport_gougingfire","backport_ragingbolt","backport_ironboulder","backport_ironcrown","backport_az_floette"};
        String[] legends={"ogerpon","backport_okidogi","backport_munkidori","backport_fezandipiti","backport_terapagos","backport_pecharunt"};
        int sets=0,infos=0;
        for(String group:new String[]{"standard","legendaries"})for(String name:group.equals("standard")?standard:legends){
            JsonObject json=resource("data/pixelmon/spawning/"+group+"/"+name+".set.json").getAsJsonObject();
            String id=json.get("id").getAsString();
            List<SpawnSet> pool=group.equals("standard")?PixelmonSpawning.standard:PixelmonSpawning.legendaries;
            SpawnSet loaded=pool.stream().filter(s->id.equals(s.id)).findFirst().orElse(null);
            require(loaded!=null && !loaded.spawnInfos.isEmpty(),"Not loaded into native "+group+" pool: "+id);
            require(loaded.spawnInfos.size()==json.getAsJsonArray("spawnInfos").size(),"Lost spawn entries "+id);
            for(int i=0;i<loaded.spawnInfos.size();i++){
                SpawnInfo info=loaded.spawnInfos.get(i);JsonObject source=json.getAsJsonArray("spawnInfos").get(i).getAsJsonObject();
                require(!info.locationTypes.isEmpty() && info.rarity>0,"Invalid spawn location/rarity "+id);
                JsonObject c=source.getAsJsonObject("condition");
                if(c!=null && (c.has("stringBiomes") || c.has("biomes")))require(!info.condition.biomes.isEmpty(),"Unresolved biome categories "+id+" entry "+i);
                ++infos;
            }
            ++sets;
        }
        for(String name:Content.DATA.getAsJsonArray("disabledPreviewSpecies").toString().replace("[","").replace("]","").replace("\"","").split(","))
            for(List<SpawnSet> pool:PixelmonSpawning.getAll().values())for(SpawnSet set:pool)require(!set.id.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)),"Disabled preview spawned: "+name);
        int rewards=0;
        for(int star=1;star<=6;star++)for(int type=0;type<19;type++){require(TeraRaids.rewards(star,type,new Random(1)).get(0).func_77973_b()==Backport.REGISTERED.get(TeraItems.NAMES[type]+"_tera_shard").get(),"Missing dedicated Tera shards");++rewards;}
        for(String name:new String[]{"clear_amulet","covert_cloak","fairy_feather","masterpiece_teacup","unremarkable_teacup","syrupy_apple","gimmighoul_coin","teal_mask","wellspring_mask","hearthflame_mask","cornerstone_mask","utility_umbrella"})
            require(DropItemRegistry.tier3.stream().anyMatch(s->(Content.NS+":"+name).equals(s.func_77973_b().getRegistryName().toString())),"Missing survival reward "+name);
        for(BackportTechnicalMoves move:BackportTechnicalMoves.values())require(DropItemRegistry.tier3.stream().anyMatch(s->TechnicalMoveItem.getMove(s)==move),"Missing survival TM "+move);
        int megaItems=0;
        Field biomeStrings=SpawnCondition.class.getDeclaredField("stringBiomes");biomeStrings.setAccessible(true);
        Field repeatChance=BossTier.class.getDeclaredField("megaStoneRepeatChance");repeatChance.setAccessible(true);
        Field firstChance=BossTier.class.getDeclaredField("megaStoneObtainChance");firstChance.setAccessible(true);
        boolean renewable=false;
        for(BossTier tier:BossTierRegistry.getBossTiers())if(tier.canRandomlySpawn() && tier.isMega() && firstChance.getDouble(tier)>0 && repeatChance.getDouble(tier)>0)renewable=true;
        require(renewable,"No native Mega boss tier permits repeated stones; second X/Y stone would be unobtainable");
        for(RegistryObject<Item> item:Backport.REGISTERED.values())if(item.get() instanceof MegaStoneItem){
            require(PixelmonItems.megaStones.contains(Backport.item(item.getId().func_110623_a())),"Mega stone missing boss drop pool "+item.getId());
            boolean compatible=false,spawnable=false;
            for(JsonElement patch:Content.DATA.getAsJsonArray("patches")){
                Species s=PixelmonSpecies.fromNationalDex(patch.getAsJsonObject().getAsJsonObject("species").get("dex").getAsInt());
                for(Stats f:s.getForms())if(f.getMegaForm(item.get()).isPresent()){
                    compatible=true;
                    for(SpawnSet set:PixelmonSpawning.megas)for(SpawnInfo raw:set.spawnInfos)if(raw instanceof SpawnInfoPokemon){
                        SpawnInfoPokemon info=(SpawnInfoPokemon)raw;
                        if(info.getSpecies()!=s || !info.getForm().getMegaForm(item.get()).isPresent())continue;
                        require(info.rarity>0 && !info.locationTypes.isEmpty(),"Unusable Mega boss spawn "+set.id);
                        require(info.condition!=null,"Invalid Mega condition "+set.id);
                        List<?> named=(List<?>)biomeStrings.get(info.condition);
                        if(named!=null && !named.isEmpty())require(!info.condition.biomes.isEmpty(),"No legacy biome resolves for Mega boss "+set.id+" "+named);
                        require(info.spawnSpecificBossRate!=null && info.spawnSpecificBossRate==1f,"Not a guaranteed boss "+set.id);
                        Pokemon pokemon=info.getPokemonSpec().create();pokemon.setLevelNum(70);pokemon.initialize();
                        PixelmonEntity entity=new PixelmonEntity(ServerLifecycleHooks.getCurrentServer().func_241755_D_(),pokemon);
                        BossTier guaranteed=new BossTier("qa-mega","QA",false,1,java.awt.Color.WHITE,1,true,1,1,"",1,0);
                        boolean dropped=false;
                        for(int attempt=0;attempt<128 && !dropped;attempt++)for(DropSet.Drop drop:guaranteed.getDrops(FakePlayerFactory.getMinecraft(ServerLifecycleHooks.getCurrentServer().func_241755_D_()),entity))
                            if(drop.getDisplayItem().func_77973_b()==item.get())dropped=true;
                        require(dropped,"Native Mega boss cannot drop "+item.getId()+" from "+info.getForm().getName());
                        spawnable=true;break;
                    }
                }
            }
            require(compatible,"Orphan mega stone "+item.getId());require(spawnable,"No compatible native Mega boss spawn for "+item.getId());++megaItems;
        }
        int acquisitionItems=0;
        for(Map.Entry<String,RegistryObject<Item>> entry:Backport.REGISTERED.entrySet()){
            String name=entry.getKey();Item item=entry.getValue().get();
            if(name.equals("tera_den_spawner"))continue; // Creative placement tool; naturally generated entity is the survival route.
            if(item instanceof MegaStoneItem){++acquisitionItems;continue;}
            if(name.equals("tera_blast_disc_single_use")){require(TeraRaids.rewards(1,0,new Random(1)).stream().anyMatch(stack->stack.func_77973_b()==item),"Single-use disc source");++acquisitionItems;continue;}
            if(name.endsWith("_tera_shard")){++acquisitionItems;continue;}
            if(name.endsWith("_gym_badge")){GymCityVerification.badgeSource(name.substring(0,name.length()-10));++acquisitionItems;continue;}
            if(name.equals("tera_orb") || name.equals("eternatus_meteorite") || name.equals("metal_alloy") || name.endsWith("_nectar")){
                net.minecraft.item.crafting.IRecipe<?> recipe=ServerLifecycleHooks.getCurrentServer().func_199529_aN().func_215367_a(new ResourceLocation(Content.NS,name)).orElse(null);
                require(recipe!=null && recipe.func_77571_b().func_77973_b()==item,"Recipe not loaded "+name);
                for(net.minecraft.item.crafting.Ingredient ingredient:recipe.func_192400_c())require(ingredient.func_193365_a().length>0,"Unresolved recipe ingredient "+name);
                if(!name.equals("tera_orb"))require(DropItemRegistry.tier3.stream().noneMatch(stack->stack.func_77973_b()==item),"Expensive Meteorite still in chest loot");
            }else require(DropItemRegistry.tier3.stream().anyMatch(stack->stack.func_77973_b()==item),"No survival source for added item "+entry.getValue().getId());
            ++acquisitionItems;
        }
        require(acquisitionItems==Backport.REGISTERED.size()-1,"Unclassified acquisition items");
        Map<ResourceLocation,JsonElement> drops=new LinkedHashMap<>();
        try(JarFile jar=new JarFile(System.getProperty("backport.test.oldJar"))){
            try(InputStream in=jar.getInputStream(jar.getJarEntry("data/pixelmon/drops/raiddrops.json"))){drops.put(new ResourceLocation("pixelmon","raiddrops"),new JsonParser().parse(new InputStreamReader(in,StandardCharsets.UTF_8)));}
        }
        JsonObject original=new JsonParser().parse(drops.values().iterator().next().toString()).getAsJsonObject();
        Content.apply("pixelmon","drops",drops);String once=drops.toString();Content.apply("pixelmon","drops",drops);require(once.equals(drops.toString()),"Loot reload duplicates");
        JsonObject merged=drops.values().iterator().next().getAsJsonObject();
        require(merged.equals(original),"Max raid loot changed; Tera drops must be independent");
        for(Map.Entry<String,JsonElement> star:original.entrySet())for(Map.Entry<String,JsonElement> type:star.getValue().getAsJsonObject().entrySet())for(JsonElement row:type.getValue().getAsJsonArray())require(merged.getAsJsonObject(star.getKey()).getAsJsonArray(type.getKey()).contains(row),"Lost legacy raid reward");
        System.out.println("BACKPORT_WORLD_PASS native spawn sets="+sets+" entries="+infos+" raid shard pools="+rewards+" survival acquisition items="+acquisitionItems+" TMs=7 compatible mega boss drops="+megaItems+" renewable X/Y drops, resolved boss biomes, legacy loot preserved, reload idempotent");
    }
}
