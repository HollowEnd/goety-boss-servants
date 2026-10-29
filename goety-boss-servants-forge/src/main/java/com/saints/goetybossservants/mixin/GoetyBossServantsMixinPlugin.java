package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.compat.ModCompat;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Skips each boss mod's mixins when that mod isn't installed (the boss mods are optional; see
 * ModCompat). Mixins not listed here (the Goety ritual one) always apply.
 */
public class GoetyBossServantsMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "com.saints.goetybossservants.mixin.";
    private static final Map<String, String> REQUIRED_MOD = Map.ofEntries(
            Map.entry("MiraMonstrosityAccessor", ModCompat.MEOWHEM),
            Map.entry("MiraMonstrosityServantMixin", ModCompat.MEOWHEM),
            Map.entry("CaptainCorneliaMixin", ModCompat.AQUAMIRAE),
            Map.entry("OldChampionMixin", ModCompat.SOULSLIKE),
            Map.entry("SoulslikeGoalMixin", ModCompat.SOULSLIKE),
            Map.entry("NethermanAccessor", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanCloneMixin", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanDarknessGoalMixin", ModCompat.KNIGHTQUEST),
            Map.entry("NethermanLavaTeleportGoalMixin", ModCompat.KNIGHTQUEST),
            Map.entry("SaintsDragonStunMixin", ModCompat.SAINTS_DRAGONS),
            Map.entry("SaintsDragonSleepMixin", ModCompat.SAINTS_DRAGONS),
            Map.entry("SaintsDragonTargetMixin", ModCompat.SAINTS_DRAGONS));

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = REQUIRED_MOD.get(mixinClassName.substring(PACKAGE.length()));
        // Mixins are set up before mods are constructed, so ask FML's early mod list directly.
        return modId == null || LoadingModList.get().getModFileById(modId) != null;
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
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
