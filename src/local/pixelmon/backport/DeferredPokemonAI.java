package local.pixelmon.backport;
import java.util.*;
import net.minecraft.entity.ai.goal.GoalSelector;

/** A field skill can start a battle while its goal is still being iterated. */
public final class DeferredPokemonAI {
    private static final ThreadLocal<Deque<Scope>> ACTIVE=ThreadLocal.withInitial(ArrayDeque::new);
    private static final class Scope {
        final GoalSelector selector;Runnable rebuild;
        Scope(GoalSelector selector){this.selector=selector;}
    }
    private DeferredPokemonAI(){}
    public static boolean defer(GoalSelector selector,Runnable rebuild){
        for(Scope scope:ACTIVE.get())if(scope.selector==selector){scope.rebuild=rebuild;return true;}
        return false;
    }
    public static void tick(GoalSelector selector){
        Scope scope=new Scope(selector);Deque<Scope> stack=ACTIVE.get();stack.push(scope);
        try{selector.func_75774_a();}
        finally{
            stack.pop();if(stack.isEmpty())ACTIVE.remove();
            // Native populateTasks clears the goal set directly. Delay the whole
            // rebuild, not just addGoal, until the selector's streams have closed.
            if(scope.rebuild!=null)scope.rebuild.run();
        }
    }
}
