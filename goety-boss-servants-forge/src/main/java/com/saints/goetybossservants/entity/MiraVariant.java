package com.saints.goetybossservants.entity;

import net.minecraft.network.chat.Component;

/**
 * Meowhem picks the Obsidian / Warden look of the Mira Monstrosity purely from its name
 * ("Obsidian Monstrosity", "Warden Monstrosity"). Each variant here supplies that name as the
 * servant's type name, so every meowhem check (texture, glow, particles, sounds) picks it up while
 * a name tag still overrides it, exactly like on the boss.
 */
public enum MiraVariant {
    MIRA("mira"),
    OBSIDIAN("obsidian"),
    WARDEN("warden");

    private final String id;

    MiraVariant(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public Component getTypeName() {
        return switch (this) {
            // The literal English names are what meowhem compares against, whatever the game language.
            case OBSIDIAN -> Component.literal("Obsidian Monstrosity");
            case WARDEN -> Component.literal("Warden Monstrosity");
            default -> Component.translatable("entity.goetybossservants.mira_monstrosity");
        };
    }

    public static MiraVariant byId(String id) {
        for (MiraVariant variant : values()) {
            if (variant.id.equals(id)) {
                return variant;
            }
        }
        return MIRA;
    }

    public static MiraVariant byOrdinal(int ordinal) {
        MiraVariant[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : MIRA;
    }
}
