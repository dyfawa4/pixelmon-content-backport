import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.entities.npcs.NPCRelearner;
import local.pixelmon.backport.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.item.ItemStack;
import io.netty.buffer.Unpooled;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import java.util.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Test-only mod; never bundled in the extension or copied into the user's pack. */
@Mod("backport_live_test")
public class LiveVerification {
    public LiveVerification(){MinecraftForge.EVENT_BUS.register(this);}
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void same(Pokemon original,Pokemon copy,String label){
        require(original.getSpecies().getDex()==copy.getSpecies().getDex(),label+" dex");
        require(original.getForm().getName().equals(copy.getForm().getName()),label+" form");
        require(original.getAbilityName().equals(copy.getAbilityName()),label+" ability");
        // The legacy client packet carries the ability name, not its server
        // slot. Persistence/trade data must preserve both; client data only
        // needs the resolved native ability.
        if(!label.startsWith("network"))require(original.getAbilitySlot()==copy.getAbilitySlot(),label+" ability slot");
        require(original.getMoveset().size()==copy.getMoveset().size(),label+" move count");
        for(int i=0;i<original.getMoveset().size();i++)
            require(original.getMoveset().get(i).getActualMove().getAttackName().equals(copy.getMoveset().get(i).getActualMove().getAttackName()),label+" move "+i);
        require(original.getHeldItem().func_77973_b()==copy.getHeldItem().func_77973_b(),label+" held item");
        require(original.getPalette().equals(copy.getPalette()),label+" palette");
    }
    @SubscribeEvent public void started(FMLServerStartedEvent event){
        try {
            NativeDataVerification.main(new String[]{System.getProperty("backport.test.oldJar")});
            WorldContentVerification.run();
            GymCityVerification.run(event.getServer());
            TeraRaidVerification.run();
            int moveDefinitions=0,abilityDefinitions=0;
            for(JsonElement row:Content.DATA.getAsJsonArray("moves")){
                String name=row.getAsJsonObject().get("attackName").getAsString();ImmutableAttack move=AttackRegistry.getAttackBaseFromEnglishName(name).orElse(null);
                require(move!=null && move.effects!=null && !move.effects.contains(null),"Unresolved added move effects "+name);
                require(AttackRegistry.fromAttackIndex(move.getAttackIndex()).map(a->a.getAttackName().equals(name)).orElse(false),"Move network index collision "+name);
                ++moveDefinitions;
            }
            int forms=0,palettes=0,reminders=0;
            for(JsonElement patch:Content.DATA.getAsJsonArray("patches")) {
                JsonObject json=patch.getAsJsonObject().getAsJsonObject("species");
                int dex=json.get("dex").getAsInt();
                Species species=PixelmonSpecies.fromNationalDex(dex);
                require(species!=null,"Missing registered species "+dex);
                for(JsonElement formJson:json.getAsJsonArray("forms")) {
                    String name=formJson.getAsJsonObject().get("name").getAsString();
                    require(species.hasForm(name),"Missing form "+dex+" "+name);
                    Stats form=species.getForm(name);
                    for(Ability ability:form.getAbilities().getAll()){
                        require(ability!=null && !ability.getClass().getSimpleName().equals("ComingSoon"),"Unimplemented ability slot "+dex+" "+name);
                        ++abilityDefinitions;
                    }
                    Pokemon pokemon=PokemonFactory.create(species);
                    pokemon.setForm(name);pokemon.setLevelNum(100);pokemon.initialize();pokemon.rerollMoveset();pokemon.resetAbility();
                    require(pokemon.getAbility()!=null,"Missing ability "+dex+" "+name);
                    require(!pokemon.getAbility().getClass().getSimpleName().equals("ComingSoon"),"Unimplemented ability "+dex+" "+name);
                    require(!form.getMoves().getAllMoves().contains(null),"Unresolved move "+dex+" "+name);
                    require(!form.getMoves().getTMMoves9().contains(null),"Unresolved TM9 definition "+dex+" "+name);
                    List<ImmutableAttack> relearn=NPCRelearner.getRelearnableMoves(pokemon);
                    require(!relearn.contains(null),"Null reminder "+dex+" "+name);
                    List<Attack> known=new ArrayList<>(pokemon.getMoveset());
                    pokemon.getMoveset().clear();
                    List<ImmutableAttack> available=NPCRelearner.getRelearnableMoves(pokemon);
                    require(available.containsAll(form.getMoves().getMovesUpToLevel(pokemon.getPokemonLevel())),"Missing reminder entry "+dex+" "+name);
                    pokemon.getMoveset().addAll(known);
                    reminders+=relearn.size();
                    for(String palette:new String[]{"none","shiny"}) {
                        pokemon.setPalette(palette);
                        same(pokemon,PokemonFactory.create(pokemon.writeToNBT(new CompoundNBT())),"NBT "+dex+" "+name);
                        PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());
                        try {pokemon.writeToByteBuffer(buffer);same(pokemon,PokemonFactory.create(buffer),"network "+dex+" "+name);}
                        finally {buffer.release();}
                        ++palettes;
                    }
                    ++forms;
                }
            }
            Pokemon ogerpon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(1017));
            for(String mask:new String[]{"wellspring","hearthflame","cornerstone","teal"}) {
                ogerpon.setHeldItem(new ItemStack(Backport.REGISTERED.get(mask+"_mask").get()));
                require(ogerpon.getForm().isForm(mask),"Mask equip "+mask);
                same(ogerpon,PokemonFactory.create(ogerpon.writeToNBT(new CompoundNBT())),"Mask NBT "+mask);
            }
            ogerpon.setHeldItem(ItemStack.field_190927_a);require(ogerpon.getForm().isForm("teal"),"Mask remove");
            ItemInteractionVerification.run(event.getServer());
            TeraVerification.run();
            TestMapVerification.run(event.getServer());
            GymMapVerification.run(event.getServer());
            AshVerification.run();
            System.out.println("BACKPORT_LIVE_PASS forms="+forms+" NBT/network palette cases="+palettes+" reminder entries="+reminders+" masks=5 added move definitions="+moveDefinitions+" all ability slots="+abilityDefinitions);
        } catch(Throwable failure){System.out.println("BACKPORT_LIVE_FAIL");failure.printStackTrace();}
    }
}
