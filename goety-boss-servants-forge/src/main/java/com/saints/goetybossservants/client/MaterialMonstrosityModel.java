package com.saints.goetybossservants.client;

import com.Polarice3.Goety.client.render.model.RedstoneMonstrosityModel;
import com.saints.goetybossservants.entity.monstrosity.MaterialMonstrosity;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.NotNull;

/**
 * Goety's Redstone Monstrosity model and animations (plus the Sculk one's tentacle slam, see
 * SculkAnimations), reshaped after every pose for the materials built differently (see
 * MonstrosityExtras.reshape): the Netherite one's horns, and the Sculk one's crooked head, missing
 * jaw and tentacle arm.
 */
public class MaterialMonstrosityModel extends RedstoneMonstrosityModel<MaterialMonstrosity> {
    /** How the Sculk one's tentacle is bent for its slam in the pose just set up (see SculkAnimations.tentacleCurl). */
    float tentacleCurl;

    public MaterialMonstrosityModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(@NotNull MaterialMonstrosity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        this.tentacleCurl = 0.0f;
        if (entity.material() == MonstrosityMaterial.SCULK) {
            this.animate(entity.tentacleSlamState, SculkAnimations.TENTACLE_SLAM, ageInTicks);
            if (entity.tentacleSlamState.isStarted()) {
                this.tentacleCurl = SculkAnimations.tentacleCurl(entity.tentacleSlamState.getAccumulatedTime() / 1000.0f);
            }
        }
        MonstrosityExtras.reshape(this.root(), false, entity.material());
    }
}
