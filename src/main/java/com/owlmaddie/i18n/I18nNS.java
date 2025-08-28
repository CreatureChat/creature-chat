// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.i18n;

import static com.owlmaddie.ModInit.MODID;

import net.minecraft.network.chat.Component;

/**
 * Namespace helper for translation keys.
 */
public final class I18nNS {
    private I18nNS() {}

    public static String k(String path) {
        return MODID + "." + path;
    }

    public static Component tr(String path, String en, Object... args) {
        return Component.translatableWithFallback(k(path), en, args);
    }
}
