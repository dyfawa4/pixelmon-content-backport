import com.google.gson.*;
import local.pixelmon.backport.Content;
import java.util.*;
import java.util.jar.*;
import java.nio.file.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Audit every merged Mega species/form/gender/palette used by native Boss HUDs. */
public final class MegaBossAvatarVerification {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Map<Integer,JsonObject> species=new TreeMap<>();
        List<String> rows=new ArrayList<>();Set<String> customSprites=new TreeSet<>();int forms=0,palettes=0;
        try(JarFile jar=new JarFile(args[0])){
            for(var e=jar.entries();e.hasMoreElements();){var entry=e.nextElement();String path=entry.getName();
                if(!path.startsWith("data/pixelmon/species/")||!path.endsWith(".json"))continue;
                try(Reader in=new InputStreamReader(jar.getInputStream(entry),java.nio.charset.StandardCharsets.UTF_8)){
                    JsonObject s=new JsonParser().parse(in).getAsJsonObject();species.put(s.get("dex").getAsInt(),s);
                }
            }
            for(JsonElement e:Content.DATA.getAsJsonArray("patches")){
                JsonObject row=e.getAsJsonObject(),incoming=row.getAsJsonObject("species");int dex=incoming.get("dex").getAsInt();
                if(!species.containsKey(dex))species.put(dex,incoming.deepCopy());
                else if(!row.get("mode").getAsString().equals("new"))species.put(dex,Content.mergeSpecies(species.get(dex),incoming));
            }
            for(JsonObject s:species.values())for(JsonElement f:s.getAsJsonArray("forms")){
                JsonObject form=f.getAsJsonObject();if(!form.get("name").getAsString().startsWith("mega"))continue;
                forms++;require(form.has("genderProperties"),"No Mega avatar gender data "+s.get("dex"));
                for(JsonElement g:form.getAsJsonArray("genderProperties")){
                    JsonObject group=g.getAsJsonObject();Set<String> names=new HashSet<>();
                    for(JsonElement p:group.getAsJsonArray("palettes")){
                        JsonObject palette=p.getAsJsonObject();String name=palette.get("name").getAsString();names.add(name);
                        require(palette.has("sprite"),"Missing Mega palette sprite "+s.get("dex")+" "+name);
                        String id=palette.get("sprite").getAsString();String[] parts=id.split(":",2);
                        String path="assets/"+parts[0]+"/textures/"+parts[1];BufferedImage image;
                        if(parts[0].equals("pixelmon_backport")){
                            Path file=Path.of("resources",path);require(Files.isRegularFile(file),"Missing custom Mega sprite "+id);image=ImageIO.read(file.toFile());customSprites.add(path);
                        }else{
                            JarEntry entry=jar.getJarEntry(path);require(entry!=null,"Missing native Mega sprite "+id);
                            try(InputStream in=jar.getInputStream(entry)){image=ImageIO.read(in);}
                        }
                        require(image!=null&&image.getWidth()==32&&image.getHeight()==32,"Mega avatar must use native 32x32 sprite "+id);
                        require(image.getColorModel().hasAlpha(),"Opaque Mega avatar "+id);
                        boolean visible=false,transparent=false;
                        for(int y=0;y<32;y++)for(int x=0;x<32;x++){int a=image.getRGB(x,y)>>>24;visible|=a>0;transparent|=a==0;}
                        require(visible&&transparent,"Empty/unmasked Mega sprite "+id);palettes++;
                        rows.add("| "+s.get("dex").getAsInt()+" | "+s.get("name").getAsString()+" | "+form.get("name").getAsString()+" | "+group.get("gender").getAsString()+" | "+name+" | 32×32 / 实际资源存在 |\n");
                    }
                    require(names.contains("none")&&names.contains("shiny"),"Normal/shiny Mega palettes absent "+s.get("dex"));
                }
            }
        }
        String report="# Mega Boss头像资源核查\n\n原生数据与回迁补丁使用实际Content.mergeSpecies合并；逐一检查所有Mega形态的性别与普通、闪光、特殊材质头像引用。检查PNG尺寸、透明背景和实际资源，不能替代游戏内人工验收。\n\n"+
            "Mega形态："+forms+"；性别/材质引用："+palettes+"；自定义独立头像："+customSprites.size()+"。\n\n| 编号 | 宝可梦 | 形态 | 性别资源组 | 材质 | 检查结果 |\n|---|---|---|---|---|---|\n"+String.join("",rows);
        Files.writeString(Path.of("docs/MEGA_BOSS_AVATAR_AUDIT_1.4.1.md"),report,java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("PASS every merged Mega Boss avatar: "+forms+" forms, "+palettes+" gender/palette references, "+customSprites.size()+" custom sprite assets; all actual transparent 32x32 resources. Gameplay remains manual.");
    }
}
