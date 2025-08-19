// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.utils;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import java.util.Optional;

/**
 * 1.21.5: putUuid/getUuid were removed, so store as a long[] tag instead.
 */
public class NbtCompoundHelper {
    public static void putUuid(CompoundTag nbt, String key, UUID uuid) {
        nbt.putLongArray(key, new long[]{
                uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits()
        });
    }

    public static UUID getUuid(CompoundTag nbt, String key) {
        // Preferred format for 1.21.5+ worlds: long[2] array of msb/lsb
        Optional<long[]> opt = nbt.getLongArray(key);
        if (opt.isPresent()) {
            long[] data = opt.get();
            return new UUID(data[0], data[1]);
        }

        // Backwards compatibility: 1.21.4 and older stored UUIDs as an
        // IntArrayTag (4 ints) via NbtUtils#createUUID.  Attempt to read
        // that structure so that older worlds continue to load.
        Tag tag = nbt.get(key);
        if (tag instanceof IntArrayTag intTag) {
            int[] ints = intTag.getAsIntArray();
            if (ints.length == 4) {
                long msb = ((long) ints[0] << 32) | (ints[1] & 0xFFFFFFFFL);
                long lsb = ((long) ints[2] << 32) | (ints[3] & 0xFFFFFFFFL);
                return new UUID(msb, lsb);
            }
        }

        throw new IllegalStateException("Missing UUID tag: " + key);
    }

    public static boolean containsUuid(CompoundTag nbt, String key) {
        return nbt.contains(key);
    }
}
