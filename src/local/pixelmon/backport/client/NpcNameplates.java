package local.pixelmon.backport.client;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.entities.npcs.*;
import local.pixelmon.backport.Content;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;
/**1.21-style role/name lines, using the native synchronized identity and distance gate. */
public final class NpcNameplates {
    private NpcNameplates(){}
    public static String name(NPCEntity npc){
        if(npc.func_200201_e()!=null)return npc.func_200201_e().getString();
        if(npc.getNickName()!=null&&!npc.getNickName().isBlank())return npc.getNickName();
        String name=npc.func_200200_C_().getString();
        if(npc instanceof NPCFisherman&&(name.isBlank()||name.equals("Old Fisherman")))return I18n.func_135052_a("pixelmon.backport.npc.fisherman_name");
        if(npc instanceof NPCTrainer){
            NPCTrainer trainer=(NPCTrainer)npc;
            // Stock trainers store a name-list index (often "0"), not a label.
            // Keep the index in native NBT; resolve it only at presentation sites.
            if(trainer.getBaseTrainer()!=null){
                try{String localized=trainer.getName(Minecraft.func_71410_x().field_71474_y.field_74363_ab);if(localized!=null&&!localized.isBlank()&&(!trainer.usingDefaultName||!localized.equals(name)||!index(name)))return localized;}
                catch(IndexOutOfBoundsException ignored){} // Old data-pack name list no longer has this index.
            }
            if(name.isBlank()||trainer.usingDefaultName&&index(name))return I18n.func_135052_a("pixelmon.backport.npc.trainer");
        }
        if(!name.isBlank())return local.pixelmon.backport.NpcLocalization.translate(name,Minecraft.func_71410_x().field_71474_y.field_74363_ab);
        if(npc instanceof NPCNurseJoy)return I18n.func_135052_a(npc.getTextureIndex()==1?"pixelmon.backport.npc.joy":"pixelmon.backport.npc.doctor");
        return I18n.func_135052_a("pixelmon.backport.npc.villager");
    }
    private static boolean index(String value){return value!=null&&!value.isEmpty()&&value.chars().allMatch(Character::isDigit);}
    public static String level(NPCEntity npc){
        if(!(npc instanceof NPCTrainer))return "";
        var tier=npc.getBossTier();
        if(tier!=null&&!tier.isNotBoss())return "equal".equalsIgnoreCase(tier.getID())?I18n.func_135052_a("pixelmon.backport.npc.level.equal"):"";
        int value=((NPCTrainer)npc).getTrainerLevel();
        return value>0?I18n.func_135052_a("pixelmon.backport.npc.level",value):"";
    }
    public static String tooltip(NPCEntity npc){String name=name(npc),level=level(npc);return level.isBlank()?name:name+" · "+level;}
    public static String role(NPCEntity npc,String display,String subtitle){
        if(npc instanceof NPCShopkeeper)return I18n.func_135052_a("gui.shopkeeper");
        if(npc instanceof NPCNurseJoy)return I18n.func_135052_a(npc.getTextureIndex()==1?"pixelmon.backport.npc.nurse":"pixelmon.backport.npc.doctor");
        if(npc instanceof NPCFisherman)return I18n.func_135052_a("pixelmon.backport.npc.fisherman");
        if(npc instanceof NPCQuestGiver)return I18n.func_135052_a("pixelmon.backport.npc.quest");
        if(npc instanceof NPCChatting)return I18n.func_135052_a("pixelmon.backport.npc.villager");
        if(npc instanceof NPCTrader)return I18n.func_135052_a("pixelmon.backport.npc.trader");
        if(npc instanceof NPCRelearner)return I18n.func_135052_a("pixelmon.backport.npc.relearner");
        if(npc instanceof NPCTutor)return I18n.func_135052_a("pixelmon.backport.npc.tutor");
        if(npc instanceof NPCTrainer){
            var tier=npc.getBossTier();
            if(tier!=null&&tier.isBoss())return I18n.func_135052_a(tier.getName());
            return I18n.func_135052_a("pixelmon.backport.npc.trainer");
        }
        String title=display==null?"":display;
        if(title.equals(name(npc)))title=I18n.func_135052_a("pixelmon.backport.npc.role");
        if(subtitle!=null&&!subtitle.isBlank()&&!subtitle.equals(name(npc)))title=title.isBlank()?subtitle:title+" · "+subtitle;
        return title;
    }
    public static void draw(NPCEntity npc,String display,String subtitle,MatrixStack matrix,IRenderTypeBuffer buffer,int light){
        if(npc.func_82150_aj())return;
        var mc=Minecraft.func_71410_x();String name=name(npc),role=role(npc,display,subtitle);
        if(name.isBlank())name=display==null?"":display;
        String level=level(npc);
        if(!level.isBlank()){
            boolean fallback=npc instanceof NPCTrainer&&npc.func_200201_e()==null&&(npc.getNickName()==null||npc.getNickName().isBlank())&&name.equals(I18n.func_135052_a("pixelmon.backport.npc.trainer"));
            // A client may not have the server-only trainer name catalogue ID.
            // In that case show role + real level, rather than repeating "Trainer".
            if(fallback)name=level;else role=role.isBlank()?level:role+" · "+level;
        }
        matrix.func_227860_a_();
        matrix.func_227861_a_(0,npc.getModelScale().func_195900_b()<1?2.1:2.5,0);
        matrix.func_227863_a_(mc.func_175598_ae().func_229098_b_());matrix.func_227862_a_(-.025f,-.025f,.025f);
        var transform=matrix.func_227866_c_().func_227870_a_();
        int background=(int)(mc.field_71474_y.func_216840_a(.25f)*255)<<24;
        // Depth-aware normal text keeps labels from showing through walls.
        if(!role.isBlank()&&!role.equals(name))mc.field_71466_p.func_243247_a(new StringTextComponent(role),-mc.field_71466_p.func_78256_a(role)/2f,-12,0x5599ff,false,transform,buffer,false,background,light);
        mc.field_71466_p.func_243247_a(new StringTextComponent(name),-mc.field_71466_p.func_78256_a(name)/2f,0,0xffffff,false,transform,buffer,false,background,light);
        matrix.func_227865_b_();
    }
}
