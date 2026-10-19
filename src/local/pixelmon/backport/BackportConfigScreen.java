package local.pixelmon.backport;
import com.google.gson.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import java.util.concurrent.atomic.AtomicInteger;

/** Native input widgets, Chinese labels, and width-independent row rendering. */
public final class BackportConfigScreen extends Screen {
    private static final AtomicInteger IDS=new AtomicInteger();
    private static final String[] LABELS={"常规","战斗","生成","繁殖","场外技能","维度","巢穴","结构","任务","指令","存储","画面（本机）"};
    private final Screen parent;
    private int group,page,pages=1,request,left,width,editY,loadedRows;
    private String search="",message="";
    private JsonArray entries=new JsonArray();private JsonObject selected;private JsonElement choice;
    private TextFieldWidget filter,value;private boolean pending,error;private Button saveButton;
    public BackportConfigScreen(Screen parent){super(new StringTextComponent("宝可梦配置"));this.parent=parent;}
    private String group(){return ConfigEditor.GROUPS[group];}
    private boolean local(){return group().equals("graphics");}
    private int rows(){return Math.max(1,Math.min(8,(field_230709_l_-200)/24));}
    private boolean choices(){return selected!=null&&(selected.has("options")||selected.get("value").isJsonPrimitive()&&selected.get("value").getAsJsonPrimitive().isBoolean());}
    private String label(JsonObject row){return row.has("label")?row.get("label").getAsString():ConfigLabels.text(row.get("path").getAsString());}
    private String fit(String text,int pixels){return field_230712_o_.func_238412_a_(text,Math.max(0,pixels));}
    private final class Action extends Button {
        private final boolean leading;
        Action(int x,int y,int w,int h,String text,boolean leading,IPressable action){super(x,y,w,h,new StringTextComponent(text),action);this.leading=leading;}
        @Override public void func_230431_b_(MatrixStack m,int mx,int my,float partial){
            int color=!field_230693_o_?0xff343434:field_230692_n_?0xff777777:0xff555555;
            func_238467_a_(m,field_230690_l_,field_230691_m_,field_230690_l_+field_230688_j_,field_230691_m_+field_230689_k_,0xff111111);
            func_238467_a_(m,field_230690_l_+1,field_230691_m_+1,field_230690_l_+field_230688_j_-1,field_230691_m_+field_230689_k_-1,color);
            func_238467_a_(m,field_230690_l_+1,field_230691_m_+1,field_230690_l_+field_230688_j_-1,field_230691_m_+2,field_230692_n_?0xffdddddd:0xff999999);
            String text=fit(func_230458_i_().getString(),field_230688_j_-12);
            int x=leading?field_230690_l_+7:field_230690_l_+(field_230688_j_-field_230712_o_.func_78256_a(text))/2;
            field_230712_o_.func_238421_b_(m,text,x,field_230691_m_+(field_230689_k_-8)/2,field_230693_o_?0xffffff:0x999999);
        }
    }
    private Button button(int x,int y,int w,String text,boolean leading,Button.IPressable action){return func_230480_a_(new Action(x,y,w,20,text,leading,action));}
    @Override protected void func_231160_c_(){
        String draft=value==null?null:value.func_146179_b();
        if(filter!=null)search=filter.func_146179_b();
        field_230710_m_.clear();field_230705_e_.clear();value=null;
        width=Math.min(520,field_230708_k_-24);left=(field_230708_k_-width)/2;editY=field_230709_l_-99;
        button(left,28,22,"<",false,b->changeGroup(-1));
        button(left+26,28,108,LABELS[group],false,b->changeGroup(1));
        button(left+138,28,22,">",false,b->changeGroup(1));
        filter=new TextFieldWidget(field_230712_o_,left+166,28,width-214,20,new StringTextComponent("搜索配置名称"));
        filter.func_146203_f(128);filter.func_146180_a(search);field_230705_e_.add(filter);
        button(left+width-44,28,44,"搜索",false,b->{search=filter.func_146179_b();page=0;refresh();});
        for(int i=0;i<Math.min(rows(),entries.size());i++){
            final JsonObject row=entries.get(i).getAsJsonObject();
            String text=label(row)+"："+ConfigInput.display(row.get("value"));
            if(selected==row)text="§e"+text;
            Button line=button(left,56+i*24,width,text,true,b->{selected=row;choice=row.get("value");message="";error=false;value=null;func_231160_c_();});line.field_230693_o_=!pending;
        }
        int nav=editY-44;
        Button previous=button(left,nav,52,"上一页",false,b->{page--;refresh();});previous.field_230693_o_=page>0&&!pending;
        Button next=button(left+58,nav,52,"下一页",false,b->{page++;refresh();});next.field_230693_o_=page+1<pages&&!pending;
        button(left+width-52,nav,52,"刷新",false,b->refresh());
        boolean editable=selected!=null&&!selected.get("readOnly").getAsBoolean()&&!pending;
        if(choices()){
            Button picker=button(left,editY,width-88,ConfigInput.display(choice)+"（点击切换）",false,b->{
                if(selected.has("options")){
                    JsonArray options=selected.getAsJsonArray("options");int index=0;for(int i=0;i<options.size();i++)if(options.get(i).equals(choice))index=i;
                    choice=options.get((index+1)%options.size());
                }else choice=new JsonPrimitive(!choice.getAsBoolean());
                func_231160_c_();
            });picker.field_230693_o_=editable;
        }else {
            value=new TextFieldWidget(field_230712_o_,left,editY,width-88,20,new StringTextComponent("配置值"));value.func_146203_f(12000);
            value.func_146180_a(selected==null?"":draft!=null?draft:ConfigInput.edit(selected.get("value")));value.func_146184_c(editable);field_230705_e_.add(value);
        }
        saveButton=button(left+width-80,editY,80,"保存此项",false,b->save());saveButton.field_230693_o_=editable;
        button(left+width-64,field_230709_l_-26,64,"完成",false,b->func_231175_as__());
    }
    private void changeGroup(int delta){group=(group+delta+LABELS.length)%LABELS.length;page=0;search="";filter=null;refresh();}
    @Override public void func_231175_as__(){Minecraft.func_71410_x().func_147108_a(parent);}
    @Override public void func_231023_e_(){if(filter!=null)filter.func_146178_a();if(value!=null)value.func_146178_a();if(request==0||!pending&&loadedRows!=rows())refresh();}
    private void refresh(){
        if(filter!=null)search=filter.func_146179_b();selected=null;choice=null;value=null;entries=new JsonArray();
        request=IDS.incrementAndGet();loadedRows=rows();pending=true;error=false;message="读取配置…";func_231160_c_();
        try{
            if(local())receive(ConfigEditor.page(group(),search,page,rows()),"");
            else if(Minecraft.func_71410_x().field_71439_g==null){pending=false;message="请进入世界后编辑服务器配置；画面设置可在此修改";func_231160_c_();}
            else {var r=new ConfigNetwork.Request(request,group(),search,page);r.size=rows();TeraNetwork.CHANNEL.sendToServer(r);}
        }catch(Exception e){pending=false;error=true;message=e.getMessage();func_231160_c_();}
    }
    private void save(){
        if(selected==null||pending||selected.get("readOnly").getAsBoolean())return;
        try{
            JsonElement next=choices()?choice:ConfigInput.parse(selected,value.func_146179_b());
            if(local()){String result=ConfigEditor.save(group(),selected.get("path").getAsString(),selected.get("value"),next);receive(ConfigEditor.page(group(),search,page,rows()),result);}
            else {request=IDS.incrementAndGet();var r=new ConfigNetwork.Request(request,group(),search,page);r.size=rows();r.path=selected.get("path").getAsString();r.expected=selected.get("value").toString();r.value=next.toString();pending=true;message="保存中…";saveButton.field_230693_o_=false;TeraNetwork.CHANNEL.sendToServer(r);}
        }catch(Exception e){error=true;message=e.getMessage();}
    }
    private void receive(JsonObject data,String text){entries=data.getAsJsonArray("entries");page=data.get("page").getAsInt();pages=data.get("pages").getAsInt();pending=false;message=text;selected=null;choice=null;value=null;func_231160_c_();}
    public static void accept(ConfigNetwork.Reply r){Screen current=Minecraft.func_71410_x().field_71462_r;if(!(current instanceof BackportConfigScreen))return;var s=(BackportConfigScreen)current;if(s.request!=r.id)return;s.pending=false;s.error=r.error;if(!r.data.isEmpty())s.receive(new JsonParser().parse(r.data).getAsJsonObject(),r.message);else{s.message=r.message;s.func_231160_c_();}}
    private String hint(){
        if(selected==null)return "点击配置项后编辑，再点“保存此项”；可用中文搜索";
        if(selected.get("readOnly").getAsBoolean())return "此列表过长，请在原配置文件中修改；原数据不会被截断";
        if(choices())return "点击切换选项，再保存";
        if(selected.get("value").isJsonArray())return "各项用分号“；”隔开；清空表示空列表";
        if(selected.get("value").isJsonPrimitive()&&selected.get("value").getAsJsonPrimitive().isNumber())return "直接输入数字"+(selected.has("min")?"；范围 "+selected.get("min").getAsString()+" ～ "+selected.get("max").getAsString():"");
        return "直接输入文字，无需加引号";
    }
    @Override public void func_230430_a_(MatrixStack m,int mx,int my,float partial){
        func_238467_a_(m,0,0,field_230708_k_,field_230709_l_,0xf0101010);
        func_238471_a_(m,field_230712_o_,"宝可梦配置",field_230708_k_/2,10,0xffffff);
        super.func_230430_a_(m,mx,my,partial);if(filter!=null)filter.func_230430_a_(m,mx,my,partial);if(value!=null)value.func_230430_a_(m,mx,my,partial);
        field_230712_o_.func_238421_b_(m,(page+1)+" / "+pages+" · "+(local()?"本机设置":"房主／管理员设置"),left+120,editY-38,0xb7d4e8);
        field_230712_o_.func_238421_b_(m,fit(hint(),width),left,editY-12,0xdddddd);
        String help=selected==null?"":selected.get("help").getAsString();
        if(selected!=null&&selected.get("restart").getAsBoolean())help="保存后需要重启。"+help;
        if(!help.isEmpty()){
            var lines=field_230712_o_.func_238425_b_(new StringTextComponent(help),width);
            for(int i=0;i<Math.min(2,lines.size());i++)field_230712_o_.func_238422_b_(m,lines.get(i),left,editY+27+i*11,0xb7d4e8);
        }
        if(message!=null&&!message.isEmpty())field_230712_o_.func_238421_b_(m,fit(message.replace('\n',' '),width),left,field_230709_l_-52,error?0xff7777:0xb7d4e8);
        else if(entries.size()==0&&!pending)field_230712_o_.func_238421_b_(m,"没有匹配的配置项",left,58,0xcccccc);
    }
}
