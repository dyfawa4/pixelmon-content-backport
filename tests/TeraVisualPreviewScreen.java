import java.util.*;
import java.util.function.Supplier;
import com.google.gson.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.models.smd.AnimationType;
import com.pixelmonmod.pixelmon.client.models.PixelmonModelSmd;
import com.pixelmonmod.pixelmon.client.models.smd.DeformVertex;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import local.pixelmon.backport.mixin.TeraVertexAccess;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

/** Distributed ONLY in the independent test helper. Actors never enter a world
 * or party; the production renderer handles their client-local visual metadata. */
public final class TeraVisualPreviewScreen extends Screen {
    static final int W=800,H=450,ROWS=15;
    final List<Species> species=new ArrayList<>(PixelmonSpecies.getAll()),filtered=new ArrayList<>();
    Species chosen;int page,form,type=2,animation;boolean shiny,compare=true,spin=true,paused;
    final AnimationType[] animations={AnimationType.IDLE,AnimationType.WALK,AnimationType.FLY,AnimationType.SPECIAL};
    PixelmonEntity original,crystal;TextFieldWidget search;
    float fit,ox,oy,yaw=30,pitch=12,zoom=1;String query="",error="";
    int frame;
    JsonArray reviewJobs,reviewResults;boolean reviewFinished;String reviewName;
    public TeraVisualPreviewScreen(){
        super(new StringTextComponent("太晶外观图鉴 · 测试专用"));
        species.sort(Comparator.comparingInt(Species::getDex));filtered.addAll(species);
        chosen=species.stream().filter(s->s.getDex()==6).findFirst().orElse(species.get(0));
        var requests=java.nio.file.Paths.get("tera-preview-review.json");
        if(java.nio.file.Files.isRegularFile(requests))try{
            reviewJobs=new JsonParser().parse(java.nio.file.Files.readString(requests)).getAsJsonArray();reviewResults=new JsonArray();
        }catch(Exception failure){throw new IllegalStateException("Invalid private material review jobs",failure);}
    }
    String name(Species s){return s.getNameTranslation().getString();}
    String typeName(int t){return new TranslationTextComponent("pixelmon.backport.tera.type."+TeraItems.NAMES[t]).getString();}
    List<Stats> forms(){return chosen.getForms();}
    String formName(){String value=forms().get(form).getLocalizedName();return value.isEmpty()?"普通形态":value;}
    void filter(String value){
        query=value;String key=value.trim().toLowerCase(Locale.ROOT).replace("#","");filtered.clear();
        for(Species s:species)if(key.isEmpty() || name(s).toLowerCase(Locale.ROOT).contains(key) || s.getName().toLowerCase(Locale.ROOT).contains(key) || Integer.toString(s.getDex()).equals(key) || s.getDex()==CapSpecies.DEX && ("帽皮小智戴帽子的皮卡丘".contains(key)))filtered.add(s);
        page=0;
    }
    void select(Species s){chosen=s;form=0;zoom=1;recreate();}
    void recreate(){
        error="";
        try{
            original=actor();crystal=actor();
            ((TeraVisualState)crystal).backport$setTeraVisualType(type);
        }catch(Exception failure){original=crystal=null;error=failure.getClass().getSimpleName()+": "+failure.getMessage();Pixelmon.LOGGER.error("Tera preview "+chosen.getName(),failure);}
    }
    PixelmonEntity actor(){
        Pokemon pokemon=PokemonFactory.create(chosen);pokemon.setForm(forms().get(form));pokemon.setLevelNum(60);pokemon.initialize();pokemon.setGrowth(EnumGrowth.Ordinary);pokemon.setShiny(shiny);
        if(reviewJobs!=null)pokemon.setGender(Gender.MALE);
        PixelmonEntity entity=new PixelmonEntity(Minecraft.func_71410_x().field_71441_e,pokemon);
        entity.func_70012_b(0,65,0,0,0);entity.setSpriteModel(false);entity.setAnimated(true);entity.setAnimation(animations[animation]);
        ((TeraVisualState)entity).backport$setTeraVisualType(-1);return entity;
    }
    void button(int x,int y,int w,int h,Supplier<String> label,Runnable click){func_230480_a_(new Card(x,y,w,h,label,click,-1));}
    @Override public void func_231160_c_(){
        field_230710_m_.clear();field_230705_e_.clear();
        fit=Math.min((field_230708_k_-16f)/W,(field_230709_l_-16f)/H);ox=(field_230708_k_-W*fit)/2;oy=(field_230709_l_-H*fit)/2;
        search=new TextFieldWidget(Minecraft.func_71410_x().field_71466_p,18,54,170,19,new StringTextComponent("搜索名称 / 英文 / 编号"));
        search.func_146180_a(query);search.func_212954_a(this::filter);field_230705_e_.add(search);
        for(int row=0;row<ROWS;row++){final int r=row;button(16,82+row*20,176,18,()->{int i=page*ROWS+r;return i<filtered.size()?String.format("%04d  %s",filtered.get(i).getDex(),name(filtered.get(i))):"";},()->{int i=page*ROWS+r;if(i<filtered.size())select(filtered.get(i));});}
        button(16,392,48,21,()->"上页",()->page=Math.max(0,page-1));
        button(144,392,48,21,()->"下页",()->page=Math.min(Math.max(0,(filtered.size()-1)/ROWS),page+1));
        button(216,49,30,22,()->"<",()->{form=Math.floorMod(form-1,forms().size());recreate();});
        button(250,49,230,22,()->formName()+"  "+(form+1)+"/"+forms().size(),()->{form=(form+1)%forms().size();recreate();});
        button(484,49,30,22,()->">",()->{form=(form+1)%forms().size();recreate();});
        button(520,49,88,22,()->shiny?"异色：开":"异色：关",()->{shiny=!shiny;recreate();});
        for(int i=0;i<19;i++){final int t=i;func_230480_a_(new Card(628+(i%2)*76,82+(i/2)*25,70,22,()->typeName(t),()->{type=t;if(crystal!=null)((TeraVisualState)crystal).backport$setTeraVisualType(type);},t));}
        button(628,342,146,22,()->"下一属性  >",()->{type=(type+1)%19;if(crystal!=null)((TeraVisualState)crystal).backport$setTeraVisualType(type);});
        button(216,359,92,22,()->compare?"原型对照：开":"原型对照：关",()->compare=!compare);
        button(312,359,92,22,()->spin?"自动旋转：开":"自动旋转：关",()->spin=!spin);
        button(408,359,92,22,()->paused?"继续动画":"暂停动画",()->paused=!paused);
        button(504,359,104,22,()->"动作："+new String[]{"待机","行走","飞行","特殊"}[animation],()->{animation=(animation+1)%animations.length;recreate();});
        button(216,390,58,22,()->"缩小 −",()->zoom=Math.max(.2f,zoom/1.2f));
        button(280,390,58,22,()->"放大 +",()->zoom=Math.min(5,zoom*1.2f));
        button(344,390,76,22,()->"重置视角",()->{zoom=1;yaw=30;pitch=12;});
        button(426,390,92,22,()->"变身光束",()->{if(crystal!=null)((TeraVisualState)crystal).backport$setTeraVisualStart(crystal.field_70170_p.func_82737_E());});
        button(524,390,84,22,()->"保存截图",this::screenshot);
        button(628,390,146,22,()->"返回测试地图 · Esc",()->func_231175_as__());
        if(original==null)recreate();
    }
    @Override public void func_231023_e_(){
        search.func_146178_a();if(spin)yaw+=.6f;
        if(!paused)for(PixelmonEntity actor:new PixelmonEntity[]{original,crystal})if(actor!=null){actor.field_70173_aa++;actor.getAnimationVariables().tick();actor.setAnimation(animations[animation]);}
    }
    void drawActor(MatrixStack matrix,PixelmonEntity actor,int x,int y,float pixels){
        if(actor==null)return;
        try{ScreenHelper.drawEntity(matrix,actor,x,y,pixels,yaw*2,pitch*2);}
        catch(Exception failure){error=failure.getClass().getSimpleName()+": "+failure.getMessage();}
        RenderSystem.enableDepthTest();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
    }
    @Override public void func_230430_a_(MatrixStack m,int mx,int my,float partial){
        if(reviewJobs!=null)reviewStep();
        func_238467_a_(m,0,0,field_230708_k_,field_230709_l_,0xff091321);
        m.func_227860_a_();m.func_227861_a_(ox,oy,0);m.func_227862_a_(fit,fit,1);
        panel(m,8,8,784,434,0xff152738);panel(m,12,45,186,374,0xff0e1e2c);
        panel(m,208,78,408,272,0xff20374b);panel(m,620,45,162,374,0xff0e1e2c);
        text(m,"太晶外观图鉴",20,19,0xeafaff);text(m,"测试专用 · "+species.size()+" 种",630,22,0x8eb6c7);
        text(m,"中文、英文或全国编号搜索",18,42,0x8faabd);
        text(m,String.format("%04d  %s",chosen.getDex(),name(chosen)),218,22,0xc5f9ee);
        text(m,"太晶属性 · 19 种",630,57,0xb5e8f1);
        text(m,(page+1)+" / "+Math.max(1,(filtered.size()+ROWS-1)/ROWS),78,399,0x93b0bd);
        text(m,"拖动旋转 · 滚轮缩放 · 所有对象均为临时预览",218,427,0x8da9bc);
        text(m,"F8 打开 / 关闭",630,427,0x8da9bc);
        if(compare){text(m,"原型",273,88,0x8ba5b9);text(m,typeName(type)+"太晶",476,88,0xa1e7ef);func_238467_a_(m,412,105,413,344,0xff2f4a60);}
        else text(m,typeName(type)+"太晶",382,88,0xa1e7ef);
        float pixels=fittedPixels()*zoom;
        var window=Minecraft.func_71410_x().func_228018_at_();
        double screenScale=(double)window.func_198105_m()/field_230708_k_;
        RenderSystem.enableScissor((int)((ox+208*fit)*screenScale),(int)(window.func_198083_n()-(oy+350*fit)*screenScale),(int)(408*fit*screenScale),(int)(246*fit*screenScale));
        if(compare)drawActor(m,original,310,325,pixels);
        drawActor(m,crystal,compare?514:412,325,pixels);
        RenderSystem.disableScissor();
        RenderSystem.disableDepthTest();
        if(!error.isEmpty())text(m,Minecraft.func_71410_x().field_71466_p.func_238412_a_(error,390),217,337,0xff9988);
        int lx=(int)((mx-ox)/fit),ly=(int)((my-oy)/fit);
        super.func_230430_a_(m,lx,ly,partial);search.func_230430_a_(m,lx,ly,partial);
        m.func_227865_b_();RenderSystem.enableDepthTest();
        frame++;
        if(reviewJobs==null && java.nio.file.Files.isRegularFile(java.nio.file.Paths.get("tera-preview-screenshot")) && frame==90)screenshot();
        if(reviewJobs!=null && !reviewFinished && frame%18==13)reviewCapture();
    }
    void reviewStep(){
        if(reviewFinished || frame%18!=0)return;
        int index=frame/18;
        if(index>=reviewJobs.size()){
            reviewFinished=true;
            try{java.nio.file.Files.writeString(java.nio.file.Paths.get("tera-review-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(reviewResults));}
            catch(Exception failure){failure.printStackTrace();}
            System.out.println("TERA_MATERIAL_REVIEW_COMPLETE cases="+reviewResults.size());
            Minecraft.func_71410_x().func_71400_g();return;
        }
        JsonObject q=reviewJobs.get(index).getAsJsonObject();reviewName=q.get("id").getAsString();
        if(!reviewName.matches("[a-zA-Z0-9_-]+"))throw new IllegalArgumentException("Invalid review case name");
        chosen=species.stream().filter(s->s.getDex()==q.get("dex").getAsInt()).findFirst().orElseThrow();
        form=0;if(q.has("form"))for(int i=0;i<forms().size();i++)if(forms().get(i).getName().equals(q.get("form").getAsString()))form=i;
        shiny=q.has("shiny") && q.get("shiny").getAsBoolean();type=q.get("type").getAsInt();
        yaw=q.has("yaw")?q.get("yaw").getAsFloat():40;pitch=12;zoom=1;compare=true;spin=false;paused=true;animation=0;
        recreate();if(original!=null)original.field_70173_aa=120;if(crystal!=null)crystal.field_70173_aa=120;
        System.out.println("TERA_MATERIAL_REVIEW_CASE "+reviewName);
    }
    void reviewCapture(){
        JsonObject result=new JsonObject();result.addProperty("id",reviewName);result.addProperty("error",error);
        try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(Minecraft.func_71410_x().func_228018_at_().func_198105_m(),Minecraft.func_71410_x().func_228018_at_().func_198083_n(),Minecraft.func_71410_x().func_147110_a())){
            var path=java.nio.file.Paths.get("screenshots","material-review",reviewName+".png");java.nio.file.Files.createDirectories(path.getParent());image.func_209270_a(path);result.addProperty("screenshot",path.toString());
        }catch(Exception failure){result.addProperty("error",failure.toString());}
        reviewResults.add(result);
    }
    float fittedPixels(){
        if(original!=null && original.getModel() instanceof PixelmonModelSmd){
            PixelmonModelSmd<?> model=(PixelmonModelSmd<?>)original.getModel();
            if(model.theModel!=null && model.theModel.body!=null){
                float x0=Float.POSITIVE_INFINITY,y0=x0,z0=x0,x1=-x0,y1=x1,z1=x1;
                for(DeformVertex v:model.theModel.body.verts){var p=((TeraVertexAccess)v).backport$restPosition();x0=Math.min(x0,p.x);y0=Math.min(y0,p.y);z0=Math.min(z0,p.z);x1=Math.max(x1,p.x);y1=Math.max(y1,p.y);z1=Math.max(z1,p.z);}
                float world=model.getScale()*original.getScaleFactor()*original.getPixelmonScale()*(PixelmonConfigProxy.getGraphics().isScaleModelsUp()?1.3f:1)/16;
                float horizontal=(float)Math.hypot(x1-x0,y1-y0)*world,height=(z1-z0)*world;
                return Math.min((compare?176:360)/Math.max(.1f,horizontal),205/Math.max(.2f,height*1.65f));
            }
        }
        return 85;
    }
    void screenshot(){
        try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(Minecraft.func_71410_x().func_228018_at_().func_198105_m(),Minecraft.func_71410_x().func_228018_at_().func_198083_n(),Minecraft.func_71410_x().func_147110_a())){
            var path=java.nio.file.Paths.get("screenshots","tera-preview-"+chosen.getDex()+"-"+TeraItems.NAMES[type]+"-"+System.currentTimeMillis()+".png");java.nio.file.Files.createDirectories(path.getParent());image.func_209270_a(path);System.out.println("TERA_PREVIEW_SCREENSHOT "+path);
        }catch(Exception failure){failure.printStackTrace();}
    }
    static void panel(MatrixStack m,int x,int y,int w,int h,int color){func_238467_a_(m,x,y,x+w,y+h,color);}
    static void text(MatrixStack m,String text,int x,int y,int color){ScreenHelper.drawString(m,text,x,y,color,false);}
    final class Card extends Button {
        final Supplier<String> label;final int attribute;
        Card(int x,int y,int w,int h,Supplier<String> label,Runnable run,int attribute){super(x,y,w,h,new StringTextComponent(""),b->run.run());this.label=label;this.attribute=attribute;}
        @Override public void func_230431_b_(MatrixStack m,int mx,int my,float partial){
            int x=field_230690_l_,y=field_230691_m_,w=field_230688_j_,h=field_230689_k_;
            boolean over=mx>=x && mx<x+w && my>=y && my<y+h,selected=attribute>=0 && attribute==type;
            panel(m,x,y,w,h,selected?0xff328b92:over?0xff315169:0xff1d3448);
            if(attribute>=0)panel(m,x,y,3,h,0xff000000|Tera.TYPES[attribute].getColor());
            text(m,Minecraft.func_71410_x().field_71466_p.func_238412_a_(label.get(),w-10),x+6,y+(h-8)/2,selected?0xffffff:0xc8e0eb);
        }
    }
    @Override public boolean func_231044_a_(double x,double y,int button){return super.func_231044_a_((x-ox)/fit,(y-oy)/fit,button);}
    @Override public boolean func_231048_c_(double x,double y,int button){return super.func_231048_c_((x-ox)/fit,(y-oy)/fit,button);}
    @Override public boolean func_231045_a_(double x,double y,int button,double dx,double dy){
        double lx=(x-ox)/fit,ly=(y-oy)/fit;if(button==0 && lx>208 && lx<616 && ly>78 && ly<350){spin=false;yaw+=dx/fit;pitch=Math.max(-70,Math.min(70,pitch+(float)dy/fit));return true;}
        return super.func_231045_a_(lx,ly,button,dx/fit,dy/fit);
    }
    @Override public boolean func_231043_a_(double x,double y,double scroll){
        if((x-ox)/fit<200){page=Math.max(0,Math.min(Math.max(0,(filtered.size()-1)/ROWS),page-(int)scroll));return true;}
        zoom=Math.max(.2f,Math.min(5,zoom*(scroll>0?1.12f:1/1.12f)));return true;
    }
    @Override public boolean func_231046_a_(int key,int scan,int modifiers){if(key==297){func_231175_as__();return true;}return super.func_231046_a_(key,scan,modifiers);}
    @Override public boolean func_231177_au__(){return false;}
}
