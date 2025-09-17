package com.owlmaddie.network.C2S;

import com.owlmaddie.network.NetworkingConstants;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record AuthResponsePayload(UUID requestId, String apiKey) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AuthResponsePayload> PACKET_ID = new CustomPacketPayload.Type<>(NetworkingConstants.PACKET_C2S_AUTH_RESPONSE);
    public static final StreamCodec<RegistryFriendlyByteBuf, AuthResponsePayload> PACKET_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AuthResponsePayload::requestId,
            ByteBufCodecs.STRING_UTF8, AuthResponsePayload::apiKey,
            AuthResponsePayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}

