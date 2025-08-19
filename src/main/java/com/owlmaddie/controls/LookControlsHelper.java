// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.controls;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;

/**
 * Version-specific helper for {@link LookControls}.
 * <p>
 * In baseline versions, only {@link Ghast} is considered a ghast-like entity.
 * Newer Minecraft versions may override this class to include additional ghast
 * variants without duplicating the entire {@code LookControls} implementation.
 */
public class LookControlsHelper {
    public static boolean isGhast(Mob entity) {
        return entity instanceof Ghast;
    }
}
