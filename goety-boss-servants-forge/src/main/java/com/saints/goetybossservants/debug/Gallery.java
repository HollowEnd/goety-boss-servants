package com.saints.goetybossservants.debug;

import com.Polarice3.Goety.common.blocks.ModBlocks;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.saints.goetybossservants.GoetyBossServants;
import com.saints.goetybossservants.compat.ModCompat;
import com.saints.goetybossservants.construct.MaterialMonstrosityMold;
import com.saints.goetybossservants.construct.MonstrosityMoldShape;
import com.saints.goetybossservants.entity.CaptainCorneliaServant;
import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Dev-only: the CurseForge page's pictures (gradlew runClient -Pgallery). Every mob this mod adds,
 * each boss's summoning page of the Black Book, and each monstrosity mold's bottom and top layers,
 * drawn off screen once over black and once over white (tools/GalleryFinish.java works out the
 * transparency from the pair, then trims and smooths them), to run-showcase/gallery. Then quits.
 */
@Mod.EventBusSubscriber(modid = GoetyBossServants.MOD_ID, value = Dist.CLIENT)
public class Gallery {
    static final boolean ENABLED = Boolean.getBoolean(GoetyBossServants.MOD_ID + ".gallery");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation BOOK = new ResourceLocation("goety", "black_book");
    private static final Path OUT = Path.of("gallery");
    // Mobs and molds are drawn at twice the size they're kept at (GalleryFinish halves them, smoothing the edges).
    private static final int MOB_SIZE = 2048;

    private interface Job {
        void run() throws Exception;
    }

    private record Step(int at, Job run) {
    }

