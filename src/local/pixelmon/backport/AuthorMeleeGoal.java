package local.pixelmon.backport;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.util.Hand;

/** Native pathfinding, ordinary-monster reach and cooldown, retaining the line-of-sight guard. */
public final class AuthorMeleeGoal extends MeleeAttackGoal {
    public AuthorMeleeGoal(AuthorCombatBoss boss){super(boss,1,true);}
}
