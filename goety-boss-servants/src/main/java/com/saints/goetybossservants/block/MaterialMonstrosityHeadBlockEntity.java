package com.saints.goetybossservants.block;

import com.saints.goetybossservants.entity.monstrosity.MonstrosityMaterial;
import com.saints.goetybossservants.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class MaterialMonstrosityHeadBlockEntity extends BlockEntity {
    @Nullable
    private UUID ownerId;
    @Nullable
    private String ownerName;
    @Nullable
    private String customName;
    private MonstrosityMaterial material = MonstrosityMaterial.COPPER;

    public MaterialMonstrosityHeadBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.MATERIAL_MONSTROSITY_HEAD_BE, pos, state);
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Nullable
    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwnerName(@Nullable String ownerName) {
        this.ownerName = ownerName;
    }

    @Nullable
    public String getCustomName() {
        return this.customName;
    }

    public void setCustomName(@Nullable String customName) {
        this.customName = customName;
    }

    public MonstrosityMaterial getMaterial() {
        return this.material;
    }

    public void setMaterial(MonstrosityMaterial material) {
        this.material = material;
        this.setChanged();
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.ownerName = tag.contains("owner_name") ? tag.getString("owner_name") : null;
        this.customName = tag.contains("mod_custom_name") ? tag.getString("mod_custom_name") : null;
        this.material = MonstrosityMaterial.byId(tag.getString("material"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.ownerId != null) {
            tag.putUUID("Owner", this.ownerId);
        }
        if (this.ownerName != null) {
            tag.putString("owner_name", this.ownerName);
        }
        if (this.customName != null && !this.customName.isEmpty()) {
            tag.putString("mod_custom_name", this.customName);
        }
        tag.putString("material", this.material.getId());
    }

    // The client needs the material to pick the head texture.
    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
