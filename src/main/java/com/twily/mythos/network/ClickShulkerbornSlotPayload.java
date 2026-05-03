package com.twily.mythos.network;

import com.twily.mythos.Mythos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ClickShulkerbornSlotPayload(int slot, boolean secondary, boolean quickMove) implements CustomPacketPayload {

    public static final Type<ClickShulkerbornSlotPayload> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(Mythos.MOD_ID, "click_shulkerborn_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ClickShulkerbornSlotPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ClickShulkerbornSlotPayload::slot,
        ByteBufCodecs.BOOL,
        ClickShulkerbornSlotPayload::secondary,
        ByteBufCodecs.BOOL,
        ClickShulkerbornSlotPayload::quickMove,
        ClickShulkerbornSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