    private static final List<Step> steps = new ArrayList<>();
    private static int tick = -1;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ENABLED || event.phase != TickEvent.Phase.END || minecraft.player == null || minecraft.level == null) {
            return;
        }
        ++tick;
        if (tick == 60) {
            minecraft.options.hideGui = true;
            plan();
        }
        for (Step step : steps) {
            if (step.at() == tick) {
                try {
                    step.run().run();
                } catch (Throwable t) {
                    LOGGER.error("GALLERY FAIL at tick {}", tick, t);
                }
            }
        }
    }

    // ----- What to draw -----------------------------------------------------------------------

    private static void plan() {
        int at = 80;
        // The mobs.
        List<String[]> mobs = new ArrayList<>();
        mob(mobs, "mira_monstrosity", ModRegistry.MIRA_MONSTROSITY, "");
        mob(mobs, "mira_monstrosity_obsidian", ModRegistry.MIRA_MONSTROSITY, "{MiraVariant:'obsidian'}");
        mob(mobs, "mira_monstrosity_warden", ModRegistry.MIRA_MONSTROSITY, "{MiraVariant:'warden'}");
        mob(mobs, "captain_cornelia", ModRegistry.CAPTAIN_CORNELIA, "");
        mob(mobs, "void_blossom", ModRegistry.VOID_BLOSSOM, "");
        mob(mobs, "night_lich", ModRegistry.NIGHT_LICH, "");
        mob(mobs, "old_champion", ModRegistry.OLD_CHAMPION, "");
        mob(mobs, "returning_knight", ModRegistry.RETURNING_KNIGHT, "");
        mob(mobs, "netherman", ModRegistry.NETHERMAN, "{shouldPlaySummonAnimation:0b}");
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            mob(mobs, material.entityName(), ModRegistry.MONSTROSITIES.get(material), "");
        }
        for (String[] mob : mobs) {
            steps.add(new Step(at, () -> drawMob(mob[0], mob[1])));
            at += 4;
        }

        // The bosses' summoning rituals, on page 2 of their entries (the Mira Monstrosity's is a mold, drawn below).
        for (String boss : List.of("cornelia", "void_blossom", "night_lich", "old_champion", "returning_knight", "netherman")) {
            String entry = "goetybossservants/bosses/" + boss;
            steps.add(new Step(at, () -> BookChecks.Patchouli.open(BOOK, new ResourceLocation("goety", entry), 2)));
            steps.add(new Step(at + 10, () -> drawBook(boss)));
            at += 14;
        }
        steps.add(new Step(at, () -> Minecraft.getInstance().setScreen(null)));
        at += 4;

        // The molds, layer by layer (the core is on the top layer, the brick under it on the bottom one).
        for (MonstrosityMaterial material : MonstrosityMaterial.values()) {
            MaterialMonstrosityMold.Mold mold = MaterialMonstrosityMold.mold(material);
            steps.add(new Step(at, () -> drawMold(material.entityName(), mold.ringShown(), mold.fillShown(), mold.centerShown(), mold.coreShown(),
                    material == MonstrosityMaterial.SCULK)));
            at += 4;
        }
        if (ModRegistry.MIRA_MONSTROSITY != null) {
            Supplier<Block> terracotta = () -> Blocks.ORANGE_TERRACOTTA, redstone = () -> ModBlocks.REINFORCED_REDSTONE_BLOCK.get();
            steps.add(new Step(at, () -> drawMold("mira_monstrosity", () -> Blocks.LAVA, terracotta, terracotta, redstone, false)));
            steps.add(new Step(at + 4, () -> drawMold("mira_monstrosity_obsidian", () -> Blocks.OBSIDIAN, terracotta, terracotta, redstone, false)));
            steps.add(new Step(at + 8, () -> drawMold("mira_monstrosity_warden", () -> Blocks.SCULK, terracotta, terracotta, redstone, false)));
            at += 12;
        }
        steps.add(new Step(at + 10, () -> {
            LOGGER.info("GALLERY DONE");
            Runtime.getRuntime().halt(0);
        }));
    }

    private static void mob(List<String[]> mobs, String name, RegistryObject<? extends EntityType<?>> type, String nbt) {
        if (type != null && type.isPresent()) {
            mobs.add(new String[]{name, ForgeRegistries.ENTITY_TYPES.getKey(type.get()) + nbt});
        }
    }

    // ----- Mobs -------------------------------------------------------------------------------

    // Three-quarter view from a little above, fitted to the canvas over a few tries (models reach past their hitboxes).
    private static void drawMob(String name, String spec) throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        int brace = spec.indexOf('{');
        CompoundTag tag = brace < 0 ? new CompoundTag() : TagParser.parseTag(spec.substring(brace));
        tag.putString("id", brace < 0 ? spec : spec.substring(0, brace));
        Entity entity = EntityType.loadEntityRecursive(tag, minecraft.level, e -> e);
        if (!(entity instanceof LivingEntity living)) {
            LOGGER.error("GALLERY FAIL {}: not a living entity ({})", name, entity);
            return;
        }
        // A summoned Cornelia holds her main weapon (finalizeSpawn equips it; loading from NBT doesn't).
        if (ModCompat.aquamirae() && living instanceof CaptainCorneliaServant cornelia) {
            cornelia.equipWeapon(cornelia.getSeasonalWeapon());
        }
        float yaw = 205.0f;
        living.setYRot(yaw);
        living.yRotO = yaw;
        living.setYBodyRot(yaw);
        living.yBodyRotO = yaw;
        living.setYHeadRot(yaw);
        living.yHeadRotO = yaw;
        living.setXRot(0.0f);
        living.xRotO = 0.0f;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(new Quaternionf().rotateX((float) Math.toRadians(-12.0)));
        // Let its animations run a few seconds first (a fresh mob may be mid-way into a spawn animation,
        // the Void Blossom still underground): draw it small now and then as its clock moves on.
        for (int i = 0; i < 8; ++i) {
            living.tickCount += 10;
            once(256, 256, 1, 0.0f, graphics -> InventoryScreen.renderEntityInInventory(graphics, 128, 200, 20, pose, null, living)).close();
        }

        double scale = 0.42 * MOB_SIZE / Math.max(living.getBbHeight(), living.getBbWidth());
        double x = MOB_SIZE / 2.0, y = MOB_SIZE * 0.72;
        NativeImage[] passes = null;
        for (int attempt = 0; attempt < 4; ++attempt) {
            int s = (int) Math.round(scale), px = (int) Math.round(x), py = (int) Math.round(y);
            close(passes);
            passes = twice(MOB_SIZE, MOB_SIZE, 1, graphics -> InventoryScreen.renderEntityInInventory(graphics, px, py, s, pose, null, living));
            int[] box = bounds(passes);
            if (box == null) {
                LOGGER.error("GALLERY FAIL {}: drew nothing", name);
                close(passes);
                return;
            }
            boolean clipped = box[0] <= 1 || box[1] <= 1 || box[2] >= MOB_SIZE - 2 || box[3] >= MOB_SIZE - 2;
            double size = Math.max(box[2] - box[0], box[3] - box[1]) / (double) MOB_SIZE;
            if (!clipped && size > 0.62 && size < 0.9) {
                break;
            }
            // Rescale about the anchor, then move the drawing's middle to the canvas's.
            double k = clipped ? 0.55 : 0.78 / size;
            double midX = (box[0] + box[2]) / 2.0, midY = (box[1] + box[3]) / 2.0;
            scale *= k;
            x = MOB_SIZE / 2.0 - (midX - x) * k;
            y = MOB_SIZE / 2.0 - (midY - y) * k;
        }
        write("mobs", name, passes, null);
    }

    // ----- Book pages -------------------------------------------------------------------------

    // The open book, four times its usual size; the rectangle it fills is written beside it.
    private static void drawBook(String name) throws IOException {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.screen;
        if (screen == null) {
            LOGGER.error("GALLERY FAIL {}: the book didn't open", name);
            return;
        }
        int guiScale = 4, guiWidth = 320, guiHeight = 200, width = guiWidth * guiScale, height = guiHeight * guiScale;
        Window window = minecraft.getWindow();
        int oldWidth = window.getWidth(), oldHeight = window.getHeight();
        double oldScale = window.getGuiScale();
        window.setWidth(width);
        window.setHeight(height);
        window.setGuiScale(guiScale);
        screen.resize(minecraft, guiWidth, guiHeight);
        // Without the page-turn and back buttons, which hang off the book's bottom edge (resizing back restores them).
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget) {
                widget.setX(-10000);
            }
        }
        NativeImage[] passes;
        try {
            passes = twice(width, height, guiScale, graphics -> screen.render(graphics, -1000, -1000, minecraft.getFrameTime()));
        } finally {
            window.setWidth(oldWidth);
            window.setHeight(oldHeight);
            window.setGuiScale(oldScale);
            screen.resize(minecraft, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
        // Patchouli centres its 272x180 book.
        int[] book = {(guiWidth / 2 - 136) * guiScale, (guiHeight / 2 - 90) * guiScale, 272 * guiScale, 180 * guiScale};
        write("summoning", name, passes, book);
    }

    // ----- Molds ------------------------------------------------------------------------------

    // centreCloseUp: also the core and the four blocks round it on their own, larger (the Sculk mold's are
    // half-height shriekers round a slab, lost among the full-height fill at the layer's size).
    private static void drawMold(String name, Supplier<Block> ring, Supplier<Block> fill, Supplier<Block> center, Supplier<Block> core, boolean centreCloseUp)
            throws IOException {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        MonstrosityMoldShape.STONE_LOCATIONS.forEach(pos -> blocks.put(pos, Blocks.STONE_BRICKS.defaultBlockState()));
        MonstrosityMoldShape.DIAMOND_LOCATIONS.forEach(pos -> blocks.put(pos, ModBlocks.DIAMOND_MOLD_BLOCK.get().defaultBlockState()));
        MonstrosityMoldShape.RING_LOCATIONS.forEach(pos -> blocks.put(pos, ring.get().defaultBlockState()));
        MonstrosityMoldShape.FILL_LOCATIONS.forEach(pos -> blocks.put(pos, fill.get().defaultBlockState()));
        MonstrosityMoldShape.CENTER_LOCATIONS.forEach(pos -> blocks.put(pos, center.get().defaultBlockState()));
        // The two rim spots the mold never checks get bricks, as in the book's pattern.
        List.of(new BlockPos(-1, 0, 6), new BlockPos(-1, 0, -6)).forEach(pos -> blocks.put(pos, Blocks.STONE_BRICKS.defaultBlockState()));
        blocks.put(MonstrosityMoldShape.CORE, core.get().defaultBlockState());
        blocks.put(MonstrosityMoldShape.CORE.below(), Blocks.STONE_BRICKS.defaultBlockState());
        for (int layer : new int[]{-1, 0}) {
            Map<BlockPos, BlockState> only = new HashMap<>();
            blocks.forEach((pos, state) -> {
                if (pos.getY() == layer) {
                    only.put(pos.atY(0), state);
                }
            });
            NativeImage[] passes = twice(MOB_SIZE, 1400, 1, graphics -> drawBlocks(graphics, only, MOB_SIZE / 2.0f, 640.0f, 84.0f));
            write("molds", name + (layer < 0 ? "_bottom" : "_top"), passes, null);
        }
        if (centreCloseUp) {
            Map<BlockPos, BlockState> centre = new HashMap<>();
            MonstrosityMoldShape.CENTER_LOCATIONS.forEach(pos -> centre.put(pos, center.get().defaultBlockState()));
            centre.put(MonstrosityMoldShape.CORE, core.get().defaultBlockState());
            write("molds", name + "_centre", twice(MOB_SIZE, 1400, 1, graphics -> drawBlocks(graphics, centre, MOB_SIZE / 2.0f, 760.0f, 300.0f)), null);
        }
    }

    // Isometric, like the Black Book's multiblock pages, lit like the world at midday.
    private static void drawBlocks(GuiGraphics graphics, Map<BlockPos, BlockState> blocks, float x, float y, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 3000.0f);
        // Y up; mirrored once more, as the GUI's own y flip turns faces inside out (entities in the GUI do the same).
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(30.0f));
        pose.mulPose(Axis.YP.rotationDegrees(45.0f));
        pose.translate(-0.5f, -0.5f, -0.5f);
        MultiBufferSource.BufferSource buffers = graphics.bufferSource();
        BlockRenderDispatcher renderer = minecraft.getBlockRenderer();
        RandomSource random = RandomSource.create(42L);
        Layer level = new Layer(blocks);
        minecraft.gameRenderer.lightTexture().turnOnLightLayer();
        for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            if (!state.getFluidState().isEmpty()) {
                fluid(pose, buffers, state);
            } else {
                BakedModel model = renderer.getBlockModel(state);
                for (RenderType type : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                    renderer.renderBatched(state, pos, level, pose, buffers.getBuffer(type), false, random, ModelData.EMPTY, type);
                }
                if (state.getBlock() instanceof EntityBlock block) {
                    BlockEntity blockEntity = block.newBlockEntity(pos, state);
                    if (blockEntity != null) {
                        blockEntity.setLevel(minecraft.level);
                        BlockEntityRenderer<BlockEntity> special = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
                        if (special != null) {
                            special.render(blockEntity, 0.0f, pose, buffers, 0xF000F0, OverlayTexture.NO_OVERLAY);
                        }
                    }
                }
            }
            pose.popPose();
        }
        buffers.endBatch();
        minecraft.gameRenderer.lightTexture().turnOffLightLayer();
        pose.popPose();
    }

    // A still fluid source: its still texture on a block 14/16 high, faces shaded as the world shades them.
    private static void fluid(PoseStack pose, MultiBufferSource buffers, BlockState state) {
        String still = state.is(Blocks.LAVA) ? "block/lava_still" : "block/water_still";
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(new ResourceLocation(still));
        VertexConsumer consumer = buffers.getBuffer(RenderType.solid());
        float h = 0.875f;
        float[][][] faces = {
                {{0, h, 0}, {0, h, 1}, {1, h, 1}, {1, h, 0}, {0, 1, 0, 1.0f}},
                {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, -1, 0, 0.5f}},
                {{1, h, 0}, {1, 0, 0}, {0, 0, 0}, {0, h, 0}, {0, 0, -1, 0.8f}},
                {{0, h, 1}, {0, 0, 1}, {1, 0, 1}, {1, h, 1}, {0, 0, 1, 0.8f}},
                {{0, h, 0}, {0, 0, 0}, {0, 0, 1}, {0, h, 1}, {-1, 0, 0, 0.6f}},
                {{1, h, 1}, {1, 0, 1}, {1, 0, 0}, {1, h, 0}, {1, 0, 0, 0.6f}}};
        float[][] uv = {{sprite.getU0(), sprite.getV0()}, {sprite.getU0(), sprite.getV1()}, {sprite.getU1(), sprite.getV1()}, {sprite.getU1(), sprite.getV0()}};
        Matrix4f matrix = pose.last().pose();
        for (float[][] face : faces) {
            float[] normal = face[4];
            int shade = Math.round(normal[3] * 255);
            // Both windings, so whichever way the view turns it, culling keeps one.
            for (int[] order : new int[][]{{0, 1, 2, 3}, {3, 2, 1, 0}}) {
                for (int i : order) {
                    consumer.vertex(matrix, face[i][0], face[i][1], face[i][2]).color(shade, shade, shade, 255).uv(uv[i][0], uv[i][1]).uv2(0xF000F0)
                            .normal(pose.last().normal(), normal[0], normal[1], normal[2]).endVertex();
                }
            }
        }
    }

    /** One layer of blocks as a world, so models get the world's face shading and ambient occlusion. */
    private record Layer(Map<BlockPos, BlockState> blocks) implements BlockAndTintGetter {
        @Override
        public float getShade(Direction direction, boolean shade) {
            if (!shade) {
                return 1.0f;
            }
            return switch (direction) {
                case DOWN -> 0.5f;
                case UP -> 1.0f;
                case NORTH, SOUTH -> 0.8f;
                default -> 0.6f;
            };
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return Minecraft.getInstance().level.getLightEngine();
        }

        @Override
        public int getBrightness(LightLayer layer, BlockPos pos) {
            return 15;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int darkening) {
            return 15;
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver resolver) {
            return 0xFFFFFF;
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinBuildHeight() {
            return -64;
        }
    }

    // ----- Drawing off screen -----------------------------------------------------------------

    /**
     * Draws the same thing twice off screen, over opaque black and over opaque white, in a GUI-style
     * projection (guiScale pixels to a unit). Comparing the two gives each pixel's coverage, whatever
     * blending the drawing used.
     */
    private static NativeImage[] twice(int width, int height, int guiScale, Consumer<GuiGraphics> draw) {
        return new NativeImage[]{once(width, height, guiScale, 0.0f, draw), once(width, height, guiScale, 1.0f, draw)};
    }

    private static NativeImage once(int width, int height, int guiScale, float background, Consumer<GuiGraphics> draw) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        target.setClearColor(background, background, background, 1.0f);
        target.clear(Minecraft.ON_OSX);
        target.bindWrite(true);
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        VertexSorting sorting = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0f, (float) width / guiScale, (float) height / guiScale, 0.0f, 1000.0f, 21000.0f), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack view = RenderSystem.getModelViewStack();
        view.pushPose();
        view.setIdentity();
        view.translate(0.0f, 0.0f, -11000.0f);
        RenderSystem.applyModelViewMatrix();
        FogRenderer.setupNoFog();
        Lighting.setupFor3DItems();
        try {
            Bare graphics = new Bare(minecraft, width / guiScale, height / guiScale);
            draw.accept(graphics);
            graphics.flush();
        } finally {
            view.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
        }
        NativeImage image = new NativeImage(width, height, false);
        RenderSystem.bindTexture(target.getColorTextureId());
        image.downloadTexture(0, false);
        image.flipY();
        target.destroyBuffers();
        minecraft.getMainRenderTarget().bindWrite(true);
        return image;
    }

    /** GUI drawing without a screen's darkened backdrop (the gradient over the whole screen). */
    private static final class Bare extends GuiGraphics {
        private final int width, height;

        Bare(Minecraft minecraft, int width, int height) {
            super(minecraft, minecraft.renderBuffers().bufferSource());
            this.width = width;
            this.height = height;
        }

        private boolean wholeScreen(int x1, int y1, int x2, int y2) {
            return x1 <= 0 && y1 <= 0 && x2 >= this.width && y2 >= this.height;
        }

        @Override
        public void fillGradient(int x1, int y1, int x2, int y2, int from, int to) {
            if (!this.wholeScreen(x1, y1, x2, y2)) {
                super.fillGradient(x1, y1, x2, y2, from, to);
            }
        }

        @Override
        public void fillGradient(int x1, int y1, int x2, int y2, int z, int from, int to) {
            if (!this.wholeScreen(x1, y1, x2, y2)) {
                super.fillGradient(x1, y1, x2, y2, z, from, to);
            }
        }
    }

    // The drawn part of a pair: where the black pass isn't black or the white pass isn't white (x0, y0, x1, y1).
    private static int[] bounds(NativeImage[] passes) {
        int width = passes[0].getWidth(), height = passes[0].getHeight();
        int x0 = width, y0 = height, x1 = -1, y1 = -1;
        for (int y = 0; y < height; ++y) {
            for (int x = 0; x < width; ++x) {
                int dark = passes[0].getPixelRGBA(x, y) & 0xFFFFFF, light = passes[1].getPixelRGBA(x, y) & 0xFFFFFF;
                if (dark != 0 || light != 0xFFFFFF) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        return x1 < 0 ? null : new int[]{x0, y0, x1, y1};
    }

    private static void write(String kind, String name, NativeImage[] passes, int[] rect) throws IOException {
        Path dir = OUT.resolve(kind);
        Files.createDirectories(dir);
        passes[0].writeToFile(dir.resolve(name + ".black.png"));
        passes[1].writeToFile(dir.resolve(name + ".white.png"));
        if (rect != null) {
            Files.writeString(dir.resolve(name + ".rect"), rect[0] + " " + rect[1] + " " + rect[2] + " " + rect[3]);
        }
        close(passes);
        LOGGER.info("GALLERY wrote {}/{}", kind, name);
    }

    private static void close(NativeImage[] passes) {
        if (passes != null) {
            for (NativeImage image : passes) {
                image.close();
            }
        }
    }
}
