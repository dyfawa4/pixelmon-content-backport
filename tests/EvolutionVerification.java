import java.util.*;
import java.lang.reflect.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionEvolution;
import local.pixelmon.backport.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.server.ServerWorld;

/** Actual item interaction and native evolution finalization, no custom evolve implementation. */
public final class EvolutionVerification {
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void run(FakePlayer player)throws Exception{
        PlayerPartyStorage party=StorageProxy.getParty(player);Pokemon previous=party.get(0);
        try{
            Object[][] cases={{840,"","syrupy_apple",1011,""},{884,"","metal_alloy",1018,""},{1012,"counterfeit","unremarkable_teacup",1013,"unremarkable"},{1012,"artisan","masterpiece_teacup",1013,"masterpiece"},{999,"chest","gimmighoul_coin",1000,""},{999,"roaming","gimmighoul_coin",1000,""}};
            for(Object[] row:cases){
                Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex((Integer)row[0]));p.setForm((String)row[1]);p.setLevelNum(70);p.initialize();party.set(0,p);
                PixelmonEntity entity=p.getOrSpawnPixelmon(player);
                ItemStack item=new ItemStack(Backport.REGISTERED.get((String)row[2]).get(),2);player.func_184611_a(Hand.MAIN_HAND,item);
                require(new InteractionEvolution().processInteract(entity,player,Hand.MAIN_HAND,item),"Item evolution rejected "+Arrays.toString(row));
                require(item.func_190916_E()==1,"Evolution item consumption "+row[2]);
                EvolutionQuery query=null;for(EvolutionQuery q:EvolutionQueryList.queryList)if(q.pokemonUUID.equals(p.getUUID()))query=q;
                require(query!=null,"Evolution query missing "+row[2]);
                EvolutionQueryList.spawnPokemon(player,p.getUUID());EvolutionQueryList.acceptQuery(player,p.getUUID());
                for(int i=0;i<300 && p.getSpecies().getDex()==(Integer)row[0];i++)EvolutionQueryList.tick(player.field_70170_p);
                require(p.getSpecies().getDex()==(Integer)row[3] && p.getForm().getName().equals(row[4]),"Wrong evolution destination "+Arrays.toString(row));query.abort();EvolutionQueryList.queryList.remove(query);
            }
            System.out.println("BACKPORT_EVOLUTION_PASS 6 actual item interactions, consumption and native finalization");
        }finally{party.set(0,previous);}
    }
}
