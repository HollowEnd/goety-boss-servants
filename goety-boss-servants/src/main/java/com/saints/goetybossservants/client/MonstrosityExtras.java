package com.saints.goetybossservants.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Extra model pieces that set the material monstrosities apart, added on top of Goety's Redstone
 * Monstrosity model: lightning rods (Copper), vines and a poppy (Iron), a crown, cape, ermine
 * mantle and sceptre (Gold), candles, its book floating over its back pack (drawn as an enchanting table) and orbiting runes (Lapis),
 * crystal clusters (Diamond), warlord horns (Netherite), a tentacle arm, a bone horn, dripping sculk,
 * bone prongs and Warden tendrils (Sculk).
 *
 * <p>Each piece hangs off one of Goety's bones ("top" of the head, "body", the shoulders, the right
 * hand, or "upper" for the runes) and follows its animation. They use the material's 256x256
 * parts texture (see tools/MonstrositySkins.java): the material block everywhere, a secondary
 * surface at (0,64), glow colours at (64,64) and (96,64), see-through art at (64,96), and
 * per-material pieces from (128,0) and (0,128).
 */
public final class MonstrosityExtras {
    /** Goety's bones, as paths from the model root. */
    private static final Map<String, List<String>> BONES = Map.of(
            "top", List.of("monstrosity", "upper", "head", "top"),
            "body", List.of("monstrosity", "upper", "body"),
            "upper", List.of("monstrosity", "upper"),
            "right_shoulder", List.of("monstrosity", "upper", "right_arm", "right_shoulder"),
            "right_middle", List.of("monstrosity", "upper", "right_arm", "right_shoulder", "right_middle"),
            "left_shoulder", List.of("monstrosity", "upper", "left_arm", "left_shoulder"),
            "right_hand", List.of("monstrosity", "upper", "right_arm", "right_shoulder", "right_middle", "right_hand"));
    private static final float RUNE_SCALE = 3.0f;
    // The Lapis runes float in a ring above its shoulders (`upper` space; the shoulders top out near -80).
    private static final float RING_Y = -95.0f;
    private static final float RING_RADIUS = 58.0f;
    // The Lapis one's back pack is its enchanting table (see its skin): the top, in body space.
    private static final float TABLE_Y = -51.0f;
    private static final float TABLE_Z = 21.0f;
    // The Gold sceptre runs up through its fist (pommel showing below the fingers) and leans back
    // against its shoulder, like a staff held at rest. Goety's hands are tilted, so this is less
    // than it looks.
    private static final float SCEPTRE_TILT = 0.9f;
    private static final Map<MonstrosityMaterial, Pieces> PIECES = new EnumMap<>(MonstrosityMaterial.class);
    private static BookModel book;
    private static ModelPart eyes;

    /** Solid pieces, glowing pieces, see-through pieces (cutout) and glowing see-through runes. */
    record Pieces(ModelPart solid, ModelPart glow, ModelPart cutout, ModelPart runes) {
    }

    private MonstrosityExtras() {
    }

    static Pieces pieces(MonstrosityMaterial material) {
        return PIECES.computeIfAbsent(material, key -> new Pieces(bake(key, Kind.SOLID), bake(key, Kind.GLOW), bake(key, Kind.CUTOUT), bake(key, Kind.RUNES)));
    }

    enum Kind {
        SOLID, GLOW, CUTOUT, RUNES
    }

