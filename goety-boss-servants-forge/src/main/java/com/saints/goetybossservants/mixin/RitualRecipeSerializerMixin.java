package com.saints.goetybossservants.mixin;

import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Goety refuses to load a ritual with no pedestal ingredients ("No ingredients for shapeless
 * recipe"). The boss-soul revive rituals have none (the soul is the activation item), so skip that
 * check, as the Fabric version does. Rituals that list ingredients are unaffected.
 *
 * <p>The full descriptor picks Goety's own fromJson (named the same in dev and in the real game)
 * over its bridge method; NonNullList.isEmpty is plain Java, so no refmap is needed.
 */
@Mixin(targets = "com.Polarice3.Goety.common.crafting.RitualRecipe$Serializer", remap = false)
public abstract class RitualRecipeSerializerMixin {
    @Redirect(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lcom/Polarice3/Goety/common/crafting/RitualRecipe;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;isEmpty()Z"), require = 1, allow = 1)
    private boolean goetybossservants$allowNoIngredients(NonNullList<?> ingredients) {
        return false;
    }
}
