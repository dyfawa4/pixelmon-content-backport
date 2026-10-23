package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.quests.QuestObjectiveEvent;
import com.pixelmonmod.pixelmon.quests.QuestProgress;
import com.pixelmonmod.pixelmon.quests.objectives.Objective;
import com.pixelmonmod.pixelmon.quests.objectives.objectives.player.ItemObjective;
import com.pixelmonmod.pixelmon.quests.quest.*;
import com.pixelmonmod.pixelmon.storage.playerData.QuestData;
import local.pixelmon.backport.mixin.NpcQuestProgressAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;

/** Adapt native objectives, counters, stages and rewards without replacing saved quest identities. */
public final class NpcQuestLogic {
    private static final Set<String> POSSESSION = Set.of("ITEM_CRAFT", "ITEM_SMELT", "ITEM_PICKUP", "APRICORN_HARVEST");
    private NpcQuestLogic() {}

    public static boolean possession(String id) { return POSSESSION.contains(id); }

    public static List<ItemStack> inventory(PlayerInventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.field_70462_a);
        stacks.addAll(inventory.field_184439_c);
        return stacks;
    }

    public static boolean itemTest(String id, Arguments a, QuestProgress p, Context c) {
        Item item = a.value(0, p);
        int damage = a.value(2, p);
        String name = a.value(3, p);
        CompoundNBT nbt = a.value(4, p);
        if (possession(id)) {
            // An event is only a prompt to recheck ownership. Repeated partial outputs must
            // not accumulate credit, and old saves need no quest or stage replacement.
            return p.getParent().getPlayer() != null && NpcQuestItems.count(
                    inventory(p.getParent().getPlayer().field_71071_by), item, damage, name, nbt)
                    >= Math.max(1, (Integer)a.value(1, p));
        }
        if (id.equals("ITEM_HAS") || id.equals("ITEM_HAS_NOT")) {
            Object value = c.get(0);
            if (!(value instanceof Iterable<?>)) return false;
            @SuppressWarnings("unchecked") Iterable<ItemStack> stacks = (Iterable<ItemStack>)value;
            boolean enough = NpcQuestItems.count(stacks, item, damage, name, nbt) >= Math.max(1, (Integer)a.value(1, p));
            return id.equals("ITEM_HAS") == enough;
        }
        Object value = c.get(0);
        return value instanceof ItemStack && NpcQuestItems.matches((ItemStack)value, item, damage, name, nbt);
    }

    public static boolean npcTest(String id, QuestData data, QuestProgress p, Arguments a, Context c) {
        Object value = c.get(0);
        UUID expected = a.value(1, p);
        if (!(value instanceof Entity) || expected == null || !expected.equals(((Entity)value).func_110124_au())
                || data.getPlayer() == null || ((Entity)value).field_70128_L
                || ((Entity)value).field_70170_p != data.getPlayer().field_70170_p) return false;
        boolean successBranch = a.value(2, p);
        Item item = a.value(3, p);
        int needed = Math.max(1, (Integer)a.value(4, p)), damage = a.value(5, p);
        String name = a.value(6, p);
        CompoundNBT nbt = a.value(7, p);
        ItemStack display = item == null ? ItemStack.field_190927_a : new ItemStack(item);
        if (nbt != null && !nbt.isEmpty() && item != null) display.func_77982_d(nbt.func_74737_b());
        List<ItemStack> stacks = inventory(data.getPlayer().field_71071_by);
        int owned = NpcQuestItems.count(stacks, item, damage, name, nbt);
        boolean enough = owned >= needed;
        if (!successBranch) return !enough; // A failure probe must never remove items.
        if (!enough) {
            data.getPlayer().func_146105_b(new TranslationTextComponent("pixelmon_backport.npc_quest.missing",
                    item == null ? new TranslationTextComponent("pixelmon_backport.npc_quest.item") : display.func_200301_q(), needed, owned), false);
            return false;
        }
        if (!NpcQuestItems.submit(stacks, item, needed, damage, name, nbt, id.equals("NPC_GIVE"))) return false;
        if (id.equals("NPC_GIVE")) {
            data.getPlayer().field_71071_by.func_70296_d();
            data.getPlayer().func_146105_b(new TranslationTextComponent("pixelmon_backport.npc_quest.submitted",
                    item == null ? new TranslationTextComponent("pixelmon_backport.npc_quest.item") : display.func_200301_q(), needed, owned-needed), false);
        }
        return true;
    }

    /** Native Objective.receive still owns condition checks, action ordering, stage changes and packets. */
    public static void complete(QuestProgress p, int index, Objective objective, Object[] context) {
        String id = objective.getInternalObjective().identifier();
        int units = 1;
        if (objective.getInternalObjective() instanceof ItemObjective) {
            if (id.equals("ITEM_HAS") || id.equals("ITEM_HAS_NOT") || possession(id)) units = Math.max(1, p.getObjectiveTotalQuantity(index) - p.getObjectiveQuantityComplete(index));
            else if (id.equals("ITEM_DROP") && context.length > 0 && context[0] instanceof ItemStack)
                units = Math.max(1, ((ItemStack)context[0]).func_190916_E());
        }
        if (units == 1) { p.completeObjective(index); return; }
        int[] remaining = ((NpcQuestProgressAccess)(Object)p).backport$remaining();
        if (index < 0 || index >= remaining.length) return;
        QuestObjectiveEvent.Progress event = new QuestObjectiveEvent.Progress(p.getParent().getPlayer(), p,
                p.getCurrentStage(), objective, index, remaining[index]);
        Pixelmon.EVENT_BUS.post(event);
        remaining[index] = event.completion > 0 ? Math.max(0, event.completion - units) : event.completion;
    }
}
