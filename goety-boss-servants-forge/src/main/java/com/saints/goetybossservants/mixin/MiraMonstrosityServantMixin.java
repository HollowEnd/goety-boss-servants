package com.saints.goetybossservants.mixin;

import com.Polarice3.Goety.api.entities.ally.IServant;
import com.saints.goetybossservants.entity.MiraMonstrosityServant;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.UUID;

/**
 * Makes {@link MiraMonstrosityServant} a Goety {@link IServant}.
 *
 * <p>IOwned.getOwnerId() returns a UUID, but meowhem's MiraMachineEntity already has an int
 * getOwnerId(), and javac won't let one class declare both. Mixin can: the "gbs$" prefixed method
 * below is renamed to getOwnerId()Ljava/util/UUID; next to the inherited getOwnerId()I. The rest of
 * the interface is implemented by name in MiraMonstrosityServant itself.
 */
@Mixin(value = MiraMonstrosityServant.class, remap = false)
@Implements(@Interface(iface = IServant.class, prefix = "gbs$", remap = Interface.Remap.NONE))
public abstract class MiraMonstrosityServantMixin {
    public UUID gbs$getOwnerId() {
        return ((MiraMonstrosityServant) (Object) this).getGoetyOwnerId();
    }
}
