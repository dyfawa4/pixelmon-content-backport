package local.pixelmon.backport;

import java.util.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.ResourceLocation;

/** Shared NPC quest item semantics. Ball/species/material identity remains explicit. */
public final class NpcQuestItems {
    private static final Set<String> COLORS = Set.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    private NpcQuestItems() {}

    public static boolean sameFunction(Item actual, Item expected) {
        if (actual == expected) return true;
        if (actual == null || expected == null) return false;
        ResourceLocation a = actual.getRegistryName(), b = expected.getRegistryName();
        return a != null && b != null && a.func_110624_b().equals("pixelmon")
                && b.func_110624_b().equals("pixelmon") && family(a.func_110623_a()) != null
                && family(a.func_110623_a()).equals(family(b.func_110623_a()));
    }

    private static String family(String path) {
        for (String suffix : new String[]{"_healer", "_pc"})
            if (path.endsWith(suffix) && COLORS.contains(path.substring(0, path.length() - suffix.length()))) return suffix;
        return null;
    }

    public static boolean matches(ItemStack stack, Item item, int damage, String name, CompoundNBT required) {
        if (stack == null || stack.func_190926_b() || (item != null && !sameFunction(stack.func_77973_b(), item))) return false;
        if (damage >= 0 && stack.func_77952_i() != damage) return false;
        if (name != null && !name.isEmpty() && !stack.func_200301_q().getString().equalsIgnoreCase(name)) return false;
        return required == null || required.isEmpty() || subset(required, stack.func_77978_p());
    }

    /** Required keys are a subset; extra cosmetic/runtime tags do not invalidate a quest item. */
    private static boolean subset(INBT expected, INBT actual) {
        if (expected instanceof CompoundNBT) {
            if (!(actual instanceof CompoundNBT)) return false;
            CompoundNBT e = (CompoundNBT) expected, a = (CompoundNBT) actual;
            for (String key : e.func_150296_c()) if (!subset(e.func_74781_a(key), a.func_74781_a(key))) return false;
            return true;
        }
        return expected != null && expected.equals(actual);
    }

    public static int count(Iterable<ItemStack> stacks, Item item, int damage, String name, CompoundNBT nbt) {
        long total = 0;
        for (ItemStack stack : stacks) if (matches(stack, item, damage, name, nbt))
            total = Math.min(Integer.MAX_VALUE, total + stack.func_190916_E());
        return (int) total;
    }

    /** Check the whole requirement before changing any slot. The caller owns the server tick. */
    public static boolean submit(List<ItemStack> stacks, Item item, int quantity, int damage, String name, CompoundNBT nbt, boolean consume) {
        int needed = Math.max(1, quantity);
        if (count(stacks, item, damage, name, nbt) < needed) return false;
        if (consume) for (ItemStack stack : stacks) {
            if (!matches(stack, item, damage, name, nbt)) continue;
            int take = Math.min(needed, stack.func_190916_E());
            stack.func_190918_g(take);
            needed -= take;
            if (needed == 0) break;
        }
        return true;
    }
}
