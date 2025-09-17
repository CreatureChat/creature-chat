package com.owlmaddie.network.S2C;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;

import java.util.UUID;

public record AuthRequestPayload(UUID requestId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AuthRequestPayload> PACKET_ID = new CustomPacketPayload.Type<>(NetworkingConstants.PACKET_S2C_AUTH_REQUEST);
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthRequestPayload> PACKET_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AuthRequestPayload::requestId,
            AuthRequestPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}

