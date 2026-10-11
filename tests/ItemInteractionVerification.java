import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.*;
import com.pixelmonmod.pixelmon.items.*;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import com.pixelmonmod.pixelmon.items.heldItems.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.links.DelegateLink;
import com.pixelmonmod.pixelmon.api.interactions.IInteraction;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import local.pixelmon.backport.BackportTechnicalMoves;
import local.pixelmon.backport.Content;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.nio.file.*;

/** Executes native right-click paths with a Forge FakePlayer in an isolated world.
 * FakePlayer discards UI packets; interactive screens still need a client test. */
public final class ItemInteractionVerification {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run(MinecraftServer server)throws Exception {
        ServerWorld world=server.func_71218_a(World.field_234918_g_);
        UUID uuid=UUID.fromString("4a8ccb82-e786-44f1-8e0e-d1ca7a756361");
        FakePlayer player=FakePlayerFactory.get(world,new GameProfile(uuid,"BackportTest"));
        Map<UUID,ServerPlayerEntity> players=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,server.func_184103_al(),"field_177454_f");
        require(!players.containsKey(uuid),"Test UUID in use");players.put(uuid,player);world.func_217369_A().add(player);
        int calls=0,equipment=0;JsonArray inventory=new JsonArray();
        try {
            for(Item item:ForgeRegistries.ITEMS) {
                String id=item.getRegistryName().toString();
                if(!id.startsWith("pixelmon:") && !id.startsWith(Content.NS+":"))continue;
                IInteraction interaction=item instanceof AbilityCapsuleItem?new InteractionAbilityCapsule():
                    item instanceof AbilityPatchItem?new InteractionAbilityPatch():
                    item instanceof MedicineItem?new InteractionPotion():
                    item instanceof IncreaseEVItem?new InteractionVitamins():
                    item instanceof MintItem?new InteractionMint():
                    item instanceof ExpCandyItem?new InteractionRareCandy():
                    item instanceof DynamaxCandyItem?new InteractionDynamaxCandy():
                    item instanceof JuiceShoppeItem?new InteractionBerryJuice():
                    item instanceof CurryDishItem?new InteractionCurry():
                    item instanceof BottlecapItem?new InteractionBottleCap():
                    item instanceof PPUpItem?new InteractionPPUp():
                    item instanceof EtherItem?new InteractionEther():
                    item instanceof ElixirItem?new InteractionElixir():
                    item instanceof MaxSoupItem?new InteractionMaxSoup():
                    item instanceof HeldItem && !(item instanceof OriginFormItem)?new InteractionHeldItem():null;
                JsonObject row=new JsonObject();row.addProperty("id",id);row.addProperty("class",item.getClass().getName());
                row.addProperty("rightClickHandler",interaction==null?"requires separate audit":interaction.getClass().getSimpleName());inventory.add(row);
                if(interaction==null && !(item instanceof HeldItem))continue;
                for(JsonElement patch:Content.DATA.getAsJsonArray("patches")) {
                    JsonObject speciesJson=patch.getAsJsonObject().getAsJsonObject("species");
                    int dex=speciesJson.get("dex").getAsInt();Species species=PixelmonSpecies.fromNationalDex(dex);
                    for(JsonElement formJson:speciesJson.getAsJsonArray("forms")) {
                        String formName=formJson.getAsJsonObject().get("name").getAsString();
                        // Temporary battle forms are checked via held items/battles,
                        // not fed candy or taught moves while outside a battle.
                        if(species.getForm(formName).isTemporary())continue;
                        Pokemon pokemon=PokemonFactory.create(species);pokemon.setForm(formName);pokemon.setLevelNum(100);pokemon.initialize();
                        PlayerPartyStorage party=new PlayerPartyStorage(uuid,false);
                        party.set(0,pokemon);
                        PixelmonEntity entity=pokemon.getOrCreatePixelmon(world,0,5,0);
                        require(entity.getOwner()==player,"Test ownership "+dex);
                        ItemStack stack=new ItemStack(item,2);player.func_184611_a(Hand.MAIN_HAND,stack);
                        String label=id+" / "+dex+" / "+formName;
                        if(item instanceof HeldItem) {
                            pokemon.setHeldItem(stack);
                            Pokemon saved=PokemonFactory.create(pokemon.writeToNBT(new CompoundNBT()));
                            require(saved.getHeldItem().func_77973_b()==item,"Held persistence "+label);
                            ++equipment;
                        }
                        if(interaction==null)continue;
                        // Exercise actual feeding/equipping paths, separately from
                        // the serialization test above. Several berries are eaten
                        // directly instead of becoming a held item.
                        pokemon.setHeldItem(ItemStack.field_190927_a);
                        if(item instanceof MedicineItem || item instanceof BerryRestoreHPItem) {
                            pokemon.setHealth(item instanceof ReviveItem?0:1);
                            pokemon.setStatus(new Poison());
                            for(Attack attack:pokemon.getMoveset())attack.pp=0;
                        }
                        if(item instanceof EtherItem || item instanceof ElixirItem || item instanceof BerryLeppaItem)
                            for(Attack attack:pokemon.getMoveset())attack.pp=0;
                        if(item instanceof DecreaseEVBerryItem) {
                            pokemon.getEVs().fillFromArray(new int[]{40,40,40,40,40,40});pokemon.setFriendship(0);
                        }
                        if(item instanceof BerryStatusItem) {
                            BerryStatusItem berry=(BerryStatusItem)item;
                            if(berry.canHealStatus(StatusType.Poison))pokemon.setStatus(new Poison());
                            else if(berry.canHealStatus(StatusType.Burn))pokemon.setStatus(new Burn());
                            else if(berry.canHealStatus(StatusType.Paralysis))pokemon.setStatus(new Paralysis());
                            else if(berry.canHealStatus(StatusType.Sleep))pokemon.setStatus(new Sleep());
                            else if(berry.canHealStatus(StatusType.Freeze))pokemon.setStatus(new Freeze());
                        }
                        if(item instanceof IncreaseEVItem || item instanceof JuiceShoppeItem)pokemon.getEVs().fillFromArray(new int[]{0,0,0,0,0,0});
                        if(item instanceof ExpCandyItem)pokemon.setLevelNum(50);
                        if(item instanceof DynamaxCandyItem)pokemon.setDynamaxLevel(0);
                        if(item instanceof MintItem && pokemon.getBaseNature()==((MintItem)item).getNature())pokemon.setNature(Nature.getRandomNature());
                        if(item instanceof AbilityCapsuleItem || item instanceof AbilityPatchItem) {
                            pokemon.setAbility(pokemon.getForm().getAbilities().getAbilities()[0]);pokemon.overrideAbilitySlot();
                        }
                        String beforeAbility=pokemon.getAbilityName();
                        int beforeExp=pokemon.getExperience();
                        require(interaction.processInteract(entity,player,Hand.MAIN_HAND,stack),"Native interaction unhandled "+label);
                        if(item instanceof AbilityCapsuleItem && pokemon.getForm().getAbilities().getAbilities().length>1)
                            require(!beforeAbility.equals(pokemon.getAbilityName()),"Capsule no change "+label);
                        if(item instanceof AbilityPatchItem && pokemon.getForm().getAbilities().getHiddenAbilities().length>0)
                            require(pokemon.hasHiddenAbility(),"Patch did not set hidden "+label);
                        if(item instanceof MintItem)require(pokemon.getMintNature()==((MintItem)item).getNature() || pokemon.getBaseNature()==((MintItem)item).getNature(),"Mint no change "+label);
                        if(item instanceof IncreaseEVItem)require(pokemon.getEVs().getStat(((IncreaseEVItem)item).type)>0,"Vitamin no change "+label);
                        if(item instanceof ExpCandyItem)require(pokemon.getPokemonLevel()>50 || pokemon.getExperience()>beforeExp,"Candy no change "+label);
                        if(item instanceof DynamaxCandyItem)require(pokemon.getDynamaxLevel()==(pokemon.getForm().hasTag("cannot_dynamax")?0:1),"Dynamax candy wrong result "+label);
                        if(item instanceof ReviveItem)require(pokemon.getHealth()>0,"Revive no change "+label);
                        if(item instanceof BerryRestoreHPItem)require(pokemon.getHealth()>1,"HP berry no change "+label);
                        if(item instanceof DecreaseEVBerryItem)require(pokemon.getEVs().getStat(((DecreaseEVBerryItem)item).type)<40,"EV berry no change "+label);
                        if(item instanceof ElixirItem)require(pokemon.getMoveset().get(0).pp>0,"Elixir no change "+label);
                        if(item instanceof MaxSoupItem)require(pokemon.hasGigantamaxFactor()==pokemon.getForm().getGigantamax().canHaveFactor(),"Max soup eligibility "+label);
                        if(item instanceof HeldItem) {
                            if(!(item instanceof BerryLeppaItem))require(stack.func_190916_E()<2,"Held interaction did not consume/equip "+label);
                            Pokemon saved=PokemonFactory.create(pokemon.writeToNBT(new CompoundNBT()));
                            require(saved.getHeldItem().func_77973_b()==pokemon.getHeldItem().func_77973_b(),"Equipped item lost on save "+label);
                        }
                        ++calls;
                    }
                }
            }
            int selectedMoveCases=verifySelectedMoveItems(player);
            int machineCases=0;
            for(BackportTechnicalMoves move:BackportTechnicalMoves.values()) {
                require(move.getAttack()!=null,"Missing TM attack "+move);
                ItemStack stack=PixelmonItems.createTMStackFor(move,2);
                require(stack!=null && stack.func_190916_E()==2,"Native TM factory "+move);
                require(TechnicalMoveItem.getMove(stack)==move,"TM NBT resolution "+move);
                require(TechnicalMoveItem.getMove(ItemStack.func_199557_a(stack.serializeNBT()))==move,"TM item save "+move);
                for(int dex=1011;dex<=1025;dex++) {
                    Species species=PixelmonSpecies.fromNationalDex(dex);
                    for(Stats form:species.getForms()) {
                        boolean listed=form.getMoves().getTMMoves().contains(move.getAttack());
                        if(!listed)continue;
                        require(form.getMoves().canLearn(move),"TM eligibility "+dex+" "+form.getName()+" "+move);
                        Pokemon pokemon=PokemonFactory.create(species);pokemon.setForm(form.getName());pokemon.getMoveset().clear();
                        pokemon.setStorage(new PlayerPartyStorage(uuid,false),new StoragePosition(-1,0));
                        PixelmonEntity entity=pokemon.getOrCreatePixelmon(world,0,5,0);
                        ItemStack machine=TechnicalMoveItem.of(TMType.TM9,move);player.func_184611_a(Hand.MAIN_HAND,machine);
                        require(new InteractionTechnicalMove().processInteract(entity,player,Hand.MAIN_HAND,machine),"TM interaction "+move);
                        require(pokemon.getMoveset().hasAttack(move.getAttack()),"TM did not teach "+move);
                        ++machineCases;
                    }
                }
            }
            EvolutionVerification.run(player);
            RepairVerification.run(player);
            local.pixelmon.backport.TeraItemVerification.run(player);
            TeraRaidVerification.playerRaid(player);
            IntegrationVerification.run(player);
            FeedbackVerification.run(player);
            Version12Verification.runReal(server);
            Files.write(Paths.get("item-interaction-inventory.json"),new GsonBuilder().setPrettyPrinting().create().toJson(inventory).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("BACKPORT_ITEMS_PASS native right-click cases="+calls+" held persistence cases="+equipment+" new TM learning cases="+machineCases+" selected move/party item cases="+selectedMoveCases+" catalog="+inventory.size());
        } finally {players.remove(uuid);world.func_217369_A().remove(player);}
    }
    static int verifySelectedMoveItems(FakePlayer player) {
        PlayerPartyStorage party=StorageProxy.getParty(player);int cases=0;
        try {
            for(JsonElement patch:Content.DATA.getAsJsonArray("patches")) {
                int dex=patch.getAsJsonObject().getAsJsonObject("species").get("dex").getAsInt();
                for(Item item:ForgeRegistries.ITEMS) {
                    if(!(item instanceof PPUpItem || item instanceof EtherItem || item instanceof SacredAshItem))continue;
                    Pokemon pokemon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));
                    pokemon.getMoveset().clear();pokemon.getMoveset().add(new Attack("Tackle"));party.set(0,pokemon);
                    ItemStack stack=new ItemStack(item,2);player.func_184611_a(Hand.MAIN_HAND,stack);
                    Attack attack=pokemon.getMoveset().get(0);String label=item.getRegistryName()+" / "+dex;
                    if(item instanceof PPUpItem) {
                        int max=attack.getMaxPP();PPUpItem.handleMoveSelect(player,pokemon.getUUID(),0);
                        require(attack.getMaxPP()>max && stack.func_190916_E()==1,"PP item selection "+label);
                        attack.ppLevel=3;int count=stack.func_190916_E();
                        PPUpItem.handleMoveSelect(player,pokemon.getUUID(),0);
                        require(stack.func_190916_E()==count,"Max PP item consumed on rejection "+label);
                    } else if(item instanceof EtherItem) {
                        attack.pp=0;require(((EtherItem)item).useEther(new DelegateLink(pokemon),0) && attack.pp>0,"Ether selection "+label);
                        attack.pp=attack.getMaxPP();require(!((EtherItem)item).useEther(new DelegateLink(pokemon),0),"Full PP ether accepted "+label);
                    } else {
                        pokemon.setHealth(0);((SacredAshItem)item).func_77659_a(player.field_70170_p,player,Hand.MAIN_HAND);
                        require(pokemon.getHealth()==pokemon.getMaxHealth() && stack.func_190916_E()==1,"Sacred ash party revive "+label);
                    }
                    ++cases;
                }
            }
        } finally {party.set(0,null);}
        return cases;
    }
}
