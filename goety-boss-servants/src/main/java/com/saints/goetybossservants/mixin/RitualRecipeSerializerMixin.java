package com.saints.goetybossservants.mixin;

import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Goety's ritual recipe serializer rejects recipes with an empty "ingredients" array ("No
 * ingredients for shapeless recipe"). The boss-soul revive rituals use no pedestal items, just the
 * soul as the activation item, so skip that check (the rest of Goety handles an empty list fine).
 */
@Mixin(targets = "com.Polarice3.Goety.common.crafting.RitualRecipe$Serializer")
public abstract class RitualRecipeSerializerMixin {
    @Redirect(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lcom/Polarice3/Goety/common/crafting/RitualRecipe;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;isEmpty()Z"))
    private boolean goetybossservants$allowNoIngredients(NonNullList<?> ingredients) {
        return false;
    }
}