    /**
     * Renders the pieces on a monstrosity model whose bones are already posed. {@code root} is the
     * model's root part; {@code onlyHead} limits it to the head's pieces (for the head block,
     * whose root holds the head directly). {@code hasBook}: the Lapis one's enchanting table (its
     * back pack) has an enchanted book floating over it. {@code limbSwing} and {@code limbSwingAmount}
     * are its walk (the Sculk one's tentacle drags along behind it, and its jaw strands swing);
     * {@code tentacleCurl} is how its tentacle is bent for its slam (see SculkAnimations.tentacleCurl).
     */
    static void render(MonstrosityMaterial material, ModelPart root, boolean onlyHead, boolean hasBook, float ageInTicks, float limbSwing,
                       float limbSwingAmount, float tentacleCurl, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Pieces pieces = pieces(material);
        if (material == MonstrosityMaterial.SCULK) {
            if (!onlyHead) {
                swayTentacle(pieces.solid(), ageInTicks, limbSwing, limbSwingAmount, tentacleCurl);
                swayTentacle(pieces.cutout(), ageInTicks, limbSwing, limbSwingAmount, tentacleCurl);
            }
            swayStrands(pieces.solid(), onlyHead, ageInTicks, limbSwing, limbSwingAmount);
            swayStrands(pieces.glow(), onlyHead, ageInTicks, limbSwing, limbSwingAmount);
        }
        ResourceLocation texture = MaterialMonstrosityRenderer.texture(material, "parts");
        if (pieces.runes().hasChild("upper")) {
            ModelPart ring = pieces.runes().getChild("upper").getChild("ring");
            ring.yRot = ageInTicks * 0.03f;
            ring.y = RING_Y + (float) Math.sin(ageInTicks * 0.08) * 3.0f;
        }
        if (material == MonstrosityMaterial.GOLD && !onlyHead) {
            // The cape hangs down whichever way the body leans (only partly when it leans far, as when
            // sitting, so it stays clear of the legs) and sways a little.
            ModelPart monstrosity = root.getChild("monstrosity");
            ModelPart upper = monstrosity.getChild("upper");
            float lean = monstrosity.xRot + upper.xRot + upper.getChild("body").xRot;
            pieces.solid().getChild("body").getChild("cape").xRot = Math.max(0.08f - lean, -0.15f) + (float) Math.sin(ageInTicks * 0.07) * 0.04f;
        }
        draw(pieces.solid(), root, onlyHead, poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        // The Sculk one's see-through planes (its Warden tendrils and drips) show each side's own face
        // only: drawn from both sides at once, their two faces fight.
        RenderType cutout = material == MonstrosityMaterial.SCULK ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
        draw(pieces.cutout(), root, onlyHead, poseStack, buffers.getBuffer(cutout), light, overlay);
        draw(pieces.glow(), root, onlyHead, poseStack, buffers.getBuffer(RenderType.eyes(texture)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        draw(pieces.runes(), root, onlyHead, poseStack, buffers.getBuffer(RenderType.entityTranslucentEmissive(texture)), 0xF000F0, OverlayTexture.NO_OVERLAY);
        if (material == MonstrosityMaterial.LAPIS && hasBook && !onlyHead) {
            renderBook(root, ageInTicks, poseStack, buffers, light, overlay);
        }
        if (material == MonstrosityMaterial.SCULK && !onlyHead) {
            renderSensorTendrils(root, poseStack, buffers, light, overlay);
        }
    }

    // Where the tendrils of sculk sensors grow out of the top of the Sculk one's back (y -57, in body
    // space): a sensor's tendrils start 8 pixels up its block, so the block sits that far down.
    private static final float[][] SENSORS = {{-18.0f, -49.0f, 3.0f}, {17.0f, -49.0f, 6.0f}, {0.0f, -49.0f, 0.0f}};
    private static BlockState sensor;

    /**
     * The tendrils of active sculk sensors on the Sculk one's back: drawn from the game's own block
     * model (only its quads textured with a tendril sprite), so they animate as a sensor's do.
     */
    private static void renderSensorTendrils(ModelPart root, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (sensor == null) {
            sensor = Blocks.SCULK_SENSOR.defaultBlockState().setValue(SculkSensorBlock.PHASE, SculkSensorPhase.ACTIVE);
        }
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(sensor);
        VertexConsumer buffer = buffers.getBuffer(Sheets.cutoutBlockSheet());
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> quads = new java.util.ArrayList<>(model.getQuads(sensor, null, random));
        for (Direction side : Direction.values()) {
            quads.addAll(model.getQuads(sensor, side, random));
        }
        quads.removeIf(quad -> !quad.getSprite().contents().name().getPath().contains("tendril"));
        for (float[] place : SENSORS) {
            poseStack.pushPose();
            toBone(root, "body", false, poseStack);
            poseStack.translate(place[0] / 16.0f, place[1] / 16.0f, place[2] / 16.0f);
            // Model space is upside down.
            poseStack.scale(-1.0f, -1.0f, 1.0f);
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            for (BakedQuad quad : quads) {
                buffer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, light, overlay);
            }
            poseStack.popPose();
        }
    }

    private static void draw(ModelPart group, ModelPart root, boolean onlyHead, PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
        for (Map.Entry<String, List<String>> bone : BONES.entrySet()) {
            if (!group.hasChild(bone.getKey()) || onlyHead && !bone.getKey().equals("top")) {
                continue;
            }
            poseStack.pushPose();
            toBone(root, bone.getKey(), onlyHead, poseStack);
            group.getChild(bone.getKey()).render(poseStack, buffer, light, overlay);
            poseStack.popPose();
        }
    }

    // Moves the pose into a bone's space.
    private static void toBone(ModelPart root, String bone, boolean onlyHead, PoseStack poseStack) {
        if (onlyHead) {
            // The head block's model: root > head > top.
            root.getChild("head").translateAndRotate(poseStack);
            root.getChild("head").getChild("top").translateAndRotate(poseStack);
            return;
        }
        ModelPart part = root;
        part.translateAndRotate(poseStack);
        for (String name : BONES.get(bone)) {
            part = part.getChild(name);
            part.translateAndRotate(poseStack);
        }
    }

    /** The held enchanted book, floating open over the enchanting table and slowly turning, like a vanilla table's. */
    private static void renderBook(ModelPart root, float ageInTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (book == null) {
            book = new BookModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BOOK));
        }
        poseStack.pushPose();
        toBone(root, "body", false, poseStack);
        poseStack.translate(0.0f, (TABLE_Y - 7.0f - (float) Math.sin(ageInTicks * 0.1) * 1.5f) / 16.0f, TABLE_Z / 16.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * 1.5f));
        // Up is -y in model space, so the book tips the other way from a vanilla table's.
        poseStack.mulPose(Axis.ZP.rotationDegrees(-80.0f));
        float flip = (float) Math.sin(ageInTicks * 0.05) * 0.08f;
        book.setupAnim(ageInTicks, 0.1f + flip, 0.9f - flip, 1.0f);
        book.render(poseStack, EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entitySolid, true), light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    // ----- eyes --------------------------------------------------------------------------------

    /**
     * Goety's eyes with the middle one centred (Goety's sits 1 pixel right of the head's centre
     * line) and the side ones even (its right one pokes a quarter pixel past the head). Same
     * texture spots as Goety's, so every layer textures them as before. Each eye is its own part:
     * the Sculk one has lost its right one.
     */
    private static ModelPart eyes() {
        if (eyes == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition all = mesh.getRoot().addOrReplaceChild("eyes", CubeListBuilder.create(), PartPose.ZERO);
            all.addOrReplaceChild("right", box(0, 14, -14.0f, -6.0f, -20.5f, 4, 4, 0), PartPose.ZERO);
            all.addOrReplaceChild("left", box(0, 14, 10.0f, -6.0f, -20.5f, 4, 4, 0), PartPose.ZERO);
            all.addOrReplaceChild("middle", box(0, 18, -3.0f, -9.0f, -20.5f, 6, 3, 0), PartPose.ZERO);
            eyes = LayerDefinition.create(mesh, 512, 256).bakeRoot().getChild("eyes");
        }
        return eyes;
    }

