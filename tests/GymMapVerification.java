import java.util.*;
import com.mojang.authlib.GameProfile;
import local.pixelmon.backport.GymCities;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.FakePlayerFactory;

/** Targeted map assembly, actual biome registration and badge display fixtures. */
public final class GymMapVerification {
    static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void run(MinecraftServer server){
        var world=server.func_241755_D_();int total=0;
        for(String type:GymTestMaps.TYPES){
            var pieces=GymTestMaps.assemble(world,type,BlockPos.field_177992_a);total+=pieces.size();
            require(pieces.stream().anyMatch(p->p.toString().contains("_pokecenter]")),"Inspection city lacks center "+type);
            require(pieces.stream().anyMatch(p->p.toString().contains("_pokemart]")),"Inspection city lacks mart "+type);
            if(GymCities.DATA.getAsJsonObject("newThemes").has(type)){
                int rewards=0,houses=0;var bounds=net.minecraft.util.math.MutableBoundingBox.func_78887_a();
                for(var piece:pieces){
                    bounds.func_78888_b(piece.func_74874_b());if(piece.toString().contains("_house_"))++houses;
                    var matcher=java.util.regex.Pattern.compile("pixelmon_backport:gyms/[a-z]+/[a-z0-9_]+").matcher(piece.toString());
                    if(!matcher.find())continue;
                    String path="data/pixelmon_backport/structures/"+matcher.group().split(":",2)[1]+".nbt";
                    try(var stream=GymCities.class.getClassLoader().getResourceAsStream(path)){
                        require(stream!=null,"Missing selected city template "+path);
                        var template=net.minecraft.nbt.CompressedStreamTools.func_74796_a(stream);var palette=template.func_150295_c("palette",10);
                        for(var raw:template.func_150295_c("blocks",10)){
                            var block=(CompoundNBT)raw;String name=palette.func_150305_b(block.func_74762_e("state")).func_74779_i("Name");
                            if(name.startsWith("pixelmon:") && name.endsWith("_chest"))++rewards;
                        }
                    }catch(java.io.IOException failure){throw new AssertionError(path,failure);}
                }
                require(houses>=8 && rewards>=6,"Incomplete populated/reward city "+type+" houses="+houses+" loot="+rewards);
                require(Math.min(bounds.func_78883_b(),bounds.func_78880_d())>=100 && Math.max(bounds.func_78883_b(),bounds.func_78880_d())>=140,"City footprint too small "+type+" "+bounds);
                System.out.println("BACKPORT_CITY_COMPONENTS "+type+" houses="+houses+" nativeLoot="+rewards+" bounds="+bounds);
            }
            System.out.println("BACKPORT_GYM_MAP_ASSEMBLY "+type+" pieces="+pieces.size());
        }
        var biomes=world.func_241828_r().func_243612_b(Registry.field_239720_u_);int locations=0;
        for(var theme:GymCities.DATA.getAsJsonObject("newThemes").entrySet())for(var name:theme.getValue().getAsJsonObject().getAsJsonArray("biomes")){
            var biome=biomes.func_82594_a(new ResourceLocation(name.getAsString()));require(biome!=null,"Missing configured biome "+name);
            require(biome.func_242440_e().func_242487_a().stream().anyMatch(s->s.get()==GymCities.FEATURES.get(theme.getKey())),"Biome lacks natural city config "+name);++locations;
        }
        var player=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("9575b03f-5571-4d58-bf8e-bc771f08505a"),"BadgeFixtureTest"));
        var data=GymTestMaps.data(world);data.ready=false;BadgeGallery.tick(player);
        GymTestMaps.navigation(world,data);
        for(int i=0;i<GymTestMaps.NAV.length;i++){
            BlockPos pos=new BlockPos(-6+(i%5)*3,149,i<5?117:123);
            require(GymTestMaps.NAV[i].equals(GymTestMaps.navigationAction(pos)),"Wrong gym navigation target "+i);
            require(world.func_180495_p(pos).func_177230_c().getRegistryName().toString().equals("minecraft:"+GymTestMaps.NAV_COLORS[i]+"_concrete"),"Missing gym navigation pad "+i);
        }
        Set<String> unique=new HashSet<>();for(var badge:BadgeGallery.badges())require(unique.add(badge.func_77973_b().getRegistryName().toString()),"Duplicate attribute badge");
        require(unique.size()==18 && data.ready && data.cursor==18,"Incomplete gallery");
        GymTestMaps.Data copy=new GymTestMaps.Data();copy.func_76184_a(data.func_189551_b(new CompoundNBT()));require(copy.ready && copy.cursor==18 && copy.navVersion==1,"Gallery/navigation state not persisted");
        System.out.println("BACKPORT_GYM_MAPS_PASS cities=18 center/mart/main gym; pieces="+total+" natural biome registrations="+locations+" distinct badges=18 gallery/save");
    }
}
