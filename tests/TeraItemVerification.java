package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.HealerEvent;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import io.netty.buffer.Unpooled;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.*;
import net.minecraftforge.common.util.FakePlayer;

/** Semantic checks of consumption, ownership, recharge and native player battles. */
public final class TeraItemVerification {
    private static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    private static Pokemon mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.heal();return p;}
    public static void run(FakePlayer player){
        PlayerPartyStorage party=StorageProxy.getParty(player);int cases=0;
        boolean creative=player.field_71075_bZ.field_75098_d;
        player.field_71075_bZ.field_75098_d=false;
        try{
            for(int i=0;i<Tera.TYPES.length;i++){
                player.field_71071_by.field_70462_a.clear();player.field_71071_by.field_184439_c.clear();
                Pokemon p=mon(143);Tera.changeType(p,Tera.TYPES[(i+1)%Tera.TYPES.length]);party.set(0,p);
                ItemStack orb=new ItemStack(Backport.REGISTERED.get("tera_orb").get());player.field_71071_by.field_184439_c.set(0,orb);
                require(TeraItems.hasOrb(player),"Offhand orb "+i);
                ItemStack a=new ItemStack(Backport.REGISTERED.get(TeraItems.NAMES[i]+"_tera_shard").get(),30),b=new ItemStack(a.func_77973_b(),19);
                player.field_71071_by.field_70462_a.set(0,a);player.field_71071_by.field_70462_a.set(1,b);
                require(!TeraItems.change(player,p,i) && a.func_190916_E()==30 && b.func_190916_E()==19,"49 shards accepted/consumed "+i);
                b.func_190920_e(20);
                require(TeraItems.change(player,p,i) && a.func_190926_b() && b.func_190926_b(),"50 shard split-stack consumption "+i);
                require(Tera.type(PokemonFactory.create(p.writeToNBT(new CompoundNBT())))==Tera.TYPES[i],"Persist changed type "+i);
                player.field_71071_by.field_70462_a.set(0,new ItemStack(Backport.REGISTERED.get(TeraItems.NAMES[i]+"_tera_shard").get(),50));
                require(!TeraItems.change(player,p,i) && player.field_71071_by.field_70462_a.get(0).func_190916_E()==50,"Same-type rejection "+i);
                orb.func_196082_o().func_74757_a("BackportTeraSpent",true);
                require(!TeraItems.hasOrb(player),"Spent orb accepted "+i);
                Pixelmon.EVENT_BUS.post(new HealerEvent.Post(player,""));
                require(TeraItems.hasOrb(player),"Native healer recharge "+i);++cases;
            }
            Pokemon p=mon(1024);party.set(0,p);
            require(!TeraItems.change(player,p,0),"Terapagos locked type");
            p=mon(1017);party.set(0,p);require(!TeraItems.change(player,p,0),"Ogerpon locked type");
            require(!TeraItems.change(player,mon(143),0),"Foreign Pokemon ownership");
            require(!TeraItems.change(player,p,-1) && !TeraItems.change(player,p,19),"Invalid type");
            p=mon(6);party.set(0,p);
            TeraNetwork.Reply data=TeraNetwork.Reply.party(Collections.singletonList(p));
            PacketBuffer buf=new PacketBuffer(Unpooled.buffer());
            try{TeraNetwork.Reply.encode(data,buf);TeraNetwork.Reply copy=TeraNetwork.Reply.decode(buf);require(copy.names.get(0) instanceof TranslationTextComponent,"Server flattened translated species name");require(copy.ids.equals(data.ids) && copy.types.equals(data.types),"Tera reply roundtrip");}finally{buf.release();}
            p.setNickname("测试中文昵称");require(TeraNetwork.Reply.party(Collections.singletonList(p)).names.get(0).getString().equals("测试中文昵称"),"Nickname lost");
            player.field_71071_by.field_70462_a.clear();player.field_71071_by.field_184439_c.clear();
            PlayerParticipant owner=new PlayerParticipant(player,p);WildPixelmonParticipant enemy=new WildPixelmonParticipant(mon(143));
            BattleController bc=new BattleController(new BattleParticipant[]{owner},new BattleParticipant[]{enemy},new BattleRules());bc.sendMessages=false;bc.modifyStats();
            PixelmonWrapper u=owner.allPokemon[0];
            require(Tera.select(u) && !u.megaEvolve() && !Tera.active(u),"Missing orb activated Tera");
            ItemStack orb=new ItemStack(Backport.REGISTERED.get("tera_orb").get());player.field_71071_by.field_184439_c.set(0,orb);
            require(Tera.select(u) && u.megaEvolve() && Tera.active(u) && !TeraItems.charged(orb),"Player Tera did not consume charge");
            PacketBuffer snapshotBuffer=new PacketBuffer(Unpooled.buffer());
            try{TeraNetwork.Snapshot snapshot=TeraNetwork.Snapshot.of(bc);TeraNetwork.Snapshot.encode(snapshot,snapshotBuffer);TeraNetwork.Snapshot copy=TeraNetwork.Snapshot.decode(snapshotBuffer);require(copy.battle==snapshot.battle && copy.defense.equals(snapshot.defense) && copy.active.equals(snapshot.active) && copy.attackStats.equals(snapshot.attackStats),"Battle defense/stat snapshot roundtrip");require(copy.active.containsKey(p.getUUID()) && copy.attackStats.containsKey(p.getUUID()),"Active Tera/stats missing from snapshot");}finally{snapshotBuffer.release();}
            for(int flags=0;flags<4;flags++){
                PacketBuffer packet=new PacketBuffer(Unpooled.buffer());
                try{TeraNetwork.Snapshot sample=new TeraNetwork.Snapshot(42);sample.defense.put(p.getUUID(),java.util.Arrays.asList(com.pixelmonmod.pixelmon.api.pokemon.Element.WATER));sample.attackStats.put(p.getUUID(),flags);TeraNetwork.Snapshot.encode(sample,packet);require(TeraNetwork.Snapshot.decode(packet).attackStats.get(p.getUUID())==flags,"Stat flag roundtrip "+flags);}finally{packet.release();}
            }
            PacketBuffer invalid=new PacketBuffer(Unpooled.buffer());
            try{invalid.writeInt(42);invalid.writeByte(1);invalid.func_179252_a(p.getUUID());invalid.writeByte(-1);invalid.writeByte(4);boolean rejected=false;try{TeraNetwork.Snapshot.decode(invalid);}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"Invalid stat flags accepted");}finally{invalid.release();}
            Tera.end(bc);require(!TeraItems.hasOrb(player),"Battle end recharged orb");
            pokeBags(player);
            System.out.println("BACKPORT_TERA_ITEMS_PASS types="+cases+" split stacks, offhand, ownership, charge, healer event, network names, player battle");
        }finally{party.set(0,null);player.field_71071_by.field_70462_a.clear();player.field_71071_by.field_184439_c.clear();player.field_71075_bZ.field_75098_d=creative;}
    }
    private static void pokeBags(FakePlayer player){
        player.field_71071_by.field_70462_a.clear();player.field_71071_by.field_184439_c.clear();
        ItemStack bagStack=new ItemStack(com.pixelmonmod.pixelmon.api.registries.PixelmonItems.red_poke_bag);
        player.field_71071_by.field_70462_a.set(0,bagStack);
        com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag bag=com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,bagStack);
        com.pixelmonmod.pixelmon.container.PokeBagContainer container=new com.pixelmonmod.pixelmon.container.PokeBagContainer(42,player.field_71071_by,bag);
        int items=0;
        for(net.minecraftforge.fml.RegistryObject<net.minecraft.item.Item> registered:Backport.REGISTERED.values()){
            ItemStack item=new ItemStack(registered.get());
            require(container.field_75151_b.get(0).func_75214_a(item),"Native bag rejects "+registered.getId());
            bag.inventory.func_70299_a(2,item);
            require(com.pixelmonmod.pixelmon.api.battles.BattleItemScanner.findMatchingItem(item,player)!=null,"Native scanner missing "+registered.getId());
            require(com.pixelmonmod.pixelmon.api.battles.BattleItemScanner.consumeItem(item,player)!=null,"Native bag consume missing "+registered.getId());
            require(com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,bagStack).inventory.func_70301_a(2).func_190926_b(),"Native bag consumption not persisted "+registered.getId());
            ++items;
        }
        // Reload after native scanner changed the stack, then exercise addon use.
        bag=com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,bagStack);
        bag.inventory.func_70299_a(0,new ItemStack(Backport.REGISTERED.get("tera_orb").get()));
        for(int i=0;i<Tera.TYPES.length;i++){
            Pokemon p=mon(143);Tera.changeType(p,Tera.TYPES[(i+1)%Tera.TYPES.length]);StorageProxy.getParty(player).set(0,p);
            ItemStack shard=new ItemStack(Backport.REGISTERED.get(TeraItems.NAMES[i]+"_tera_shard").get(),49);
            bag.inventory.func_70299_a(1,shard);
            require(!TeraItems.change(player,p,i),"Bag accepted 49 shards "+i);
            shard.func_190920_e(52);bag.inventory.func_70296_d();
            require(TeraItems.hasOrb(player) && TeraItems.change(player,p,i),"Bag Tera change rejected "+i);
            bag=com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,bagStack);
            require(bag.inventory.func_70301_a(1).func_190916_E()==2 && Tera.type(p)==Tera.TYPES[i],"Bag shard count/type not persisted "+i);
            ItemStack saved=ItemStack.func_199557_a(bagStack.func_77955_b(new CompoundNBT()));
            require(com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,saved).inventory.func_70301_a(1).func_190916_E()==2,"Bag NBT lost "+i);
        }
        Pokemon p=mon(143);StorageProxy.getParty(player).set(0,p);
        PlayerParticipant owner=new PlayerParticipant(player,p);BattleController bc=new BattleController(new BattleParticipant[]{owner},new BattleParticipant[]{new WildPixelmonParticipant(mon(6))},new BattleRules());bc.sendMessages=false;bc.modifyStats();
        require(Tera.select(owner.allPokemon[0]) && owner.allPokemon[0].megaEvolve(),"Bag orb activation");
        require(!TeraItems.hasOrb(player) && TeraItems.hasAnyOrb(player),"Bag orb charge not consumed");Tera.end(bc);
        bag=com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,bagStack);
        container=new com.pixelmonmod.pixelmon.container.PokeBagContainer(42,player.field_71071_by,bag);
        net.minecraft.inventory.container.Container previous=player.field_71070_bA;player.field_71070_bA=container;
        try{
            new TeraItems.Charging().healer(new HealerEvent.Post(player,"backport-test"));
            require(TeraItems.charged(bag.inventory.func_70301_a(0)),"Open bag healer GUI stale");
            require(TeraItems.hasOrb(player),"Bag healer recharge missing");
            bag.inventory.func_70296_d();require(TeraItems.charged(com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(player,bagStack).inventory.func_70301_a(0)),"Open bag overwrote recharge");
        }finally{player.field_71070_bA=previous;}
        player.field_71071_by.field_70462_a.set(0,ItemStack.field_190927_a);player.field_71071_by.field_184439_c.set(0,bagStack);
        require(TeraItems.hasOrb(player),"Offhand bag orb missing");
        System.out.println("BACKPORT_POKE_BAG_PASS native slot/scanner/consumption items="+items+" shard types=19 counts/NBT, bag orb battle/charge, open GUI healer persistence, offhand bag");
    }
}
