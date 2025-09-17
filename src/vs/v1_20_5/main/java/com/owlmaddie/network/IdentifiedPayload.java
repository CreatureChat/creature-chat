// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A generic payload that wraps an Identifier and a byte array for packet data. This is for
 * Minecraft 1.20.5+ versions, which switched to CustomPayload. This maintains compatibility
 * with the rest of CreaturePal code.
 */
public record IdentifiedPayload(ResourceLocation id, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<IdentifiedPayload> PACKET_ID =
            new CustomPacketPayload.Type<>(new ResourceLocation("creaturepals", "identified_payload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, IdentifiedPayload> PACKET_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, IdentifiedPayload::id,
                    ByteBufCodecs.BYTE_ARRAY, IdentifiedPayload::data,
                    IdentifiedPayload::new
            );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}