package local.pixelmon.backport;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.text.TranslationTextComponent;

/** Layout only: each effect owns and draws its independent badge. */
public final class BattleStatusIcons {
    public static int count(java.util.UUID id){return (ErosionClient.has(id)?1:0)+(CalamityClient.has(id)?1:0)+(LightningOathClient.active(id)?1:0);}
    public static int count(com.pixelmonmod.pixelmon.api.pokemon.Pokemon p){return p==null?0:(ErosionClient.has(p)?1:0)+(CalamityClient.has(p)?1:0)+(LightningOathClient.active(p.getUUID())?1:0);}
    public static boolean has(com.pixelmonmod.pixelmon.api.pokemon.Pokemon p){return count(p)>0;}
    public static void icons(MatrixStack m,com.pixelmonmod.pixelmon.api.pokemon.Pokemon p,float x,float y,float z,float size){if(p==null)return;int i=0;if(ErosionClient.has(p))ErosionClient.icon(m,x-i++*(size+2),y,z,size);if(CalamityClient.has(p))CalamityClient.icon(m,x-i++*(size+2),y,z,size);if(LightningOathClient.active(p.getUUID()))LightningOathClient.icon(m,x-i*(size+2),y,z,size);}
    public static void row(MatrixStack m,com.pixelmonmod.pixelmon.api.pokemon.Pokemon p,float x,float y,float z,float size){if(p==null)return;int i=0;if(ErosionClient.has(p))ErosionClient.icon(m,x+i++*(size+2),y,z,size);if(CalamityClient.has(p))CalamityClient.icon(m,x+i++*(size+2),y,z,size);if(LightningOathClient.active(p.getUUID()))LightningOathClient.icon(m,x+i*(size+2),y,z,size);}
    private static float extra(PixelmonClientData p){return Math.max(0,count(p.pokemonUUID)-1)*12.5f;}
    public static void badges(MatrixStack m,PixelmonClientData p,float x,float y,float z,float size){int i=0;if(ErosionClient.has(p.pokemonUUID))ErosionClient.icon(m,x+i++*(size+2),y,z,size);if(CalamityClient.has(p.pokemonUUID))CalamityClient.icon(m,x+i++*(size+2),y,z,size);if(LightningOathClient.active(p.pokemonUUID))LightningOathClient.icon(m,x+i*(size+2),y,z,size);}
    public static void opponent(MatrixStack m,PixelmonClientData p,int x,int y,float z,boolean raid){
        if(p==null)return;
        if(raid){badges(m,p,x+198-Math.max(0,count(p.pokemonUUID)-2)*22,y+12,z,20);return;}
        float left=x+54+com.pixelmonmod.pixelmon.client.gui.ScreenHelper.getStringWidth(p.getDisplayName())+(p.getGender()!=com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.NONE?7:0)+(com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.pokedex!=null&&com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.pokedex.hasCaught(p.species)?9:0)+(p.status!=-1?12:0);
        boolean inline=left<=x+117-extra(p);badges(m,p,inline?left:x+142,inline?y+4:y+32,z,10.5f);
    }
    public static void ally(MatrixStack m,PixelmonClientData p,int x,int y,float z){
        if(p==null)return;
        float left=x+14+com.pixelmonmod.pixelmon.client.gui.ScreenHelper.getStringWidth(p.getDisplayName())+(p.getGender()!=com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.NONE?7:0)+(p.status!=-1?12:0);
        boolean inline=left<=x+95-extra(p);badges(m,p,inline?left:x+13,inline?y+12.5f:y+40,z,10.5f);
    }
}
