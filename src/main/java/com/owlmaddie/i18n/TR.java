// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.i18n;

import net.minecraft.network.chat.MutableComponent;

/**
 * Simple translation record.
 */
public record TR(String path, String en) {
    public MutableComponent comp(Object... args) {
        return (MutableComponent) I18nNS.tr(path, en, args);
    }

    public String key() {
        return I18nNS.k(path);
    }
}
