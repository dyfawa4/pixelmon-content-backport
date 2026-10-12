package local.pixelmon.backport;
import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.*;
import com.pixelmonmod.pixelmon.client.gui.moveskills.MoveSkillSelectScreen;
import com.pixelmonmod.pixelmon.client.listener.SendoutListener;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;

/** Native wheel art and native skill selector, with an explicit UUID-bound target. */
public final class FieldMegaScreen extends Screen {
    int slot;UUID identity;Pokemon pokemon;float radius;int cx,cy;
    public FieldMegaScreen(int slot){super(new TranslationTextComponent("pixelmon_backport.field_wheel.title"));this.slot=slot;}
    List<Integer> released(){
        List<Integer> slots=new ArrayList<>();var party=ClientStorageManager.party;
        if(party!=null && field_230706_i_.field_71441_e!=null)for(int i=0;i<6;i++){
            Pokemon p=party.get(i);if(p!=null && SendoutListener.isInWorld(p.getUUID(),field_230706_i_.field_71441_e))slots.add(i);
        }return slots;
    }
    void select(int slot){
        this.slot=slot;
        // The default setter invokes a DEDICATED_SERVER-only method stripped
        // from the client. Match the native client selection path explicitly.
        ClientStorageManager.party.setSelectedSlot(slot,false);
        ClientStorageManager.party.sendClientUpdatePacket();
        pokemon=ClientStorageManager.party.get(slot);identity=pokemon==null?null:pokemon.getUUID();
        FieldMegaClient.bind(identity);
        // A skill selected for one Pokemon must not silently follow another target.
        PixelmonOverlayScreen.selectedMoveSkill=null;
    }
    void cycle(int direction){
        List<Integer> slots=released();if(slots.isEmpty())return;
        int current=slots.indexOf(slot);select(slots.get(Math.floorMod(current+direction,slots.size())));
    }
    @Override protected void func_231160_c_(){
        cx=field_230708_k_/2;cy=field_230709_l_/2;radius=Math.min(field_230709_l_,field_230708_k_)*.35f;
        var party=ClientStorageManager.party;if(party==null)return;
        List<Integer> slots=released();if(!slots.contains(slot) && !slots.isEmpty())select(slots.get(0));
        else {pokemon=party.get(slot);identity=pokemon==null?null:pokemon.getUUID();}
        func_230480_a_(new Button(cx-(int)radius-32,cy-10,28,20,new StringTextComponent("<"),b->cycle(-1)));
        func_230480_a_(new Button(cx+(int)radius+4,cy-10,28,20,new StringTextComponent(">"),b->cycle(1)));
    }
    void image(MatrixStack m,ResourceLocation image,float x,float y,float size){ScreenHelper.drawImageQuad(image,m,x,y,size,size,0,0,1,1,1,1,1,1,1);}
    void label(MatrixStack m,String value,int x,int y,int color){func_238471_a_(m,field_230706_i_.field_71466_p,value,x,y,color);}
    String tr(String key){return new TranslationTextComponent("pixelmon_backport.field_wheel."+key).getString();}
    @Override public void func_230430_a_(MatrixStack m,int mx,int my,float partial){
        func_230446_a_(m);com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        image(m,Resources.moveSkillWheel,cx-radius,cy-radius,radius*2);
        image(m,Resources.moveSkillWheelCenter,cx-radius*.2f,cy-radius*.2f,radius*.4f);
        if(pokemon!=null)image(m,pokemon.getSprite(),cx-radius*.2f,cy-radius*.23f,radius*.4f);
        float icon=radius*.32f;image(m,new ResourceLocation("pixelmon","textures/gui/overlay/externalmoves/mega.png"),cx-icon/2,cy-radius*.64f-icon/2,icon);
        ResourceLocation skill=Resources.moveSkillPointer;
        if(pokemon!=null){var skills=ClientProxy.getMoveSkills(pokemon);for(var s:skills)if(!s.id.equals("change_form")){skill=s.sprite;if(s.id.equals(PixelmonOverlayScreen.selectedMoveSkill))break;}}
        image(m,skill,cx-icon/2,cy+radius*.61f-icon/2,icon);
        label(m,tr(FieldMega.transformed(pokemon)?"revert":FieldMega.primalSpecies(pokemon)?"primal":FieldMega.crownedSpecies(pokemon)?(pokemon.getSpecies().getDex()==888?"crowned_sword":"crowned_shield"):"mega"),cx,cy-(int)(radius*.40),0xffffff);
        label(m,tr("skills"),cx,cy+(int)(radius*.84),0xffffff);
        label(m,pokemon==null?tr("empty"):pokemon.getDisplayName()+" · "+(slot+1),cx,cy+(int)(radius*.25),0xffffff);
        label(m,tr("target"),cx,cy-(int)radius-15,0xb5dfef);
        label(m,released().isEmpty()?tr("sendout"):tr("switch"),cx,cy+(int)radius+10,0xa6b5c2);
        super.func_230430_a_(m,mx,my,partial);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
    @Override public boolean func_231044_a_(double x,double y,int button){
        if(button==0 && Math.abs(x-cx)<radius*.55 && Math.abs(y-cy)>radius*.32 && Math.abs(y-cy)<radius*.91 && identity!=null){
            Pokemon current=ClientStorageManager.party.get(slot);
            if(current==null || !identity.equals(current.getUUID()) || !released().contains(slot)){func_231175_as__();return true;}
            if(y<cy){FieldMega.CHANNEL.sendToServer(new FieldMega.Request(identity));func_231175_as__();}
            else {select(slot);field_230706_i_.func_147108_a(new Skills(slot));}return true;
        }return super.func_231044_a_(x,y,button);
    }
    @Override public boolean func_231177_au__(){return false;}
    /** Opening the unchanged native skill picker intentionally bypasses the combined wheel. */
    public static final class Skills extends MoveSkillSelectScreen {
        private final int targetSlot;private final UUID targetId;
        public Skills(int slot){super(slot);targetSlot=slot;
            Pokemon p=ClientStorageManager.party==null?null:ClientStorageManager.party.get(slot);
            targetId=p==null?null:p.getUUID();
        }
        private boolean valid(){
            var mc=Minecraft.func_71410_x();var party=ClientStorageManager.party;
            Pokemon p=party==null?null:party.get(targetSlot);
            return targetId!=null && p!=null && targetId.equals(p.getUUID()) && party.getSelectedSlot()==targetSlot
                && mc.field_71441_e!=null && SendoutListener.isInWorld(targetId,mc.field_71441_e);
        }
        @Override public void func_231160_c_(){
            if(!valid()){PixelmonOverlayScreen.selectedMoveSkill=null;func_231175_as__();return;}
            super.func_231160_c_();
        }
        @Override public boolean func_231044_a_(double x,double y,int button){
            if(!valid()){PixelmonOverlayScreen.selectedMoveSkill=null;func_231175_as__();return true;}
            FieldMegaClient.bind(targetId);
            return super.func_231044_a_(x,y,button);
        }
    }
}
