package com.saints.goetybossservants.client;

import com.Polarice3.Goety.client.render.ScatterBombRenderer;
import com.Polarice3.Goety.common.entities.projectiles.ScatterBomb;
import com.saints.goetybossservants.GoetyBossServants;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Goety's scatter bomb, glowing sculk teal (texture from tools/MonstrositySkins.java), for the Sculk Monstrosity's sculk globs. */
public class SculkGlobRenderer extends ScatterBombRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation(GoetyBossServants.MOD_ID, "textures/entity/monstrosity/sculk/sculk_glob.png");

    public SculkGlobRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ScatterBomb glob) {
        return TEXTURE;
    }
}
