package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.compat.ModCompat;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skips each boss mod's mixins when that mod isn't installed (the boss mods are optional; see
 * ModCompat). Mixins not listed here always apply. Also opens up the Bosses of Mass Destruction
 * classes the servants extend (see BmdOpenClassesMixin).
 */
public class GoetyBossServantsMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "com.saints.goetybossservants.mixin.";
    private static final Map<String, String> REQUIRED_MOD = Map.ofEntries(
            Map.entry("CaptainCorneliaMixin", ModCompat.AQUAMIRAE),
            Map.entry("BmdOpenClassesMixin", ModCompat.BMD),
            Map.entry("OldChampionMixin", ModCompat.SOULSLIKE),
            Map.entry("SoulslikeGoalMixin", ModCompat.SOULSLIKE),
            Map.entry("NethermanMixin", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanAccessor", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanCloneMixin", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanDarknessGoalMixin", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanLavaTeleportGoalMixin", ModCompat.KNIGHTQUEST),
            Map.entry("SaintsDragonStunMixin", ModCompat.SAINTS_DRAGONS),
            Map.entry("SaintsDragonSleepMixin", ModCompat.SAINTS_DRAGONS),
            Map.entry("SaintsDragonTargetMixin", ModCompat.SAINTS_DRAGONS));
    /** Classes whose final flags preApply removes (see BmdOpenClassesMixin). */
    private static final Set<String> OPENED = Set.of(
            "net.barribob.boss.mob.utils.BaseEntity",
            "net.barribob.boss.mob.mobs.lich.LichEntity",
            "net.barribob.boss.mob.mobs.void_blossom.VoidBlossomEntity");

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = REQUIRED_MOD.get(mixinClassName.substring(PACKAGE.length()));
        return modId == null || FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (OPENED.contains(targetClassName)) {
            targetClass.access &= ~Opcodes.ACC_FINAL;
            for (MethodNode method : targetClass.methods) {
                method.access &= ~Opcodes.ACC_FINAL;
            }
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
