package local.pixelmon.backport;
/** Passive carryable item: native generic inventory-held storage already accepts it. */
public final class GrassGreenOrb extends com.pixelmonmod.pixelmon.items.QuestItem {
 public GrassGreenOrb(){super();((local.pixelmon.backport.mixin.ItemGroupAccess)(Object)this).backport$group(com.pixelmonmod.pixelmon.items.group.PixelmonItemGroups.TAB_HELD_ITEMS);}
}