    /** Hides Goety's own eyes on a monstrosity (or head block, {@code onlyHead}) model, so {@link #renderEyes} can draw them. */
    static void hideGoetyEyes(ModelPart root, boolean onlyHead) {
        ModelPart top = onlyHead ? root.getChild("head").getChild("top")
                : root.getChild("monstrosity").getChild("upper").getChild("head").getChild("top");
        top.getChild("eyes").visible = false;
    }

    /**
     * Reshapes a posed monstrosity (or head block, {@code onlyHead}) model for its material, after
     * Goety's animation. The Netherite one's own great horns replace Goety's horn stubs. The Sculk
     * one, a Redstone Monstrosity the sculk killed, has lost its jaw (sculk hangs where it was), its
     * head is knocked crooked, its right horn has become a bone horn (one of its pieces), and its
     * right forearm and hand are gone: a sculk tentacle grows from the elbow instead.
     */
    static void reshape(ModelPart root, boolean onlyHead, MonstrosityMaterial material) {
        ModelPart head = onlyHead ? root.getChild("head") : root.getChild("monstrosity").getChild("upper").getChild("head");
        ModelPart top = head.getChild("top");
        boolean horns = material != MonstrosityMaterial.NETHERITE;
        boolean sculk = material == MonstrosityMaterial.SCULK;
        top.getChild("right_horn").visible = horns && !sculk;
        top.getChild("left_horn").visible = horns;
        head.getChild("bottom").visible = !sculk;
        if (onlyHead) {
            return;
        }
        root.getChild("monstrosity").getChild("upper").getChild("right_arm").getChild("right_shoulder").getChild("right_middle").visible = !sculk;
        if (sculk) {
            head.x += 5.0f;
            head.y -= 2.0f;
            head.xRot += 0.12f;
            head.zRot += 0.3f;
        }
    }

