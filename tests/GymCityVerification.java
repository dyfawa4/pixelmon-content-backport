import java.io.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.world.generation.features.GymVillagePiece;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructureFeatures;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructures;
import com.pixelmonmod.pixelmon.world.structure.type.GymStructure;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import local.pixelmon.backport.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.feature.jigsaw.*;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.structure.*;

/** Native template loading, full city assembly, badge rewards, saved teams and scope. */
public final class GymCityVerification {
    static void require(boolean value,String label){if(!value)throw new AssertionError(label);}
    static NPCTrainer trainer(ServerWorld world,String path)throws Exception{
        String[] resource=path.split("/structures/",2);
        CompoundNBT root=world.func_184163_y().func_200219_b(new ResourceLocation(resource[0],resource[1].substring(0,resource[1].length()-4))).func_189552_a(new CompoundNBT());
        {
            CompoundNBT nbt=root.func_150295_c("entities",10).func_150305_b(0).func_74775_l("nbt");
            NPCTrainer npc=new NPCTrainer(world);npc.func_70020_e(nbt);return npc;
        }
    }
    static List<String> roster(NPCTrainer npc){
        List<String> result=new ArrayList<>();
        for(Pokemon p:npc.getPokemonStorage().getAll())if(p!=null){
            CompoundNBT training=new CompoundNBT();p.getIVs().writeToNBT(training);p.getEVs().writeToNBT(training);
            result.add(p.getUUID()+":"+p.getSpecies().getDex()+":"+p.getPokemonLevel()+":"+NpcDecisionVerification.moves(p)+":"+training);
        }
        return result;
    }
    public static void badgeSource(String type)throws Exception{
        MinecraftServer server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        NPCTrainer npc=trainer(server.func_241755_D_(),"pixelmon_backport/structures/gyms/"+type+"/gym_"+type+"_leader_a.nbt");
        require(Arrays.stream(npc.getWinnings()).anyMatch(s->s.func_77973_b()==Backport.REGISTERED.get(type+"_gym_badge").get()),"Badge not in native winnings "+type);
    }
    public static void run(MinecraftServer server)throws Exception{
        ServerWorld world=server.func_241755_D_();
        // A native template silently substitutes air for missing blocks. Verify every
        // generated palette before accepting successful jigsaw assembly.
        int paletteCount=0;
        {
            for(JsonElement entry:Content.read("backport/gym_templates.json").getAsJsonArray("templates")){
                CompoundNBT nbt;try(InputStream in=GymCities.class.getClassLoader().getResourceAsStream(entry.getAsString())){nbt=CompressedStreamTools.func_74796_a(in);}
                for(INBT raw:nbt.func_150295_c("palette",10)){
                    CompoundNBT state=(CompoundNBT)raw;ResourceLocation id=new ResourceLocation(state.func_74779_i("Name"));
                    require(net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(id),"Missing city block "+id+" in "+entry.getAsString());
                    var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(id);CompoundNBT properties=state.func_74775_l("Properties");
                    for(String property:properties.func_150296_c()){
                        var p=block.func_176194_O().func_185920_a(property);require(p!=null && p.func_185929_b(properties.func_74779_i(property)).isPresent(),"Invalid city block property "+id+" "+property+" "+properties);
                    }++paletteCount;
                }
            }
        }
        System.out.println("BACKPORT_CITY_PALETTES_PASS states="+paletteCount);
        Registry<JigsawPattern> pools=world.func_241828_r().func_243612_b(Registry.field_243555_ax);
        int cityCount=0,totalPieces=0;
        Set<Element> types=EnumSet.noneOf(Element.class);
        for(JsonElement raw:GymCities.DATA.getAsJsonArray("nativeSignatures"))types.add(Element.valueOf(raw.getAsJsonObject().get("type").getAsString().toUpperCase(Locale.ROOT)));
        for(Map.Entry<String,JsonElement> entry:GymCities.DATA.getAsJsonObject("newThemes").entrySet()){
            String type=entry.getKey(),anchor=entry.getValue().getAsJsonObject().get("anchor").getAsString();
            types.add(Element.valueOf(type.toUpperCase(Locale.ROOT)));badgeSource(type);
            NPCTrainer npc=trainer(world,"pixelmon_backport/structures/gyms/"+type+"/gym_"+type+"_leader_a.nbt");
            int[] levels=Arrays.stream(npc.getPokemonStorage().getAll()).filter(Objects::nonNull).mapToInt(Pokemon::getPokemonLevel).toArray();
            GymCities.initialize(npc);require(type.equals(npc.getPersistentData().func_74779_i(GymCities.TYPE)),"Gym theme lost");
            require(Arrays.equals(levels,Arrays.stream(npc.getPokemonStorage().getAll()).filter(Objects::nonNull).mapToInt(Pokemon::getPokemonLevel).toArray()),"New gym raised levels");
            for(Pokemon p:npc.getPokemonStorage().getAll())if(p!=null){
                require(p.getForm().getTypes().contains(Element.valueOf(type.toUpperCase(Locale.ROOT))),"Off-theme base team "+type+" "+p.getSpecies().getName());
                NpcDecisionVerification.legal(p);
            }
            List<String> before=roster(npc);CompoundNBT saved=new CompoundNBT();npc.func_213281_b(saved);
            NPCTrainer restored=new NPCTrainer(world);restored.func_70020_e(saved);GymCities.initialize(restored);
            require(before.equals(roster(restored)),"Gym team rerolled on reload "+type+" before="+before+" after="+roster(restored));
            require(NpcGimmicks.enabled(restored),"New equal gym not enhanced "+type);
            JigsawPattern start=pools.func_82594_a(new ResourceLocation("pixelmon_backport","gyms/"+type+"/start_pool"));
            require(start!=null && start.func_214946_c()>0,"New city start pool not loaded "+type);
            require(GymCities.FEATURES.containsKey(type),"New configured city absent "+type);
            List<AbstractVillagePiece> best=Collections.emptyList();
            for(int seed=1;seed<=24;seed++){
                List<AbstractVillagePiece> pieces=new ArrayList<>();
                JigsawManager.func_242837_a(world.func_241828_r(),new VillageConfig(()->start,8),
                    (a,b,c,d,e,f)->new GymVillagePiece(a,b,c,d,e,f,true),world.func_72863_F().func_201711_g(),world.func_184163_y(),
                    new BlockPos(cityCount*512,0,1024),pieces,new Random(seed),true,true);
                if(pieces.size()>best.size())best=pieces;
                boolean gym=pieces.stream().anyMatch(p->p.toString().contains("/gym_"+type+"]"));
                if(gym && pieces.size()>=12){best=pieces;break;}
            }
            require(best.size()>=12,"City generated too few buildings "+type+": "+best.size());
            require(best.stream().anyMatch(p->p.toString().contains("/gym_"+type+"]")),"City failed to assemble gym "+type);
            MutableBoundingBox bounds=MutableBoundingBox.func_78887_a();for(AbstractVillagePiece piece:best)bounds.func_78888_b(piece.func_74874_b());
            require(Math.max(bounds.func_78883_b(),bounds.func_78880_d())>=80,"City is not a full district "+type);
            System.out.println("BACKPORT_CITY_ASSEMBLY "+type+" pieces="+best.size()+" bounds="+bounds);totalPieces+=best.size();++cityCount;
        }
        require(types.size()==18 && cityCount==8,"Incomplete eighteen gym types "+types);
        Set<String> naturalTypes=new TreeSet<>();Set<String> badgeIds=new HashSet<>();int nativeCount=0;
        for(Map.Entry<String,JsonElement> nativeEntry:GymCities.DATA.getAsJsonObject("nativeBadges").entrySet()){
            String type=nativeEntry.getKey();
            NPCTrainer leader=trainer(world,"pixelmon/structures/gyms/"+type+"/gym_"+type+"_leader_a.nbt");
            require(leader.getWinnings().length>0 && !leader.getWinnings()[0].func_190926_b(),"Original gym lacks badge "+type);
            require(badgeIds.add(leader.getWinnings()[0].func_77973_b().getRegistryName().toString()),"Original type badges overlap "+type);
            require(world.func_184163_y().func_200219_b(new ResourceLocation("pixelmon","gyms/"+type+"/gym_"+type)).func_186259_a().func_177958_n()>=20,"Original gym template absent "+type);
            StructureFeature<?,?> feature=(StructureFeature<?,?>)PixelmonStructureFeatures.class.getField(type.toUpperCase(Locale.ROOT)+"_GYM").get(null);
            GymConfig config=(GymConfig)feature.field_236269_c_;
            Set<String> expected=new TreeSet<>();expected.add(type);
            Set<String> observed=new TreeSet<>();
            for(int seed=1;seed<=128 && !observed.containsAll(expected);seed++){
                GymStructure.Start start=new GymStructure.Start((GymStructure)PixelmonStructures.GYMS.get(),0,0,MutableBoundingBox.func_78887_a(),0,seed);
                start.generatePieces(world.func_241828_r(),world.func_72863_F().func_201711_g(),world.func_184163_y(),0,0,world.func_226691_t_(BlockPos.field_177992_a),config);
                require(start.func_186161_c().size()>=12,"Native generation produced only a shell "+type);
                String root=start.func_186161_c().get(0).toString();
                for(String possible:expected)if(root.contains("gyms/"+possible+"/"))observed.add(possible);
            }
            require(observed.containsAll(expected),"Native natural generation missed themes "+type+" "+observed+" wanted "+expected);
            naturalTypes.addAll(observed);++nativeCount;
        }
        for(var entry:GymCities.FEATURES.entrySet()){
            GymConfig config=(GymConfig)entry.getValue().field_236269_c_;
            boolean found=false;
            for(int seed=1;seed<=32 && !found;seed++){
                GymStructure.Start start=new GymStructure.Start((GymStructure)PixelmonStructures.GYMS.get(),0,0,MutableBoundingBox.func_78887_a(),0,seed);
                start.generatePieces(world.func_241828_r(),world.func_72863_F().func_201711_g(),world.func_184163_y(),0,0,world.func_226691_t_(BlockPos.field_177992_a),config);
                require(start.func_186161_c().size()>=12,"New natural city is a shell "+entry.getKey());
                found=start.func_186161_c().get(0).toString().contains("gyms/"+entry.getKey()+"/");
            }
            require(found,"Native generation failed new city "+entry.getKey());naturalTypes.add(entry.getKey());
        }
        require(nativeCount==10 && naturalTypes.size()==18,"Natural city generation incomplete "+naturalTypes);
        for(String type:new String[]{"ground"})for(String suffix:new String[]{"a1","c1"})
            require(trainer(world,"pixelmon/structures/gyms/"+type+"/gym_"+type+"_leader_"+suffix+".nbt").getWinnings().length>0,"Missing legacy badge repair "+suffix);
        NPCTrainer old=trainer(world,"pixelmon/structures/gyms/electric/gym_electric_leader_a.nbt");
        require(!old.isGymLeader && GymCities.theme(old)==Element.ELECTRIC && NpcGimmicks.enabled(old),"Unmarked native gym detection failed");
        old.getPersistentData().func_74778_a(GymCities.TYPE,"");require(GymCities.theme(old)==Element.ELECTRIC,"Existing native gym signature failed");
        NPCTrainer ordinary=new NPCTrainer(world);ordinary.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));ordinary.getPokemonStorage().set(0,TeraVerification.mon(25));
        require(!NpcGimmicks.enabled(ordinary),"Ordinary equal trainer enhanced");ordinary.isGymLeader=true;
        ordinary.setBossTier(BossTierRegistry.getBossTierOrNotBoss("epic"));require(!NpcGimmicks.enabled(ordinary),"Non-equal gym enhanced");
        ordinary.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));ordinary.getPersistentData().func_74757_a("BackportNpcDisable",true);
        require(!NpcGimmicks.enabled(ordinary),"Disabled trainer enhanced");
        System.out.println("BACKPORT_GYM_CITIES_PASS types="+types.size()+" new cities="+cityCount+" assembled pieces="+totalPieces+" natural themes="+naturalTypes.size()+" native badges, saved legal teams and equal gym scope");
    }
}
