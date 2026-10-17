package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.exceptions.InvalidQuestArgsException;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.objectives.objectives.player.ItemObjective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;

/** Periodic state checks are separate from occurrence-based capture/battle/harvest goals. */
public final class NpcQuestEvents {
    private static final Set<String> STOCK_PREPARATION = Set.of("craft_poke_ball.json", "craft_great_ball.json", "craft_ultra_ball.json",
            "craft_healer.json", "craft_pc.json", "find_master_ball.json", "celebi.json");
    private NpcQuestEvents() {}

    public static void poll(TickEvent.PlayerTickEvent event) throws InvalidQuestArgsException {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayerEntity)
                || event.player instanceof FakePlayer || event.player.field_70170_p == null
                || event.player.field_70170_p.func_82737_E() % 20L != 0L) return;
        ServerPlayerEntity player = (ServerPlayerEntity)event.player;
        QuestData data = StorageProxy.getParty(player).getQuestData(false);
        List<ItemStack> inventory = NpcQuestLogic.inventory(player.field_71071_by);
        for (QuestProgress p : new ArrayList<>(data.getProgress())) {
            if (p.isComplete() || p.isFailed()) continue;
            Stage stage = p.getCurrentStage();
            if (stage == null || stage.getParsedObjectives() == null) continue;
            String filename = p.getQuestFilename().replace('\\','/');
            filename = filename.substring(filename.lastIndexOf('/') + 1);
            for (int index = 0; index < stage.getParsedObjectives().size(); index++) {
                if (p.getCurrentStage() != stage) break;
                Objective o = stage.getParsedObjectives().get(index);
                if (!(o.getInternalObjective() instanceof ItemObjective) || p.isObjectiveComplete(index)) continue;
                String id = o.getInternalObjective().identifier();
                Arguments a = o.getData();
                if (!a.isAllSet(p)) continue;
                if (id.equals("ITEM_HAS") || id.equals("ITEM_HAS_NOT")) {
                    o.receive(id, stage, data, p, index, inventory);
                } else if (STOCK_PREPARATION.contains(filename) && (id.equals("ITEM_CRAFT") || id.equals("ITEM_PICKUP"))) {
                    Item item = a.value(0,p); int damage = a.value(2,p);
                    String name = a.value(3,p); CompoundNBT nbt = a.value(4,p);
                    int total = NpcQuestItems.count(inventory,item,damage,name,nbt);
                    if (total < Math.max(1,(Integer)a.value(1,p))) continue;
                    for (ItemStack stack : inventory) if (NpcQuestItems.matches(stack,item,damage,name,nbt)) {
                        ItemStack proof = stack.func_77946_l(); proof.func_190920_e(total);
                        o.receive(id,stage,data,p,index,proof); break;
                    }
                }
            }
        }
        // Already being in the required dimension is a state, not a forced leave-and-return trip.
        data.receive("DIMENSION");
    }
}
