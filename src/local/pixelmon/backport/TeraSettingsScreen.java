package local.pixelmon.backport;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.client.gui.Resources;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import java.util.UUID;

/** Fixed design coordinates with matched rendering/input transforms at any GUI scale. */
public final class TeraSettingsScreen extends Screen {
    public static final int WIDTH=480,HEIGHT=330;
    private static final ResourceLocation ORB=new ResourceLocation(Content.NS,"textures/item/tera_orb.png");
    private static final ResourceLocation STELLAR=new ResourceLocation(Content.NS,"textures/gui/type/stellar.png");
    private TeraNetwork.Reply data;
    private int chosen,candidate;
    private float scale,originX,originY;
    private boolean pending;
    public TeraSettingsScreen(TeraNetwork.Reply data){super(new TranslationTextComponent("pixelmon.backport.tera.settings"));this.data=data;candidate=data.types.isEmpty()?0:data.types.get(0);}
    public void accept(TeraNetwork.Reply reply){
        UUID previous=data.ids.isEmpty()?null:data.ids.get(chosen);
        data=reply;pending=false;chosen=previous==null?0:Math.max(0,data.ids.indexOf(previous));
        if(reply.feedback==1 && !data.ids.isEmpty())candidate=data.types.get(chosen);
        func_231160_c_();
    }
    public static float fit(int width,int height){return Math.max(.1f,Math.min(1.25f,Math.min((width-24f)/WIDTH,(height-24f)/HEIGHT)));}
    private static String tr(String key,Object... args){return new TranslationTextComponent("pixelmon.backport.tera.ui."+key,args).getString();}
    private static String type(int index){return new TranslationTextComponent("pixelmon.backport.tera.type."+TeraItems.NAMES[index]).getString();}
    private boolean empty(){return data.ids.isEmpty();}
    private int current(){return empty()?0:data.types.get(chosen);}
    private int lock(){return empty()?0:data.locked.get(chosen);}
    public boolean canChange(){return !empty() && !pending && lock()==0 && candidate!=current() && data.anyOrb && (data.creative || data.shards[candidate]>=50);}
    @Override public void func_231160_c_(){
        field_230710_m_.clear();field_230705_e_.clear();
        scale=fit(field_230708_k_,field_230709_l_);originX=(field_230708_k_-WIDTH*scale)/2;originY=(field_230709_l_-HEIGHT*scale)/2;
        chosen=Math.min(chosen,Math.max(0,data.ids.size()-1));
        for(int i=0;i<data.ids.size();i++){final int slot=i;func_230480_a_(new Card(12,61+i*34,164,30,data.names.get(i),b->{chosen=slot;candidate=current();data.feedback=0;func_231160_c_();},0,i));}
        for(int i=0;i<19;i++){final int index=i;Card card=new Card(188+(i%4)*70,61+(i/4)*39,66,35,new StringTextComponent(type(i)),b->{candidate=index;data.feedback=0;func_231160_c_();},1,i);card.field_230693_o_=!empty() && lock()==0 && !pending;func_230480_a_(card);}
        Card confirm=new Card(308,298,158,23,new StringTextComponent(tr(pending?"pending":"confirm",type(candidate))),b->{if(!canChange())return;pending=true;TeraNetwork.CHANNEL.sendToServer(new TeraNetwork.Request(-1,data.ids.get(chosen),3,candidate));func_231160_c_();},2,0);confirm.field_230693_o_=canChange();func_230480_a_(confirm);
        func_230480_a_(new Card(12,298,72,23,new TranslationTextComponent("gui.done"),b->func_231175_as__(),2,-1));
    }
    static void panel(MatrixStack m,int x,int y,int w,int h,int edge,int fill){func_238467_a_(m,x+2,y,x+w-2,y+h,edge);func_238467_a_(m,x,y+2,x+w,y+h-2,edge);func_238467_a_(m,x+2,y+2,x+w-2,y+h-2,fill);}
    static void icon(MatrixStack m,int type,int x,int y,int size){
        if(type==18)ScreenHelper.drawImageQuad(STELLAR,m,x,y,size,size,0,0,1,1,1,1,1,1,0);
        else ScreenHelper.drawImageQuadUV(Resources.types,m,x,y,size,size,Tera.TYPES[type].textureX,Tera.TYPES[type].textureY,240,240,1,1,1,1,0);
    }
    private String reason(){
        if(empty())return tr("empty");if(pending)return tr("pending");
        if(lock()!=0)return tr(lock()==1?"ogerpon":"terapagos");
        if(!data.anyOrb)return tr("no_orb");if(candidate==current())return tr("same",type(current()));
        if(!data.creative && data.shards[candidate]<50)return tr("shortage",type(candidate),50-data.shards[candidate]);
        return data.creative?tr("creative",type(candidate)):tr("cost",type(candidate));
    }
    @Override public void func_230430_a_(MatrixStack m,int mx,int my,float partial){
        func_238467_a_(m,0,0,field_230708_k_,field_230709_l_,0xc908111e);
        m.func_227860_a_();m.func_227861_a_(originX,originY,0);m.func_227862_a_(scale,scale,1);
        panel(m,0,0,WIDTH,HEIGHT,0xff405f78,0xf5142131);
        ScreenHelper.drawImageQuad(ORB,m,14,10,24,24,0,0,1,1,1,1,1,1,0);
        text(m,field_230704_d_.getString(),45,11,0xf0fbff);text(m,tr("subtitle"),45,25,0x98b0c8);
        text(m,tr(data.chargedOrb?"charged":"spent"),318,18,data.chargedOrb?0x86e6c1:0xe2b580);
        text(m,tr("party"),12,46,0x9db9d3);text(m,tr("types"),188,46,0x9db9d3);
        func_238467_a_(m,180,46,181,278,0xff30495d);
        if(empty())text(m,tr("empty"),20,80,0xced7df);
        else {icon(m,current(),193,260,13);text(m,tr("current",type(current())),211,262,0xe1f5ff);}
        text(m,reason(),12,282,canChange()?0x9adecb:0xc3cfdb);
        if(data.feedback!=0)text(m,tr(data.feedback==1?"success":"rejected"),100,305,data.feedback==1?0x89e1b5:0xffb099);
        super.func_230430_a_(m,(int)((mx-originX)/scale),(int)((my-originY)/scale),partial);
        m.func_227865_b_();
    }
    static void text(MatrixStack m,String s,int x,int y,int color){ScreenHelper.drawString(m,s,x,y,color,false);}
    private class Card extends Button {
        private final int kind,index;
        Card(int x,int y,int w,int h,ITextComponent title,IPressable click,int kind,int index){super(x,y,w,h,title,click);this.kind=kind;this.index=index;}
        @Override public void func_230431_b_(MatrixStack m,int mx,int my,float partial){
            int x=field_230690_l_,y=field_230691_m_,w=field_230688_j_,h=field_230689_k_;
            boolean hover=field_230693_o_ && mx>=x && mx<x+w && my>=y && my<y+h;
            boolean selected=kind==0?chosen==index:kind==1 && candidate==index;
            int color=kind==1?Tera.TYPES[index].getColor():0x63cce2;
            int edge=selected?0xff000000|color:hover?0xff64869f:0xff314b62;
            panel(m,x,y,w,h,edge,selected?0xff213e50:hover?0xff294257:0xff1c2c3e);
            int foreground=field_230693_o_?0xe8f5ff:0x8495a6;
            if(kind==0){
                Pokemon pokemon=null;if(ClientStorageManager.party!=null)for(Pokemon p:ClientStorageManager.party.getAll())if(p!=null && p.getUUID().equals(data.ids.get(index)))pokemon=p;
                if(pokemon!=null)ScreenHelper.drawImageQuad(pokemon.getSprite(),m,x+2,y-1,32,32,0,0,1,1,1,1,1,1,0);
                String name=Minecraft.func_71410_x().field_71466_p.func_238412_a_(func_230458_i_().getString(),108);
                text(m,name,x+36,y+5,foreground);icon(m,data.types.get(index),x+36,y+17,10);text(m,type(data.types.get(index)),x+49,y+18,0x93b2c9);
                if(selected)text(m,"●",x+w-12,y+10,0x8ceded);
            }else if(kind==1){
                icon(m,index,x+4,y+4,13);text(m,type(index),x+21,y+6,foreground);
                text(m,data.creative?tr("free"):data.shards[index]+" / 50",x+5,y+22,data.creative || data.shards[index]>=50?0x94dabd:0x8a9bab);
                if(!empty() && current()==index)text(m,"•",x+w-9,y+5,0xd6fdff);
            }else func_238471_a_(m,Minecraft.func_71410_x().field_71466_p,func_230458_i_().getString(),x+w/2,y+8,foreground);
        }
    }
    @Override public boolean func_231044_a_(double x,double y,int button){return super.func_231044_a_((x-originX)/scale,(y-originY)/scale,button);}
    @Override public boolean func_231048_c_(double x,double y,int button){return super.func_231048_c_((x-originX)/scale,(y-originY)/scale,button);}
    @Override public boolean func_231045_a_(double x,double y,int button,double dx,double dy){return super.func_231045_a_((x-originX)/scale,(y-originY)/scale,button,dx/scale,dy/scale);}
    @Override public boolean func_231177_au__(){return false;}
}
