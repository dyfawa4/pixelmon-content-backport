package local.pixelmon.backport;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.items.PokeBallPart;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.item.ItemStack;

/** Yellow outline is an eligibility cue; it never overrides native immunity. */
public final class BurstClient {
    private static final java.util.Map<String,java.awt.image.BufferedImage> SHAPES=new java.util.HashMap<>();
    private static final java.util.Map<String,java.util.List<int[]>> EDGES=new java.util.HashMap<>();
    private BurstClient(){}
    public static boolean terrain(int battle,boolean ended,com.pixelmonmod.pixelmon.battles.status.StatusType nativeTerrain){
        var state=TeraClient.snapshot;
        return !ended && nativeTerrain==com.pixelmonmod.pixelmon.battles.status.StatusType.ElectricTerrain
            && state!=null && state.battle==battle && state.burst.values().stream().anyMatch(value->(value&16)!=0);
    }
    private static int flags(){
        var bm=ClientProxy.battleManager;var state=TeraClient.snapshot;
        if(bm==null || state==null || bm.battleControllerIndex!=state.battle || bm.getCurrentPokemon()==null)return 0;
        return state.burst.getOrDefault(bm.getCurrentPokemon().pokemonUUID,0);
    }
    public static boolean move(Attack attack){
        if(attack==null)return false;
        int displayFlags=flags();
        if((displayFlags&BurstDisplayPolicy.FIELD)==0)return false;
        var manager=ClientProxy.battleManager;if(manager==null || manager.getCurrentPokemon()==null)return false;
        var user=manager.getCurrentPokemon();
        // The button retains its base attack even while displaying a Z/Max move.
        // Read the same native conversion as setupAttack, without editing it.
        if(manager.showZMoves){
            var z=attack.getMove().getZMove(user,false);
            if(z!=null && attack.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS && Attack.hasAttack(z.attackName)){
                // Generic Z definitions omit category and inherit it from the
                // original move. This detached preview must retain that link.
                Attack preview=new Attack(z.attackName);preview.originalMove=attack;attack=preview;
            }
        }
        else if(manager.dynamaxing || user.pokemonUUID.equals(manager.dynamax)){
            attack=user.gmaxFactor?com.pixelmonmod.pixelmon.battles.attacks.MaxMoveConverter.getGMaxMoveFromAttack(attack.copy(),null,user.species,user.getBaseStats())
                :com.pixelmonmod.pixelmon.battles.attacks.MaxMoveConverter.getMaxMoveFromAttack(attack.copy(),null);
        }
        Element type=BattleInfoCompat.moveType(attack,user);
        var state=TeraClient.snapshot;
        boolean electric=type==Element.ELECTRIC;
        if(!manager.showZMoves && !manager.dynamaxing && !user.pokemonUUID.equals(manager.dynamax) && state!=null && state.battle==manager.battleControllerIndex && state.burstElectricMoves.containsKey(user.pokemonUUID))electric=state.burstElectricMoves.get(user.pokemonUUID).contains(attack.getMove().getAttackName());
        if(attack.getMove().getAttackName().equals("Revelation Dance") && !BattleInfoCompat.defense(user).isEmpty())type=BattleInfoCompat.defense(user).get(0);
        boolean affected=BurstDisplayPolicy.move(displayFlags,attack.getActualMove().getAttackName(),
            electric,
            attack.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS);
        // Ordinary Z selection has no yellow cue. The Cap signature still
        // requires actual field benefit; non-Z allies use the same policy above.
        return affected && (!manager.showZMoves || user.species!=null && user.species.getDex()==CapSpecies.DEX
            && "Origin of Thunder".equals(attack.getActualMove().getAttackName()));
    }
    public static boolean ball(ItemStack stack){
        if((flags()&BurstDisplayPolicy.CAPTURE)==0 || stack.func_190926_b())return false;
        var ball=PokeBallPart.getPokeBall(stack);if(!ball.isPresent())return false;
        var bm=ClientProxy.battleManager;
        return TeraClient.snapshot.burstBalls.getOrDefault(bm.getCurrentPokemon().pokemonUUID,java.util.Collections.emptyList()).contains(ball.get().getName());
    }
    public static void outline(MatrixStack matrix,Widget widget){
        if(!widget.field_230694_p_)return;
        int x=widget.field_230690_l_,y=widget.field_230691_m_,w=widget.func_230998_h_(),h=widget.func_238483_d_();
        nativeOutline(matrix,"menu_list_item",x,y,w,h);
    }
    public static void moveOutline(MatrixStack matrix,Widget widget,int index){
        if(!widget.field_230694_p_)return;
        nativeOutline(matrix,index%2==0?"moveselection_left":"moveselection_right",widget.field_230690_l_,widget.field_230691_m_,widget.func_230998_h_(),widget.func_238483_d_());
    }
    private static void nativeOutline(MatrixStack matrix,String shape,int x,int y,int w,int h){
        if(w<2 || h<2)return;
        if(!SHAPES.containsKey(shape)){
            try(var resource=net.minecraft.client.Minecraft.func_71410_x().func_195551_G().func_199002_a(new net.minecraft.util.ResourceLocation("pixelmon","textures/gui/battle/"+shape+".png"))){
                SHAPES.put(shape,javax.imageio.ImageIO.read(resource.func_199027_b()));
            }catch(java.io.IOException error){SHAPES.put(shape,null);}
        }
        String key=shape+":"+w+":"+h;
        if(EDGES.size()>64)EDGES.clear();
        var spans=EDGES.computeIfAbsent(key,ignored->BurstOutline.spans(SHAPES.get(shape),w,h));
        for(int[] span:spans)AbstractGui.func_238467_a_(matrix,x+span[0],y+span[1],x+span[2],y+span[1]+1,0xffffd42a);
    }
    public static void outline(MatrixStack matrix,int x,int y,int w,int h){
        edge(matrix,x-1,y-1,w+2,h+2,0x66ffdc32);edge(matrix,x,y,w,h,0xffffd42a);
    }
    private static void edge(MatrixStack m,int x,int y,int w,int h,int colour){
        AbstractGui.func_238467_a_(m,x+3,y,x+w-3,y+1,colour);AbstractGui.func_238467_a_(m,x+3,y+h-1,x+w-3,y+h,colour);
        AbstractGui.func_238467_a_(m,x,y+3,x+1,y+h-3,colour);AbstractGui.func_238467_a_(m,x+w-1,y+3,x+w,y+h-3,colour);
        for(int dx=1;dx<=2;dx++){AbstractGui.func_238467_a_(m,x+dx,y+3-dx,x+dx+1,y+4-dx,colour);AbstractGui.func_238467_a_(m,x+w-dx-1,y+3-dx,x+w-dx,y+4-dx,colour);
            AbstractGui.func_238467_a_(m,x+dx,y+h-4+dx,x+dx+1,y+h-3+dx,colour);AbstractGui.func_238467_a_(m,x+w-dx-1,y+h-4+dx,x+w-dx,y+h-3+dx,colour);}
    }
}
