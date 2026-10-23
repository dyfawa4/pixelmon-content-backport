package local.pixelmon.backport;
import java.util.UUID;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;

/** Read-only legacy entity ID: safely retire old saves without retaining the removed PVP mode. */
@Deprecated
public final class AuthorCombatBoss extends CreatureEntity {
    public UUID challenger;
    public AuthorCombatBoss(EntityType<? extends AuthorCombatBoss> type,World world){super(type,world);}
    public static AttributeModifierMap.MutableAttribute attributes(){return PlayerEntity.func_234570_el_();}
    public void reset(){challenger=null;AuthorBossArenas.open(this);func_70106_y();}
    @Override public void func_70071_h_(){if(!field_70170_p.field_72995_K)reset();else super.func_70071_h_();}
    @Override public IPacket<?> func_213297_N(){return net.minecraftforge.fml.network.NetworkHooks.getEntitySpawningPacket(this);}
}