    /** Draws the centred eyes on a posed model, in whatever layer is being drawn. */
    static void renderEyes(MonstrosityMaterial material, ModelPart root, boolean onlyHead, PoseStack poseStack, VertexConsumer buffer, int light,
                           int overlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        toBone(root, "top", onlyHead, poseStack);
        eyes().getChild("right").visible = material != MonstrosityMaterial.SCULK;
        eyes().render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    // ----- the pieces --------------------------------------------------------------------------

    private static ModelPart bake(MonstrosityMaterial material, Kind kind) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        switch (material) {
            case COPPER -> copper(root, kind);
            case IRON -> iron(root, kind);
            case GOLD -> gold(root, kind);
            case LAPIS -> lapis(root, kind);
            case DIAMOND -> diamond(root, kind);
            case NETHERITE -> netherite(root, kind);
            case SCULK -> sculk(root, kind);
            default -> {
            }
        }
        ModelPart baked = LayerDefinition.create(mesh, 256, 256).bakeRoot();
        if (material == MonstrosityMaterial.IRON && kind == Kind.CUTOUT) {
            ModelPart poppy = baked.getChild("top").getChild("poppy");
            poppy.xScale = poppy.yScale = poppy.zScale = 2.0f;
        }
        if (kind == Kind.RUNES && baked.hasChild("upper")) {
            baked.getChild("upper").getChild("ring").getAllParts().forEach(part -> {
                part.xScale = RUNE_SCALE;
                part.yScale = RUNE_SCALE;
                part.zScale = RUNE_SCALE;
            });
            ModelPart ring = baked.getChild("upper").getChild("ring");
            ring.xScale = ring.yScale = ring.zScale = 1.0f;
        }
        return baked;
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d);
    }

    private static CubeListBuilder box(int u, int v, float x, float y, float z, float w, float h, float d, float grow) {
        return CubeListBuilder.create().texOffs(u, v).addBox(x, y, z, w, h, d, new CubeDeformation(grow));
    }

    /** Copper: lightning rods on both shoulders and on the head, their knobs glowing like a charged rod. */
    private static void copper(PartDefinition root, Kind kind) {
        PartDefinition right = root.addOrReplaceChild("right_shoulder", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition left = root.addOrReplaceChild("left_shoulder", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition top = root.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.ZERO);
        rod(right, "rod", kind, -29.0f, -12.0f, -1.0f, 18.0f);
        rod(left, "rod", kind, 27.0f, -12.0f, -1.0f, 18.0f);
        rod(top, "rod", kind, -1.0f, -25.0f, -10.5f, 14.0f);
    }

    private static void rod(PartDefinition bone, String name, Kind kind, float x, float base, float z, float length) {
        if (kind == Kind.SOLID) {
            bone.addOrReplaceChild(name, box(0, 64, x, base - length, z, 2, length, 2).texOffs(0, 0).addBox(x - 1.0f, base - length - 4.0f, z - 1.0f, 4, 4, 4), PartPose.ZERO);
        } else if (kind == Kind.GLOW) {
            bone.addOrReplaceChild(name, box(96, 64, x - 1.0f, base - length - 4.0f, z - 1.0f, 4, 4, 4, 0.35f), PartPose.ZERO);
        }
    }

    /** Iron: vines growing over its chest and shoulders, and a poppy on its head, like an iron golem's. */
    private static void iron(PartDefinition root, Kind kind) {
        if (kind == Kind.SOLID) {
            // Bolted iron plates on the shoulders.
            root.addOrReplaceChild("right_shoulder", box(0, 64, -35.0f, -14.0f, -7.0f, 18, 2, 14), PartPose.ZERO);
            root.addOrReplaceChild("left_shoulder", box(0, 64, 17.0f, -14.0f, -7.0f, 18, 2, 14), PartPose.ZERO);
        } else if (kind == Kind.CUTOUT) {
            root.addOrReplaceChild("body", box(64, 96, -30.0f, -57.0f, -14.75f, 16, 32, 0).texOffs(64, 96).addBox(13.0f, -57.0f, -14.75f, 16, 24, 0), PartPose.ZERO);
            root.addOrReplaceChild("right_shoulder", box(64, 96, -36.25f, -12.0f, -8.0f, 0, 16, 16), PartPose.ZERO);
            root.addOrReplaceChild("left_shoulder", box(64, 96, 36.25f, -12.0f, -8.0f, 0, 16, 16), PartPose.ZERO);
            PartDefinition top = root.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition poppy = top.addOrReplaceChild("poppy", box(96, 96, -8.0f, -16.0f, 0.0f, 16, 16, 0), PartPose.offset(7.0f, -25.0f, -10.0f));
            poppy.addOrReplaceChild("cross", box(96, 96, -8.0f, -16.0f, 0.0f, 16, 16, 0), PartPose.rotation(0.0f, (float) Math.PI / 2.0f, 0.0f));
        }
    }

    /**
     * Gold, a monarch: a jewelled crown over a velvet cap, an ermine mantle with a crimson cape
     * hanging from it, and a sceptre in its right hand.
     */
    private static void gold(PartDefinition root, Kind kind) {
        if (kind == Kind.SOLID) {
            CubeListBuilder crown = box(0, 0, -15.0f, -31.0f, -21.0f, 30, 6, 1)
                    .texOffs(0, 8).addBox(-15.0f, -31.0f, 1.0f, 30, 6, 1)
                    .texOffs(0, 16).addBox(14.0f, -31.0f, -20.0f, 1, 6, 21)
                    .texOffs(0, 16).addBox(-15.0f, -31.0f, -20.0f, 1, 6, 21);
            for (float z : new float[]{-21.0f, 1.0f}) {
                crown.texOffs(64, 0).addBox(-14.0f, -36.0f, z, 3, 5, 1)
                        .texOffs(64, 8).addBox(-1.5f, -38.0f, z, 3, 7, 1)
                        .texOffs(64, 0).addBox(11.0f, -36.0f, z, 3, 5, 1);
            }
            crown.texOffs(64, 20).addBox(14.0f, -36.0f, -11.0f, 1, 5, 3).texOffs(64, 20).addBox(-15.0f, -36.0f, -11.0f, 1, 5, 3);
            // The velvet cap inside the crown, and a gold orb on top.
            crown.texOffs(0, 160).addBox(-13.5f, -30.0f, -19.5f, 27, 5, 20)
                    .texOffs(0, 190).addBox(-10.0f, -37.0f, -16.0f, 20, 7, 13)
                    .texOffs(80, 20).addBox(-2.0f, -41.0f, -11.5f, 4, 4, 4);
            root.addOrReplaceChild("top", crown, PartPose.ZERO);
            PartDefinition body = root.addOrReplaceChild("body", box(0, 128, -32.0f, -61.0f, 4.0f, 64, 6, 24), PartPose.ZERO);
            body.addOrReplaceChild("cape", box(128, 0, -30.0f, 0.0f, 0.0f, 60, 80, 1), PartPose.offset(0.0f, -56.0f, 27.5f));
            root.addOrReplaceChild("right_hand", CubeListBuilder.create(), PartPose.ZERO)
                    .addOrReplaceChild("sceptre", box(100, 0, -1.5f, -50.0f, -1.5f, 3, 60, 3)
                                    .texOffs(16, 32).addBox(-2.5f, 10.0f, -2.5f, 5, 4, 5)
                                    .texOffs(16, 44).addBox(-3.5f, -53.0f, -3.5f, 7, 3, 7)
                                    .texOffs(44, 32).addBox(-3.0f, -59.0f, -3.0f, 6, 6, 6)
                                    .texOffs(80, 32).addBox(-1.0f, -66.0f, -1.0f, 2, 7, 2)
                                    .texOffs(80, 44).addBox(-3.0f, -64.0f, -1.0f, 6, 2, 2),
                            PartPose.offsetAndRotation(0.0f, 24.0f, -1.0f, SCEPTRE_TILT, 0.0f, 0.0f));
        } else if (kind == Kind.GLOW) {
            // Rubies on the crown, an emerald on its orb; a band of rubies round the sceptre's orb.
            root.addOrReplaceChild("top", box(64, 64, -1.5f, -30.0f, -21.75f, 3, 3, 1).texOffs(64, 64).addBox(-1.5f, -30.0f, 1.75f, 3, 3, 1)
                    .texOffs(64, 64).addBox(-13.0f, -29.0f, -21.5f, 2, 2, 1).texOffs(64, 64).addBox(11.0f, -29.0f, -21.5f, 2, 2, 1)
                    .texOffs(64, 64).addBox(-1.0f, -36.0f, -21.25f, 2, 2, 1)
                    .texOffs(96, 64).addBox(-1.0f, -40.0f, -12.25f, 2, 2, 1), PartPose.ZERO);
            root.addOrReplaceChild("right_hand", CubeListBuilder.create(), PartPose.ZERO)
                    .addOrReplaceChild("sceptre", box(64, 64, -3.0f, -57.0f, -3.0f, 6, 2, 6, 0.3f)
                                    .texOffs(96, 64).addBox(-1.0f, -64.0f, -1.0f, 2, 2, 2, new CubeDeformation(0.3f)),
                            PartPose.offsetAndRotation(0.0f, 24.0f, -1.0f, SCEPTRE_TILT, 0.0f, 0.0f));
        }
    }

    /** Lapis: candles on its shoulders, its book floating over its back pack (drawn as an enchanting table), and enchanting glyphs circling it. */
    private static void lapis(PartDefinition root, Kind kind) {
        float[][] candles = {{-12.0f, -6.0f, 6.0f}, {-6.0f, 4.0f, 4.0f}, {10.0f, -6.0f, 6.0f}, {4.0f, 4.0f, 4.0f}};
        if (kind == Kind.SOLID) {
            root.addOrReplaceChild("right_shoulder", box(0, 96, candles[0][0], -35.0f - candles[0][2], candles[0][1], 2, candles[0][2], 2)
                    .texOffs(0, 96).addBox(candles[1][0], -35.0f - candles[1][2], candles[1][1], 2, candles[1][2], 2), PartPose.ZERO);
            root.addOrReplaceChild("left_shoulder", box(0, 96, candles[2][0], -35.0f - candles[2][2], candles[2][1], 2, candles[2][2], 2)
                    .texOffs(0, 96).addBox(candles[3][0], -35.0f - candles[3][2], candles[3][1], 2, candles[3][2], 2), PartPose.ZERO);
        } else if (kind == Kind.GLOW) {
            root.addOrReplaceChild("right_shoulder", flame(candles[0]).texOffs(96, 64).addBox(candles[1][0] + 0.5f, -37.0f - candles[1][2], candles[1][1] + 0.5f, 1, 2, 1), PartPose.ZERO);
            root.addOrReplaceChild("left_shoulder", flame(candles[2]).texOffs(96, 64).addBox(candles[3][0] + 0.5f, -37.0f - candles[3][2], candles[3][1] + 0.5f, 1, 2, 1), PartPose.ZERO);
        } else if (kind == Kind.RUNES) {
            PartDefinition ring = root.addOrReplaceChild("upper", CubeListBuilder.create(), PartPose.ZERO)
                    .addOrReplaceChild("ring", CubeListBuilder.create(), PartPose.offset(0.0f, RING_Y, 0.0f));
            for (int i = 0; i < 8; ++i) {
                double angle = i * Math.PI * 2.0 / 8.0;
                ring.addOrReplaceChild("rune" + i, box(64 + (i % 4) * 16, 96 + (i / 4) * 8, -4.0f, -4.0f, 0.0f, 8, 8, 0),
                        PartPose.offsetAndRotation((float) Math.cos(angle) * RING_RADIUS, (i % 2) * 8.0f, (float) Math.sin(angle) * RING_RADIUS,
                                0.0f, (float) (Math.PI / 2.0 - angle), 0.0f));
            }
        }
    }

    private static CubeListBuilder flame(float[] candle) {
        return box(96, 64, candle[0] + 0.5f, -37.0f - candle[2], candle[1] + 0.5f, 1, 2, 1);
    }

    /** Diamond: clusters of diamond crystals growing out of its shoulders and back. */
    private static void diamond(PartDefinition root, Kind kind) {
        if (kind != Kind.SOLID) {
            return;
        }
        PartDefinition right = root.addOrReplaceChild("right_shoulder", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition left = root.addOrReplaceChild("left_shoulder", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        crystal(right, "a", -13.0f, -35.0f, -5.0f, -0.25f, -0.35f, 1.0f);
        crystal(right, "b", -6.0f, -35.0f, 5.0f, 0.35f, -0.15f, 0.75f);
        crystal(right, "c", -28.0f, -12.0f, 2.0f, 0.1f, -0.7f, 0.8f);
        crystal(left, "a", 13.0f, -35.0f, -5.0f, -0.25f, 0.35f, 1.0f);
        crystal(left, "b", 6.0f, -35.0f, 5.0f, 0.35f, 0.15f, 0.75f);
        crystal(left, "c", 28.0f, -12.0f, 2.0f, 0.1f, 0.7f, 0.8f);
        crystal(body, "a", -18.0f, -57.0f, 8.0f, 0.5f, -0.3f, 0.9f);
        // The middle one grows from the pack on its back, out of a rider's way.
        crystal(body, "b", 0.0f, -51.0f, 22.0f, 0.7f, 0.0f, 1.0f);
        crystal(body, "c", 18.0f, -57.0f, 8.0f, 0.5f, 0.3f, 0.9f);
    }

    private static void crystal(PartDefinition bone, String name, float x, float y, float z, float tiltX, float tiltZ, float size) {
        bone.addOrReplaceChild(name, box(0, 0, -3.0f * size, -12.0f * size, -3.0f * size, 6.0f * size, 12.0f * size, 6.0f * size)
                        .texOffs(24, 0).addBox(-2.0f * size, -18.0f * size, -2.0f * size, 4.0f * size, 6.0f * size, 4.0f * size)
                        .texOffs(0, 64).addBox(-1.0f * size, -22.0f * size, -1.0f * size, 2.0f * size, 4.0f * size, 2.0f * size),
                PartPose.offsetAndRotation(x, y, z, tiltX, 0.0f, tiltZ));
    }

    /**
     * Netherite: great warlord horns in place of Goety's horn stubs (hidden, see reshape),
     * growing from the sides of its head, sweeping out, then up and forward, tipped in gold, with
     * molten netherite glowing where they meet the head.
     */
    private static void netherite(PartDefinition root, Kind kind) {
        PartDefinition top = root.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            // Sunk 2 pixels into the side of the head, near its top.
            PartPose base = PartPose.offsetAndRotation(side * 12.0f, -17.0f, -9.5f, 0.0f, 0.0f, side * 1.3f);
            if (kind == Kind.SOLID) {
                PartDefinition part = top.addOrReplaceChild(side < 0 ? "right_horn" : "left_horn", box(0, 0, -5.0f, -10.0f, -5.0f, 10, 10, 10), base);
                float[][] segments = {{9.0f, 9.0f}, {7.0f, 9.0f}, {5.0f, 8.0f}, {4.0f, 7.0f}};
                float below = 10.0f;
                for (int i = 0; i < segments.length; ++i) {
                    float w = segments[i][0], h = segments[i][1];
                    int u = i == segments.length - 1 ? 0 : 40;
                    int v = i == segments.length - 1 ? 64 : i * 20;
                    part = part.addOrReplaceChild("segment" + i, box(u, v, -w / 2.0f, -h, -w / 2.0f, w, h, w),
                            PartPose.offsetAndRotation(0.0f, -below, 0.0f, 0.15f, 0.0f, -side * 0.38f));
                    below = h;
                }
                part.addOrReplaceChild("tip", box(0, 64, -1.0f, -6.0f, -1.0f, 2, 6, 2), PartPose.offsetAndRotation(0.0f, -below, 0.0f, 0.15f, 0.0f, -side * 0.3f));
            } else if (kind == Kind.GLOW) {
                top.addOrReplaceChild(side < 0 ? "right_ring" : "left_ring", box(64, 64, -5.5f, -3.0f, -5.5f, 11, 2, 11), base);
            }
        }
    }

    // The Sculk one's tentacle: thick segments from the elbow down to the ground, thinning, where it
    // bends back to drag behind it, its tip curling up.
    private static final int TENTACLE = 12;
    private static final float TENTACLE_LENGTH = 8.0f;
    private static final float[] TENTACLE_BEND = {0.02f, 0.02f, 0.02f, 0.02f, 0.02f, 0.2f, 0.4f, 0.45f, 0.4f, 0.15f, -0.15f, -0.3f};

    private static float tentacleWidth(int i) {
        return 22.0f - i * 1.35f;
    }

    /**
     * Sculk: a sculk tentacle in place of its right forearm (see reshape), dragging on the ground;
     * strands of sculk hanging from where its jaw was; sculk dripping from under its body and
     * shoulders; a pointed bone horn, curving forward, in place of its right horn; the prong walls of
     * the sculk shriekers its upper shoulder blocks have become (the rest of them is in its skin); and Warden
     * tendrils, one on each side of its head like the Warden's ears, four out of its back and two out
     * of its tentacle. (Its ribcage is in its skin, like the Warden's, and the tendrils of sculk
     * sensors on its back are real ones, see renderSensorTendrils.)
     */
    private static void sculk(PartDefinition root, Kind kind) {
        PartDefinition top = root.addOrReplaceChild("top", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition right = root.addOrReplaceChild("right_shoulder", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition left = root.addOrReplaceChild("left_shoulder", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition middle = root.addOrReplaceChild("right_middle", CubeListBuilder.create(), PartPose.ZERO);
        switch (kind) {
            case SOLID -> {
                for (int i = 0; i < JAW_STRANDS.length; ++i) {
                    float[] s = JAW_STRANDS[i];
                    // From inside its head (its underside is at y 6), so they hang from it; each swings
                    // from its root (see swayStrands).
                    top.addOrReplaceChild("strand" + i, box(0, 0, -s[3] / 2.0f, 0.0f, -s[3] / 2.0f, s[3], s[2], s[3]), PartPose.offset(s[0], 4.0f, s[1]));
                }
                boneHorn(top);
                shriekerWalls(right, -9.0f);
                shriekerWalls(left, 9.0f);
                PartDefinition segment = middle;
                for (int i = 0; i < TENTACLE; ++i) {
                    float w = tentacleWidth(i);
                    segment = segment.addOrReplaceChild("segment" + i, box(0, 0, -w / 2.0f, 0.0f, -w / 2.0f, w, TENTACLE_LENGTH + 1.0f, w),
                            PartPose.offset(0.0f, i == 0 ? 2.0f : TENTACLE_LENGTH, i == 0 ? -5.0f : 0.0f));
                }
            }
            case GLOW -> {
                // A glowing bead at the end of some of the strands (swinging with them).
                for (int i = 0; i < JAW_STRANDS.length; i += 2) {
                    float[] s = JAW_STRANDS[i];
                    top.addOrReplaceChild("strand" + i, box(64, 64, -s[3] / 2.0f, s[2] - 1.5f, -s[3] / 2.0f, s[3], 1.5f, s[3], 0.15f),
                            PartPose.offset(s[0], 4.0f, s[1]));
                }
            }
            case CUTOUT -> {
                // Sculk dripping from under its body, all round, and from its shoulders: each sheet hangs
                // from a pixel up inside the edge, just behind the face, so it runs straight out of it.
                float[] front = {-28.0f, -10.0f, 12.0f, 28.0f};
                for (int i = 0; i < front.length; ++i) {
                    drip(body, "front" + i, front[i], 0.0f, -14.3f, 0.0f);
                }
                drip(body, "back0", -20.0f, 0.0f, 15.3f, 0.0f);
                drip(body, "back1", 18.0f, 0.0f, 15.3f, 0.0f);
                drip(body, "right_side", -36.8f, 0.0f, 0.0f, (float) Math.PI / 2.0f);
                drip(body, "left_side", 36.8f, 0.0f, 0.0f, (float) Math.PI / 2.0f);
                drip(right, "front", -25.0f, 11.0f, -13.3f, 0.0f);
                drip(right, "side", -35.8f, 11.0f, 0.0f, (float) Math.PI / 2.0f);
                drip(left, "front", 22.0f, 11.0f, -13.3f, 0.0f);
                drip(left, "side", 35.8f, 11.0f, 0.0f, (float) Math.PI / 2.0f);
                // Warden ears, growing out of the sides of its head below its horns (their roots inside it).
                tendril(top, "right_ear", -12.5f, 3.0f, -9.0f, -0.2f);
                tendril(top, "left_ear", 12.5f, 3.0f, -9.0f, 0.2f);
                // More Warden tendrils growing up out of the top of its back.
                tendril(body, "back_tendril0", -28.0f, -55.0f, -4.0f, 1.0f);
                tendril(body, "back_tendril1", 28.0f, -55.0f, -4.0f, -1.0f);
                tendril(body, "back_tendril2", -5.0f, -55.0f, 10.0f, 1.25f);
                tendril(body, "back_tendril3", 5.0f, -55.0f, 10.0f, -1.25f);
                // Out of the tentacle's side (the same chain as the solid one, so it sways with it).
                PartDefinition segment = middle;
                for (int i = 0; i < TENTACLE; ++i) {
                    segment = segment.addOrReplaceChild("segment" + i, CubeListBuilder.create(),
                            PartPose.offset(0.0f, i == 0 ? 2.0f : TENTACLE_LENGTH, i == 0 ? -5.0f : 0.0f));
                    if (i == 2 || i == 5) {
                        tendril(segment, "tip", 1.0f - tentacleWidth(i) / 2.0f, TENTACLE_LENGTH / 2.0f, 0.0f, -0.4f);
                    }
                }
            }
            default -> {
            }
        }
    }

    // A sheet of dripping sculk (two-sided art), hanging from a pixel above (x, y, z), turned by yaw.
    private static void drip(PartDefinition bone, String name, float x, float y, float z, float yaw) {
        bone.addOrReplaceChild(name, box(80, 96, -8.0f, -1.0f, 0.0f, 16, 16, 0), PartPose.offsetAndRotation(x, y, z, 0.0f, yaw, 0.0f));
    }

    /**
     * The prong walls of a sculk shrieker, as the game's shrieker has them: a box inset a pixel from
     * the top of an upper shoulder block centred on x (20 wide, 27 deep, its top at y -35), 7 pixels
     * tall, its sides the shrieker's prongs and its top their L-shaped caps, see-through between
     * (textured in tools/MonstrositySkins.java). Drawn from both sides, so from inside too, as the
     * shrieker's own are.
     */
    private static void shriekerWalls(PartDefinition shoulder, float x) {
        shoulder.addOrReplaceChild("shrieker", box(128, 0, x - 9.0f, -42.0f, -12.5f, 18, 7, 25), PartPose.ZERO);
    }

    // The bone horn on its right, where Goety's stone one was: pointed, curving forward and a little up.
    private static void boneHorn(PartDefinition top) {
        PartDefinition part = top.addOrReplaceChild("bone_horn", box(0, 64, -4.5f, -4.5f, -9.0f, 9, 9, 9),
                PartPose.offsetAndRotation(-13.0f, -20.0f, -10.0f, 0.1f, 0.35f, 0.15f));
        // Its root, reaching back and in, deep into its head.
        part.addOrReplaceChild("root", box(0, 64, -1.0f, -3.5f, 0.0f, 8, 7, 8), PartPose.ZERO);
        float[][] segments = {{7.0f, 8.0f}, {5.5f, 7.0f}, {4.0f, 6.0f}, {2.5f, 5.0f}, {1.2f, 4.0f}};
        float behind = 9.0f;
        for (int i = 0; i < segments.length; ++i) {
            float w = segments[i][0], length = segments[i][1];
            part = part.addOrReplaceChild("segment" + i, box(0, 64, -w / 2.0f, -w / 2.0f, -length, w, w, length),
                    PartPose.offsetAndRotation(0.0f, 0.0f, -behind, -0.06f, -0.08f, 0.0f));
            behind = length;
        }
    }

    // A Warden tendril (one plane, as the Warden's are) growing outward from (x, y, z), to the left
    // for a positive x and mirrored to the right for a negative one, as the Warden's own, leaning by tilt.
    private static void tendril(PartDefinition bone, String name, float x, float y, float z, float tilt) {
        CubeListBuilder plane = x < 0.0f ? CubeListBuilder.create().texOffs(64, 112).mirror().addBox(-16.0f, -13.0f, 0.0f, 16, 16, 0)
                : box(64, 112, 0.0f, -13.0f, 0.0f, 16, 16, 0);
        bone.addOrReplaceChild(name, plane, PartPose.offsetAndRotation(x, y, z, 0.0f, 0.0f, tilt));
    }

    // Strands hanging from under the Sculk one's head where its jaw was: x, z, length, thickness.
    private static final float[][] JAW_STRANDS = {{-11, -17, 14, 2}, {-6, -18, 9, 3}, {-1, -16, 18, 2}, {4, -18, 11, 2}, {9, -17, 15, 3},
            {-9, -10, 12, 2}, {2, -9, 16, 3}, {10, -8, 10, 2}, {-4, -3, 13, 2}, {6, -2, 9, 2}};

    /**
     * Swings the strands hanging from its jaw (in a solid or glowing set of pieces) from their roots:
     * back and forth and a little side to side with each step as it walks, each a little out of step
     * with the others, and barely at all while it stands. Still on its head block.
     */
    private static void swayStrands(ModelPart pieces, boolean still, float ageInTicks, float limbSwing, float limbSwingAmount) {
        if (!pieces.hasChild("top")) {
            return;
        }
        ModelPart top = pieces.getChild("top");
        for (int i = 0; i < JAW_STRANDS.length; ++i) {
            if (!top.hasChild("strand" + i)) {
                continue;
            }
            ModelPart strand = top.getChild("strand" + i);
            if (still) {
                strand.xRot = 0.0f;
                strand.zRot = 0.0f;
                continue;
            }
            strand.xRot = (float) Math.sin(limbSwing * 0.6662f + i * 0.7f) * 0.3f * limbSwingAmount + (float) Math.sin(ageInTicks * 0.08f + i) * 0.03f;
            strand.zRot = (float) Math.cos(limbSwing * 0.3331f + i * 1.3f) * 0.18f * limbSwingAmount + (float) Math.sin(ageInTicks * 0.06f + i * 0.9f) * 0.03f;
        }
    }

    // The tentacle's bend for its slam: raised, curling back behind its shoulder; and thrown, carrying
    // on down from its arm and then out flat along the ground ahead.
    private static final float[] TENTACLE_RAISED = {0.0f, 0.0f, 0.0f, 0.0f, -0.1f, -0.15f, -0.2f, -0.2f, -0.2f, -0.2f, -0.15f, -0.1f};
    private static final float[] TENTACLE_THROWN = {0.0f, 0.0f, 0.0f, 0.0f, -0.2f, -0.3f, -0.35f, -0.37f, 0.0f, 0.0f, 0.0f, 0.05f};

    // Bends the tentacle down to the ground and back (in a solid or tendril set of pieces) and sways it,
    // each segment a little behind the one above; as it walks, it drags further back and swings side
    // to side. For its slam, {@code curl} (-1 to 1) bends it between that and raised or thrown.
    private static void swayTentacle(ModelPart pieces, float ageInTicks, float limbSwing, float limbSwingAmount, float curl) {
        if (!pieces.hasChild("right_middle")) {
            return;
        }
        float rest = 1.0f - Math.abs(curl);
        float[] bent = curl < 0.0f ? TENTACLE_RAISED : TENTACLE_THROWN;
        ModelPart segment = pieces.getChild("right_middle");
        for (int i = 0; i < TENTACLE; ++i) {
            segment = segment.getChild("segment" + i);
            float drag = i >= 3 && i <= 8 ? limbSwingAmount * 0.12f : 0.0f;
            float sway = (float) Math.sin(ageInTicks * 0.05f - i * 0.5f) * 0.03f;
            segment.xRot = (TENTACLE_BEND[i] + drag) * rest + bent[i] * Math.abs(curl) + sway;
            segment.zRot = ((i < 6 ? 0.04f : 0.0f) + (float) Math.sin(ageInTicks * 0.07f - i * 0.6f) * 0.03f * (1.0f + i * 0.2f)
                    + (float) Math.sin(limbSwing * 0.6f - i * 0.4f) * 0.15f * limbSwingAmount * i / TENTACLE) * rest;
        }
    }

    /**
     * One of the Sculk one's tentacles on its own (SculkTentacleRenderer), from the pose's origin:
     * the same pieces as its tentacle arm, growing up out of the ground (+y) and bending toward +z
     * by {@code curl} (see SculkTentacle.curl): reared back, upright and swaying, or lying flat.
     */
    static void renderTentacle(float curl, float ageInTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Pieces pieces = pieces(MonstrosityMaterial.SCULK);
        ResourceLocation texture = MaterialMonstrosityRenderer.texture(MonstrosityMaterial.SCULK, "parts");
        float rest = 1.0f - Math.abs(curl);
        for (ModelPart set : new ModelPart[]{pieces.solid(), pieces.cutout()}) {
            ModelPart segment = set.getChild("right_middle");
            for (int i = 0; i < TENTACLE; ++i) {
                segment = segment.getChild("segment" + i);
                // Upright it writhes, a wave running up it; reared back, it hooks over backwards; thrown,
                // it bends a quarter turn over its first three segments and lies flat, its tip curling up.
                float wave = (float) Math.sin(ageInTicks * 0.15f - i * 0.7f);
                float bend = curl < 0.0f ? (i < 2 ? 0.05f : 0.2f) * curl : (i < 3 ? 0.5f : i == TENTACLE - 1 ? -0.15f : 0.0f) * curl;
                segment.xRot = bend + wave * (0.04f + 0.1f * rest);
                segment.zRot = (float) Math.sin(ageInTicks * 0.11f - i * 0.6f) * 0.08f * rest;
            }
        }
        poseStack.pushPose();
        // Its first segment sits 2 below and 5 ahead of its arm's elbow.
        poseStack.translate(0.0f, -2.0f / 16.0f, 5.0f / 16.0f);
        pieces.solid().getChild("right_middle").render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY);
        pieces.cutout().getChild("right_middle").render(poseStack, buffers.getBuffer(RenderType.entityCutout(texture)), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
