package com.saints.goetybossservants.mixin;

import com.saints.goetybossservants.entity.CaptainCorneliaServant;
import com.saints.goetybossservants.entity.servant.BossServant;
import dev.obscuria.aquamirae.common.entity.CaptainCorneliaBoss;
import dev.obscuria.aquamirae.server.event.ShipbreakerEvent;
import dev.obscuria.fragmentum.api.network.FragmentumNetworking;
import dev.obscuria.fragmentum.api.network.PacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Stream;

/**
 * Aquamirae's own Captain Cornelia code, adjusted at runtime for the servant (CaptainCorneliaServant);
 * the boss is unchanged. rage() and equip() are Aquamirae's own method names; baseTick overrides a
 * vanilla method, so it's listed by its dev and SRG names ("m_6075_"), one of which matches.
 */
@Mixin(value = CaptainCorneliaBoss.class, remap = false)
public abstract class CaptainCorneliaMixin {
    /**
     * Her rage sends the Shipbreaker through the area: a ghost ship that kills everything in its
     * path. The servant rages without it.
     */
    @Redirect(method = "rage", at = @At(value = "INVOKE", target = "Ldev/obscuria/aquamirae/server/event/ShipbreakerEvent;trigger(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;)Ldev/obscuria/aquamirae/server/event/ShipbreakerEvent;"))
    private ShipbreakerEvent goetybossservants$noShipbreaker(ServerLevel level, Vec3 pos) {
        return (Object) this instanceof BossServant ? null : ShipbreakerEvent.trigger(level, pos);
    }

    /** Nor does it start the boss encounter for nearby players. */
    @Redirect(method = "rage", at = @At(value = "INVOKE", target = "Ldev/obscuria/fragmentum/api/network/FragmentumNetworking;sendTo(Lnet/minecraft/server/level/ServerPlayer;Ldev/obscuria/fragmentum/api/network/PacketPayload;)V"))
    private void goetybossservants$noEncounter(FragmentumNetworking networking, ServerPlayer player, PacketPayload payload) {
        if (!((Object) this instanceof BossServant)) {
            networking.sendTo(player, payload);
        }
    }

    /**
     * While she levitates in a rage she spins everything nearby with 100 health or less around her;
     * the list is built with the only toList() in baseTick. For a servant, leave out everyone her
     * other area attacks spare (players, pets, player-owned servants, allies).
     */
    @Redirect(method = {"baseTick", "m_6075_"}, at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;toList()Ljava/util/List;"), require = 1, allow = 1)
    private List<LivingEntity> goetybossservants$spareAllies(Stream<LivingEntity> nearby) {
        if ((Object) this instanceof BossServant servant) {
            return nearby.filter(entity -> !BossServant.sparedByAreaAttacks(servant, entity)).toList();
        }
        return nearby.toList();
    }

    /**
     * Aquamirae equips a weapon loadout when she spawns and halfway through her switch animation
     * (a weighted random pick). The servant equips the weapon it chose instead (its WeaponLoadout
     * parameter is Aquamirae's private record, hence @Coerce).
     */
    @Inject(method = "equip", at = @At("HEAD"), cancellable = true)
    private void goetybossservants$chosenWeapon(@Coerce Object loadout, CallbackInfo ci) {
        if ((Object) this instanceof CaptainCorneliaServant servant) {
            servant.equipChosenWeapon();
            ci.cancel();
        }
    }
}
