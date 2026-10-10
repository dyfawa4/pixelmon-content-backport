import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.egg.BreedingLogicProxy;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.*;
import com.pixelmonmod.pixelmon.items.*;
import com.pixelmonmod.pixelmon.items.FishingRodItem;
import com.pixelmonmod.pixelmon.entities.projectiles.HookEntity;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry;
import io.netty.buffer.Unpooled;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.Poison;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
import com.pixelmonmod.pixelmon.battles.status.Paralysis;
import com.pixelmonmod.pixelmon.battles.status.Sleep;
import com.pixelmonmod.pixelmon.battles.status.Burn;
import com.pixelmonmod.pixelmon.battles.status.Freeze;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon;
import com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnActionPokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import local.pixelmon.backport.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.container.WorkbenchContainer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.ForgeRegistries;

/** Real native gameplay APIs in the private Forge world; no UI or production saves. */
public final class IntegrationVerification {
    static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    static Item item(String id){Item i=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));require(i!=null,"Unknown ingredient "+id);return i;}
    public static void run(FakePlayer player)throws Exception{
        PlayerPartyStorage party=StorageProxy.getParty(player);Pokemon previous=party.get(0),previousDitto=party.get(1);ItemStack hand=player.func_184614_ca();
        int breeds=0,unbreedable=0,casts=0,medicines=0,mints=0,candies=0;
        try{
            require(BreedingLogicProxy.hasImplementation(),"Native breeding implementation absent");
            Pokemon ditto=TeraVerification.mon(132);party.set(1,ditto);
            for(JsonElement patch:Content.DATA.getAsJsonArray("patches")){
                int dex=patch.getAsJsonObject().getAsJsonObject("species").get("dex").getAsInt();
                for(Stats form:PixelmonSpecies.fromNationalDex(dex).getForms()){
                    if(form.isTemporary())continue;
                    Pokemon p=TeraVerification.mon(dex);p.setForm(form);p.heal();party.set(0,p);
                    boolean can=BreedingLogicProxy.canBreed(p,ditto);Optional<Pokemon> egg=BreedingLogicProxy.makeEgg(p,ditto);
                    require(can==egg.isPresent(),"Breeding/egg disagreement "+dex+"/"+form.getName());
                    if(can){Pokemon child=egg.get();require(child.isEgg() && child.getSpecies()==form.getBaseEvolution(),"Wrong offspring "+dex+"/"+form.getName());
                        require(child.getAbility()!=null && child.getForm()!=null,"Invalid egg data "+dex);
                        Pokemon saved=PokemonFactory.create(child.writeToNBT(new CompoundNBT()));require(saved.isEgg() && saved.getSpecies()==child.getSpecies(),"Egg save "+dex);++breeds;
                    }else ++unbreedable;
                    if(form.hasTag("legendary") || form.hasTag("mythical"))require(!can,"Legendary can breed "+dex);
                    party.set(0,p);
                    for(Item raw:ForgeRegistries.ITEMS)if(raw instanceof FishingRodItem){
                        ItemStack rod=new ItemStack(raw);player.func_184611_a(Hand.MAIN_HAND,rod);player.field_71104_cf=null;
                        raw.func_77659_a(player.field_70170_p,player,Hand.MAIN_HAND);
                        require(player.field_71104_cf instanceof HookEntity,"New Pokemon cannot fish "+dex+" / "+raw.getRegistryName());
                        player.field_71104_cf.func_70106_y();player.field_71104_cf=null;++casts;
                    }
                }
            }
            require(breeds>20 && unbreedable>10,"Insufficient native breeding coverage");
            for(SpawnSet set:PixelmonSpawning.fishing)for(SpawnInfo raw:set.spawnInfos)if(raw instanceof SpawnInfoPokemon){
                SpawnInfoPokemon info=(SpawnInfoPokemon)raw;Pokemon fish=info.getPokemonSpec().create();fish.initialize();require(fish.getForm()!=null && fish.getAbility()!=null && fish.canBattle(),"Unusable native fishing Pokemon "+set.id);
            }
            Pokemon mother=TeraVerification.mon(1018);party.set(0,mother);mother.setNature(Nature.ADAMANT);mother.setMintNature(Nature.MODEST);mother.setHeldItem(new ItemStack(item("pixelmon:ever_stone")));
            mother.getIVs().fillFromArray(new int[]{31,31,31,31,31,31});ditto.getIVs().fillFromArray(new int[]{31,31,31,31,31,31});ditto.setHeldItem(new ItemStack(item("pixelmon:destiny_knot")));
            for(int n=0;n<20;n++){Pokemon egg=BreedingLogicProxy.makeEgg(mother,ditto).orElseThrow(()->new AssertionError("Archaludon breeding"));
                require(egg.getBaseNature()==Nature.ADAMANT && egg.getMintNature()==null,"Mint incorrectly inherited instead of base nature");
                int perfect=0;for(BattleStatsType stat:BattleStatsType.EV_IV_STATS)if(egg.getIVs().getStat(stat)==31)perfect++;require(perfect>=5,"Destiny knot inheritance failed");
            }
            for(Item raw:ForgeRegistries.ITEMS){
                if(!(raw instanceof MedicineItem || raw instanceof MintItem || raw instanceof ExpCandyItem))continue;
                Pokemon p=TeraVerification.mon(1017);party.set(0,p);PixelmonEntity entity=p.getOrCreatePixelmon(player.field_70170_p,0,5,0);
                ItemStack stack=new ItemStack(raw,2);player.func_184611_a(Hand.MAIN_HAND,stack);String label=raw.getRegistryName().toString();
                if(raw instanceof MedicineItem){p.setHealth(raw instanceof ReviveItem?0:1);p.setStatus(new Poison());
                    if(label.endsWith("paralyze_heal"))p.setStatus(new Paralysis());
                    else if(label.endsWith("awakening"))p.setStatus(new Sleep());
                    else if(label.endsWith("burn_heal"))p.setStatus(new Burn());
                    else if(label.endsWith("ice_heal"))p.setStatus(new Freeze());
                    require(new InteractionPotion().processInteract(entity,player,Hand.MAIN_HAND,stack),"Medicine unhandled "+label);
                    require(stack.func_190916_E()==1,"Medicine no consumption "+label);
                    require(p.getHealth()>1 || p.getStatus()==null || p.getStatus().type==StatusType.None,"Medicine no effect "+label);p.heal();
                    new InteractionPotion().processInteract(entity,player,Hand.MAIN_HAND,stack);require(stack.func_190916_E()==1,"Healthy Pokemon consumes medicine "+label);++medicines;
                }else if(raw instanceof MintItem){Nature base=((MintItem)raw).getNature()==Nature.HARDY?Nature.ADAMANT:Nature.HARDY;p.setNature(base);
                    new InteractionMint().processInteract(entity,player,Hand.MAIN_HAND,stack);require(p.getNature()==((MintItem)raw).getNature() && p.getBaseNature()==base && stack.func_190916_E()==1,"Mint effect/consumption "+label);
                    Pokemon saved=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));require(saved.getNature()==p.getNature() && saved.getBaseNature()==base,"Mint persistence "+label);
                    new InteractionMint().processInteract(entity,player,Hand.MAIN_HAND,stack);require(stack.func_190916_E()==1,"Repeated mint consumed "+label);++mints;
                }else{p.setLevelNum(50);int exp=p.getExperience();new InteractionRareCandy().processInteract(entity,player,Hand.MAIN_HAND,stack);
                    require((p.getPokemonLevel()>50 || p.getExperience()>exp) && stack.func_190916_E()==1,"Candy effect/consumption "+label);
                    p.setLevelNum(100);new InteractionRareCandy().processInteract(entity,player,Hand.MAIN_HAND,stack);require(stack.func_190916_E()==1,"Max-level candy consumed "+label);++candies;
                }
            }
            fishingReel(player);meteorite(player);translation();
            System.out.println("BACKPORT_INTEGRATION_PASS native breeding="+breeds+" nonbreedable="+unbreedable+" egg saves/everstone/base nature/destiny knot; fishing casts="+casts+" native pool validation/reel/new Pokemon battle; medicines="+medicines+" mints="+mints+" candies="+candies+" effects/consumption/no-effect rejection; crafted Eternamax/battle stats/switch/end/removal; localized native TMs");
        }finally{party.set(0,previous);party.set(1,previousDitto);player.func_184611_a(Hand.MAIN_HAND,hand);if(player.field_71104_cf!=null)player.field_71104_cf.func_70106_y();player.field_71104_cf=null;}
    }
    static void fishingReel(FakePlayer player)throws Exception{
        Pokemon team=TeraVerification.mon(983);StorageProxy.getParty(player).set(0,team);
        FishingRodItem rod=null;for(Item raw:ForgeRegistries.ITEMS)if(raw instanceof FishingRodItem){rod=(FishingRodItem)raw;break;}require(rod!=null,"No native fishing rod");
        ItemStack stack=new ItemStack(rod);player.func_184611_a(Hand.MAIN_HAND,stack);player.func_70634_a(0,5,0);
        // FakePlayer is not registered in the world's entity UUID index. Real
        // players are; provide only that owner lookup while retaining native reel logic.
        HookEntity hook=new HookEntity(player.field_70170_p,player,rod.getRodType(),-1){@Override public net.minecraft.entity.Entity func_234616_v_(){return player;}};
        player.func_71121_q().func_180501_a(new net.minecraft.util.math.BlockPos(2,5,2),ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:water")).func_176223_P(),3);hook.func_226286_f_(2.5,5,2.5);
        SpawnInfoPokemon info=null;for(SpawnSet set:PixelmonSpawning.fishing)for(SpawnInfo raw:set.spawnInfos)if(raw instanceof SpawnInfoPokemon){info=(SpawnInfoPokemon)raw;break;}
        // Inject a caught spawn to verify the native reel path for an added
        // species deterministically. This does not add it to the fishing pool.
        require(info!=null,"Native fishing pool empty");Pokemon fish=TeraVerification.mon(1018);
        SpawnActionPokemon caught=new SpawnActionPokemon(info,hook.createSpawnLocation(),fish,null);java.lang.reflect.Field field=HookEntity.class.getDeclaredField("caught");field.setAccessible(true);field.set(hook,caught);
        try{rod.func_77659_a(player.field_70170_p,player,Hand.MAIN_HAND);BattleController battle=BattleRegistry.getBattle(player);require(battle!=null,"Native reel cannot battle a new Pokemon");require(battle.participants.stream().anyMatch(p->Arrays.stream(p.allPokemon).anyMatch(w->w.pokemon.getSpecies().getDex()==1018)),"Reel battle lost new Pokemon species");battle.endBattleWithoutXP();}
        finally{BattleController b=BattleRegistry.getBattle(player);if(b!=null)b.endBattleWithoutXP();caught.getOrCreateEntity().func_70106_y();hook.func_70106_y();player.field_71104_cf=null;}
    }
    static void meteorite(FakePlayer player){
        WorkbenchContainer container=new WorkbenchContainer(19,player.field_71071_by);
        CraftingInventory grid=(CraftingInventory)container.func_75139_a(1).field_75224_c;
        String[] ids={"minecraft:end_stone_bricks","pixelmon:azure_flute","minecraft:end_stone_bricks","minecraft:end_stone_bricks","pixelmon:meteorite","minecraft:end_stone_bricks","minecraft:end_stone_bricks","pixelmon:master_ball","minecraft:end_stone_bricks"};
        for(int i=0;i<9;i++)grid.func_70299_a(i,new ItemStack(item(ids[i])));
        IRecipe<?> raw=player.func_71121_q().func_199532_z().func_215367_a(new ResourceLocation(Content.NS,"eternatus_meteorite")).orElseThrow(()->new AssertionError("Meteorite recipe not loaded"));
        require(raw instanceof ShapedRecipe,"Meteorite recipe not shaped");ShapedRecipe recipe=(ShapedRecipe)raw;
        require(recipe.func_77569_a(grid,player.field_70170_p),"Exact Meteorite crafting grid rejected");
        ItemStack master=PokeBallRegistry.MASTER_BALL.getValue().orElseThrow().getBallItem();
        require(master.func_77973_b().getRegistryName().toString().equals("pixelmon:poke_ball"),"Native master ball fixture still uses obsolete item");
        grid.func_70299_a(7,master);require(recipe.func_77569_a(grid,player.field_70170_p),"Real native Master Ball rejected");
        ItemStack renamed=master.func_77946_l();renamed.func_200302_a(new StringTextComponent("测试大师球"));grid.func_70299_a(7,renamed);require(recipe.func_77569_a(grid,player.field_70170_p),"Renamed Master Ball rejected");
        net.minecraft.network.PacketBuffer buffer=new net.minecraft.network.PacketBuffer(Unpooled.buffer());
        Ingredient ball=recipe.func_192400_c().get(7);ball.func_199564_a(buffer);Ingredient synced=Ingredient.func_199566_b(buffer);
        require(synced.test(master) && synced.test(renamed),"Client ingredient sync lost master ball NBT matching");buffer.release();
        for(String other:new String[]{"poke_ball","ultra_ball","cherish_ball"}){ItemStack wrong=PokeBallRegistry.getPokeBall(other).getValue().orElseThrow().getBallItem();grid.func_70299_a(7,wrong);require(!recipe.func_77569_a(grid,player.field_70170_p),"Wrong ball accepted "+other);require(!synced.test(wrong),"Client accepts wrong ball "+other);}
        grid.func_70299_a(7,new ItemStack(item("pixelmon:poke_ball")));require(!recipe.func_77569_a(grid,player.field_70170_p),"Untagged Poke Ball accepted");
        grid.func_70299_a(7,master);
        ItemStack crafted=recipe.func_77572_b(grid);require(crafted.func_190916_E()==1 && crafted.func_77973_b() instanceof ReferenceItems.Meteorite,"Wrong Meteorite result");
        grid.func_70299_a(4,ItemStack.field_190927_a);require(!recipe.func_77569_a(grid,player.field_70170_p),"Missing Meteorite ingredient accepted");grid.func_70299_a(4,new ItemStack(item(ids[4])));
        container.func_75139_a(0).func_190901_a(player,crafted);for(int i=0;i<9;i++)require(grid.func_70301_a(i).func_190926_b(),"Crafting ingredient not consumed "+i);
        Pokemon p=TeraVerification.mon(890);StorageProxy.getParty(player).set(0,p);p.setLevelNum(100);p.setNature(Nature.HARDY);p.getIVs().fillFromArray(new int[6]);p.getEVs().fillFromArray(new int[6]);
        p.setHeldItem(crafted);p.heal();require(p.getForm().isForm("eternamax"),"Crafted Meteorite no transformation");
        int[] expected={620,235,505,255,505,265};int at=0;for(BattleStatsType stat:BattleStatsType.EV_IV_STATS)require(p.getStat(stat)==expected[at++],"Eternamax actual stat "+stat+"="+p.getStat(stat));
        BattleController battle=TeraVerification.battle(p);try{PixelmonWrapper w=battle.participants.get(0).allPokemon[0];require(w.getForm().isForm("eternamax") && w.getMaxHealth()==620,"Eternamax native battle");w.resetOnSwitch();require(w.getForm().isForm("eternamax"),"Eternamax lost on switch");battle.endBattleWithoutXP();require(p.getForm().isForm("eternamax"),"Eternamax lost after battle");}finally{Tera.end(battle);}
        p.setHealth(0);p.setHeldItem(ItemStack.field_190927_a);require(p.getForm().isForm("ordinary") && p.getHealth()==0,"Removal revives fainted Pokemon");p.setHeldItem(crafted);require(p.getHealth()==0,"Equip revives fainted Pokemon");
        p.heal();p.setHealth(1);p.setHeldItem(ItemStack.field_190927_a);p.setHeldItem(crafted);require(p.getHealth()<=2,"Meteorite toggle heals Pokemon");
    }
    static void translation()throws Exception{
        Map<String,String> zh=new HashMap<>();try(InputStream in=Content.class.getClassLoader().getResourceAsStream("assets/pixelmon_backport/lang/zh_cn.json")){LanguageMap.func_240593_a_(in,zh::put);}
        LanguageMap previous=LanguageMap.func_74808_a();java.lang.reflect.Field singleton=null;
        for(java.lang.reflect.Field field:LanguageMap.class.getDeclaredFields())if(field.getType()==LanguageMap.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())){singleton=field;singleton.setAccessible(true);break;}
        require(singleton!=null,"Native language singleton absent");singleton.set(null,new LanguageMap(){
            public String func_230503_a_(String key){return zh.getOrDefault(key,previous.func_230503_a_(key));}public boolean func_230506_b_(String key){return zh.containsKey(key)||previous.func_230506_b_(key);}public boolean func_230505_b_(){return false;}
            public net.minecraft.util.IReorderingProcessor func_241870_a(ITextProperties text){return previous.func_241870_a(text);}
        });
        try{List<ITechnicalMove> moves=new ArrayList<>(Arrays.asList(ITechnicalMove.getAllFor(TMType.TM9)));moves.addAll(Arrays.asList(BackportTechnicalMoves.values()));int count=0;for(ITechnicalMove move:moves)if(move.getId()>=171){ItemStack stack=TechnicalMoveItem.of(TMType.TM9,move);String key=stack.func_77973_b().func_77667_c(stack);require(zh.containsKey(key),"Missing Paldean TM translation "+move.getId());require(stack.func_200301_q().getString().equals(zh.get(key)),"Actual native TM name not localized "+move.getId());++count;}require(count==59,"Paldean translation coverage "+count);}
        finally{singleton.set(null,previous);}
    }
}
