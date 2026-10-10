package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;

/** The config plugin rewrites only weather consumers with a native wrapper
 * argument. No thread-global weather override or extra copies of abilities. */
@Mixin(targets={
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Chlorophyll",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.SwiftSwim",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.SolarPower",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.DrySkin",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.RainDish",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Hydration",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.LeafGuard",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Forecast",
"com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.FlowerGift",
"com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.multiTurn.SolarBeam",
"com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.WeatherBall",
"com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.Synthesis",
"com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.Growth",
"com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.HydroSteam",
"com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.AlwaysHit",
"com.pixelmonmod.pixelmon.battles.status.Freeze"
},remap=false)
public abstract class UmbrellaConsumerMixin {}
