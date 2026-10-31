package local.pixelmon.backport.client;
import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.*;
import local.pixelmon.backport.EvolutionGuideNetwork;
/** The fourth native PokeChecker tab: same frame, left Pokemon panel, arrows and return destination. */
public final class EvolutionGuideScreen extends PokeCheckerScreen {
    private UUID queried;private String queriedForm;private int request,page,line;private JsonObject data;private int lines;
    private Button prev,next;private static int sequence;private final Map<String,Pokemon> sprites=new HashMap<>();
    public EvolutionGuideScreen(PokeCheckerScreen previous){super(previous);}
    public EvolutionGuideScreen(PokemonStorage storage,StoragePosition position,Screen parent){super(storage,position,parent);}
    public static void accept(EvolutionGuideNetwork.Reply reply){var screen=Minecraft.func_71410_x().field_71462_r;if(screen instanceof EvolutionGuideScreen){var s=(EvolutionGuideScreen)screen;if(reply.sequence==s.request&&reply.pokemon.equals(s.queried)){s.data=reply.data;s.page=0;s.line=0;}}}
    @Override public void func_231160_c_(){super.func_231160_c_();int x=field_230708_k_/2,y=field_230709_l_/2;prev=func_230480_a_(new Button(x-29,y+61,19,14,new StringTextComponent("◀"),b->change(-1)));next=func_230480_a_(new Button(x+101,y+61,19,14,new StringTextComponent("▶"),b->change(1)));}
    void change(int amount){int count=data!=null&&data.has("routes")?data.getAsJsonArray("routes").size():0;if(count>0){page=Math.floorMod(page+amount,count);line=0;}}
    void query(){if(pokemon==null)return;String form=pokemon.getForm().getName();if(!pokemon.getUUID().equals(queried)||!form.equals(queriedForm)){queried=pokemon.getUUID();queriedForm=form;request=++sequence;data=null;page=line=0;sprites.clear();EvolutionGuideNetwork.CHANNEL.sendToServer(new EvolutionGuideNetwork.Request(queried,request));}}
    @Override public void func_230430_a_(MatrixStack m,int mx,int my,float partial){query();super.func_230430_a_(m,mx,my,partial);}
    @Override public void drawGuiContainerForegroundLayer(MatrixStack m,int mx,int my){
        // Retain native left-hand details without leaving invisible right-hand text in the
        // depth buffer, which would cut holes through the new condition text.
        boolean clipped=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);int[] box=new int[4];org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX,box);
        int edge=(int)((field_230708_k_/2-35)*(field_230706_i_.func_228018_at_().func_198105_m()/(double)field_230708_k_));
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(clipped?box[0]:0,clipped?box[1]:0,Math.max(0,Math.min(edge,clipped?box[0]+box[2]:edge)-(clipped?box[0]:0)),clipped?box[3]:field_230706_i_.func_228018_at_().func_198083_n());
        try{super.drawGuiContainerForegroundLayer(m,mx,my);}finally{org.lwjgl.opengl.GL11.glScissor(box[0],box[1],box[2],box[3]);if(!clipped)org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);}
        int left=field_230708_k_/2-32-guiLeft,top=field_230709_l_/2-105-guiTop;
        // Replace only the right content pane, retaining all native left-hand Pokemon information.
        func_238467_a_(m,left,top,left+155,top+184,0xffc6c6c6);
        center(m,EvolutionGuideText.tr("title"),left+77,top+5,0xffffff);
        if(prev==null)return;int count=data!=null&&data.has("routes")?data.getAsJsonArray("routes").size():0;prev.field_230693_o_=next.field_230693_o_=count>1;
        if(data==null){center(m,EvolutionGuideText.tr("loading"),left+77,top+72,0x555555);return;}
        if(!data.has("routes")){center(m,EvolutionGuideText.tr("unavailable"),left+77,top+72,0x555555);return;}
        if(data.has("egg")&&data.get("egg").getAsBoolean()){center(m,EvolutionGuideText.tr("egg"),left+77,top+72,0x555555);return;}
        boolean terminal=data.get("final").getAsBoolean();int y=top+19;
        if(terminal){center(m,EvolutionGuideText.tr("final"),left+77,y,0x555555);y+=13;}
        if(count==0)return;
        var route=data.getAsJsonArray("routes").get(Math.min(page,count-1)).getAsJsonObject();center(m,EvolutionGuideText.tr("group."+route.get("group").getAsString()),left+77,y,0xffffff);y+=12;
        var items=new ArrayList<net.minecraft.item.ItemStack>();for(var c:route.getAsJsonArray("conditions")){var v=c.getAsJsonObject();if(v.has("item"))items.add(EvolutionGuideText.item(v));}
        int itemCount=Math.min(items.size(),2);
        drawSprite(m,route.getAsJsonObject("from"),left+5,y,itemCount==2?26:30);drawSprite(m,route.getAsJsonObject("to"),left+(itemCount==2?125:119),y,itemCount==2?26:30);
        for(int i=0;i<itemCount;i++){int ix=left+(itemCount==1?69:50+i*40);field_230707_j_.func_180450_b(items.get(i),ix+guiLeft,y+7+guiTop);}
        if(itemCount==0)center(m,"→",left+77,y+11,0x555555);
        else{center(m,"＋",left+(itemCount==1?47:38),y+11,0x555555);if(itemCount==2)center(m,"＋",left+78,y+11,0x555555);center(m,"→",left+(itemCount==1?107:118),y+11,0x555555);}y+=35;
        String text=net.minecraft.util.text.TextFormatting.func_110646_a(EvolutionGuideText.formula(route));var wrapped=field_230706_i_.field_71466_p.func_238425_b_(new StringTextComponent(text),143);lines=wrapped.size();int visible=Math.max(1,(top+160-y)/11);line=Math.max(0,Math.min(line,Math.max(0,lines-visible)));
        for(int i=0;i<visible&&line+i<wrapped.size();i++)field_230706_i_.field_71466_p.func_238407_a_(m,wrapped.get(line+i),left+6,y+i*11,0xffffff);
        if(lines>visible){int barY=y+(int)((top+157-y-12)*(line/(double)Math.max(1,lines-visible)));func_238467_a_(m,left+151,y,left+153,top+157,0xffa4a4a4);func_238467_a_(m,left+151,barY,left+153,barY+12,0xff686868);}
        center(m,(page+1)+" / "+count,left+77,top+169,0xffffff);
        m.func_227860_a_();m.func_227861_a_(-guiLeft,-guiTop,0);prev.func_230430_a_(m,mx,my,0);next.func_230430_a_(m,mx,my,0);m.func_227865_b_();
    }
    void center(MatrixStack m,String text,int x,int y,int color){func_238471_a_(m,field_230706_i_.field_71466_p,text,x,y,color);}
    void drawSprite(MatrixStack m,JsonObject id,int x,int y,int size){String key=id.toString();Pokemon p=pokemon.getSpecies().getDex()==id.get("dex").getAsInt()&&pokemon.getForm().getName().equals(EvolutionGuideText.string(id,"form",""))?pokemon:sprites.computeIfAbsent(key,k->{var v=PokemonFactory.create(EvolutionGuideText.species(id));v.setForm(EvolutionGuideText.form(id));v.setShiny(pokemon.isShiny());return v;});ScreenHelper.drawImageQuad(p.getSprite(),m,x,y,size,size,0,0,1,1,1,1,1,1,1);}
    @Override public boolean func_231043_a_(double x,double y,double scroll){if(x>=field_230708_k_/2-32&&x<field_230708_k_/2+123){line=Math.max(0,Math.min(Math.max(0,lines-1),line+(scroll>0?-2:2)));return true;}return super.func_231043_a_(x,y,scroll);}
    @Override public boolean func_231044_a_(double x,double y,int button){
        // Summary-only rename/ribbon hotspots are outside this pane or native controls.
        if(x>=field_230708_k_/2-32&&x<field_230708_k_/2+123&&y>=field_230709_l_/2-105&&y<field_230709_l_/2+58)return true;
        return super.func_231044_a_(x,y,button);
    }
}
