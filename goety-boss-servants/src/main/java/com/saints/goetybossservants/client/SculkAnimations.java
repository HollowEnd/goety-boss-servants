package com.saints.goetybossservants.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.util.Mth;

/**
 * The Sculk Monstrosity's tentacle slam, on Goety's Redstone Monstrosity bones: it leans back,
 * raising only its tentacle arm high over its shoulder (the tentacle curling back behind it), then
 * leans forward and whips it down onto the ground ahead (MaterialMonstrosity.TENTACLE_IMPACT, 1.1
 * seconds in), holds it there a moment, and straightens up again. The tentacle's own bend is
 * tentacleCurl (its segments aren't Goety's bones, see MonstrosityExtras).
 */
public final class SculkAnimations {
    private static final float RAISED = 0.6f;
    private static final float HELD = 0.9f;
    private static final float LANDED = 1.1f;
    private static final float LIFTED = 1.5f;
    private static final float LENGTH = 2.25f;

    public static final AnimationDefinition TENTACLE_SLAM = AnimationDefinition.Builder.withLength(LENGTH)
            .addAnimation("upper", rotation(new float[][]{
                    {0.0f, 0.0f, 0.0f, 0.0f}, {RAISED, -15.0f, 10.0f, 5.0f}, {HELD, -20.0f, 12.5f, 5.0f},
                    {LANDED, 25.0f, -10.0f, -5.0f}, {LIFTED, 25.0f, -10.0f, -5.0f}, {LENGTH, 0.0f, 0.0f, 0.0f}}, LANDED))
            .addAnimation("head", rotation(new float[][]{
                    {0.0f, 0.0f, 0.0f, 0.0f}, {RAISED, -10.0f, 0.0f, 0.0f}, {HELD, -12.5f, 0.0f, 0.0f},
                    {LANDED, 15.0f, 0.0f, 0.0f}, {LIFTED, 15.0f, 0.0f, 0.0f}, {LENGTH, 0.0f, 0.0f, 0.0f}}, LANDED))
            .addAnimation("right_arm", rotation(new float[][]{
                    {0.0f, 0.0f, 0.0f, 0.0f}, {RAISED, -165.0f, 0.0f, 10.0f}, {HELD, -172.5f, 0.0f, 10.0f},
                    {LANDED, -45.0f, 0.0f, 0.0f}, {LIFTED, -45.0f, 0.0f, 0.0f}, {LENGTH, 0.0f, 0.0f, 0.0f}}, LANDED))
            .addAnimation("left_arm", rotation(new float[][]{
                    {0.0f, 0.0f, 0.0f, 0.0f}, {RAISED, -20.0f, 0.0f, -15.0f}, {HELD, -22.5f, 0.0f, -15.0f},
                    {LANDED, 15.0f, 0.0f, -10.0f}, {LIFTED, 15.0f, 0.0f, -10.0f}, {LENGTH, 0.0f, 0.0f, 0.0f}}, LANDED))
            .build();

    private SculkAnimations() {
    }

    // Rotation keyframes {seconds, x, y, z degrees}, smooth except for the swing down to the one at
    // {@code snap}, which is straight (it whips down).
    private static AnimationChannel rotation(float[][] frames, float snap) {
        Keyframe[] keyframes = new Keyframe[frames.length];
        for (int i = 0; i < frames.length; ++i) {
            float[] f = frames[i];
            keyframes[i] = new Keyframe(f[0], KeyframeAnimations.degreeVec(f[1], f[2], f[3]),
                    f[0] == snap ? AnimationChannel.Interpolations.LINEAR : AnimationChannel.Interpolations.CATMULLROM);
        }
        return new AnimationChannel(AnimationChannel.Targets.ROTATION, keyframes);
    }

    /**
     * How the tentacle is bent {@code seconds} into the slam: -1 curled back behind it as it's
     * raised, 0 as it hangs at rest, 1 thrown out flat along the ground.
     */
    public static float tentacleCurl(float seconds) {
        if (seconds < RAISED) {
            return -Mth.sin(seconds / RAISED * Mth.HALF_PI);
        }
        if (seconds < HELD) {
            return -1.0f;
        }
        if (seconds < LANDED) {
            float swing = (seconds - HELD) / (LANDED - HELD);
            return -1.0f + 2.0f * swing * swing;
        }
        if (seconds < LIFTED) {
            return 1.0f;
        }
        if (seconds < LENGTH) {
            return Mth.cos((seconds - LIFTED) / (LENGTH - LIFTED) * Mth.HALF_PI);
        }
        return 0.0f;
    }
}
