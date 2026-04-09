package com.twily.mythos.myth;

import com.twily.mythos.Mythos;
import com.twily.mythos.registry.MythosAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public final class MythState {

    public static final Identifier NONE = Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "none");

    private MythState() {
    }

    public static Identifier get(Player player) {
        String raw = player.getData(MythosAttachments.CURRENT_MYTH);
        if (raw == null || raw.isBlank()) {
            return NONE;
        }

        try {
            return raw.contains(":")
                ? Identifier.parse(raw)
                : Identifier.fromNamespaceAndPath(Mythos.MOD_ID, raw);
        } catch (IllegalArgumentException ignored) {
            return NONE;
        }
    }

    public static boolean is(Player player, Identifier mythId) {
        return get(player).equals(mythId);
    }

    public static void set(Player player, Identifier mythId) {
        player.setData(MythosAttachments.CURRENT_MYTH, mythId.toString());
    }

    public static void clear(Player player) {
        set(player, NONE);
    }

    public static Component displayName(Identifier mythId) {
        return Component.translatable("myth." + mythId.getNamespace() + "." + mythId.getPath());
    }
}
