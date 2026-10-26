package local.pixelmon.backport;

import java.util.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.*;

/** Same fixed-coordinate rendering/input transforms as the Tera orb editor. */
public final class MovingAgreementScreen extends Screen {
    public static final int WIDTH=500,HEIGHT=334,PAGE_SIZE=20;
    public MovingAgreement.State data;public int chosen,candidate=-1,page;private float scale,ox,oy;private boolean pending;
    public MovingAgreementScreen(MovingAgreement.State data){super(new TranslationTextComponent("item.pixelmon_backport.moving_agreement"));this.data=data;}
    public static void receive(MovingAgreement.State state){var mc=Minecraft.func_71410_x();if(state.open)mc.func_147108_a(new MovingAgreementScreen(state));else if(mc.field_71462_r instanceof MovingAgreementScreen){var screen=(MovingAgreementScreen)mc.field_71462_r;if(screen.data.token.equals(state.token)&&state.revision>screen.data.revision){UUID previous=screen.data.ids.isEmpty()?null:screen.data.ids.get(screen.chosen);screen.data=state;screen.chosen=previous==null?0:Math.max(0,state.ids.indexOf(previous));screen.pending=false;screen.func_231160_c_();}}}
    static String tr(String key,Object... args){return new TranslationTextComponent("pixelmon.backport.moving."+key,args).getString();}
    static PokeBall resolve(String name){for(PokeBall b:PokeBallRegistry.getAll())if(b.getName().equals(name))return b;return null;}
    static String name(String id){PokeBall ball=resolve(id);return ball==null?id:ball.getLocalizedName();}
    private String current(){return data.ids.isEmpty()?"":data.current.get(chosen);}
    public boolean canSelect(int index){return index>=0&&index<data.balls.size()&&data.counts.get(index)>0&&!pending&&!data.ids.isEmpty();}
    public boolean canChange(){return canSelect(candidate)&&data.agreements>0&&!current().equals(data.balls.get(candidate));}
    public int pages(){return Math.max(1,(data.balls.size()+PAGE_SIZE-1)/PAGE_SIZE);}
    @Override public void func_231160_c_(){
        field_230710_m_.clear();field_230705_e_.clear();scale=Math.max(.1f,Math.min(1.25f,Math.min((field_230708_k_-24f)/WIDTH,(field_230709_l_-24f)/HEIGHT)));ox=(field_230708_k_-WIDTH*scale)/2;oy=(field_230709_l_-HEIGHT*scale)/2;
        chosen=Math.min(chosen,Math.max(0,data.ids.size()-1));page=Math.min(page,pages()-1);
        for(int i=0;i<data.ids.size();i++){final int index=i;add(12,56+i*35,172,32,data.names.get(i),0,i,()->{chosen=index;data.feedback=0;func_231160_c_();},!pending);}
        for(int i=0;i<PAGE_SIZE;i++){final int index=page*PAGE_SIZE+i;if(index>=data.balls.size())break;add(198+(i%4)*72,56+(i/4)*39,68,35,name(data.balls.get(index)),1,index,()->{candidate=index;data.feedback=0;func_231160_c_();},canSelect(index));}
        add(198,255,36,20,"<",2,-1,()->{page--;func_231160_c_();},page>0&&!pending);add(450,255,36,20,">",2,-1,()->{page++;func_231160_c_();},page+1<pages()&&!pending);
        add(12,303,65,22,new TranslationTextComponent("gui.done").getString(),2,-1,()->func_231175_as__(),true);
        add(335,303,151,22,tr(pending?"pending":"confirm"),2,-1,()->{if(!canChange())return;pending=true;MovingAgreement.CHANNEL.sendToServer(new MovingAgreement.Request(data.token,data.revision,data.ids.get(chosen),current(),data.balls.get(candidate)));func_231160_c_();},canChange());
    }
    private void add(int x,int y,int w,int h,String text,int kind,int index,Runnable click,boolean enabled){Card card=new Card(x,y,w,h,text,kind,index,b->click.run());card.field_230693_o_=enabled;func_230480_a_(card);}
    private String reason(){if(pending)return tr("pending");if(data.ids.isEmpty())return tr("empty");if(data.agreements<=0)return tr("no_agreement");if(candidate<0)return tr("choose");if(data.counts.get(candidate)<=0)return tr("no_ball");if(current().equals(data.balls.get(candidate)))return tr("same");return tr("cost",name(data.balls.get(candidate)));}
    @Override public void func_230430_a_(MatrixStack m,int mx,int my,float partial){
        func_238467_a_(m,0,0,field_230708_k_,field_230709_l_,0xc908111e);m.func_227860_a_();m.func_227861_a_(ox,oy,0);m.func_227862_a_(scale,scale,1);
        TeraSettingsScreen.panel(m,0,0,WIDTH,HEIGHT,0xff405f78,0xf5142131);text(m,field_230704_d_.getString(),12,12,0xf0fbff);text(m,tr("subtitle"),12,27,0x98b0c8);text(m,tr("party"),12,43,0x9db9d3);text(m,tr("balls"),198,43,0x9db9d3);text(m,tr("stock",data.agreements),355,13,0xb9dfd4);
        func_238467_a_(m,190,43,191,274,0xff30495d);if(data.ids.isEmpty())text(m,tr("empty"),18,75,0xb1bccb);text(m,(page+1)+" / "+pages(),314,261,0xb1c8da);text(m,reason(),12,284,canChange()?0x95dfbd:0xc1cad8);
        if(data.feedback!=0)text(m,tr(data.feedback==1?"success":"rejected"),90,310,data.feedback==1?0x8cdeb7:0xffaa99);
        super.func_230430_a_(m,(int)((mx-ox)/scale),(int)((my-oy)/scale),partial);m.func_227865_b_();
        int lx=(int)((mx-ox)/scale),ly=(int)((my-oy)/scale),column=(lx-198)/72,row=(ly-56)/39;
        if(lx>=198&&column<4&&ly>=56&&row<5&&(lx-198)%72<68&&(ly-56)%39<35){int index=page*PAGE_SIZE+row*4+column;if(index<data.balls.size())func_243308_b(m,Arrays.asList(new StringTextComponent(name(data.balls.get(index))),new StringTextComponent(tr("count",data.counts.get(index)))),mx,my);}
    }
    static void text(MatrixStack m,String value,int x,int y,int color){ScreenHelper.drawString(m,value,x,y,color,false);}
    static void icon(MatrixStack m,String ball,int x,int y,int size){PokeBall b=resolve(ball);if(b!=null)ScreenHelper.drawImageQuad(b.getGUISprite(),m,x,y,size,size,0,0,1,1,1,1,1,1,0);}
    private class Card extends Button {
        final int kind,index;Card(int x,int y,int w,int h,String title,int kind,int index,IPressable click){super(x,y,w,h,new StringTextComponent(title),click);this.kind=kind;this.index=index;}
        @Override public void func_230431_b_(MatrixStack m,int mx,int my,float partial){int x=field_230690_l_,y=field_230691_m_,w=field_230688_j_,h=field_230689_k_;boolean selected=kind==0?chosen==index:kind==1&&candidate==index;boolean hover=field_230693_o_&&mx>=x&&mx<x+w&&my>=y&&my<y+h;TeraSettingsScreen.panel(m,x,y,w,h,selected?0xff67d5c5:hover?0xff64869f:0xff314b62,selected?0xff213e50:0xff1c2c3e);int fg=field_230693_o_?0xe8f5ff:0x748798;
            if(kind==0){Pokemon p=ClientStorageManager.party==null?null:ClientStorageManager.party.get(data.ids.get(index));if(p!=null)ScreenHelper.drawImageQuad(p.getSprite(),m,x+1,y,30,30,0,0,1,1,1,1,1,1,0);text(m,trim(data.names.get(index),128),x+33,y+5,fg);icon(m,data.current.get(index),x+33,y+17,12);text(m,trim(name(data.current.get(index)),111),x+48,y+19,0x9eb8cc);}
            else if(kind==1){icon(m,data.balls.get(index),x+2,y+2,17);text(m,trim(name(data.balls.get(index)),45),x+21,y+6,fg);text(m,tr("count",data.counts.get(index)),x+4,y+23,data.counts.get(index)>0?0x95d9bd:0x748798);}
            else func_238471_a_(m,Minecraft.func_71410_x().field_71466_p,func_230458_i_().getString(),x+w/2,y+7,fg);
        }
    }
    private static String trim(String text,int width){return Minecraft.func_71410_x().field_71466_p.func_238412_a_(text,width);}
    @Override public boolean func_231044_a_(double x,double y,int button){return super.func_231044_a_((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean func_231048_c_(double x,double y,int button){return super.func_231048_c_((x-ox)/scale,(y-oy)/scale,button);}
    @Override public boolean func_231045_a_(double x,double y,int button,double dx,double dy){return super.func_231045_a_((x-ox)/scale,(y-oy)/scale,button,dx/scale,dy/scale);}
    @Override public boolean func_231177_au__(){return false;}
}
