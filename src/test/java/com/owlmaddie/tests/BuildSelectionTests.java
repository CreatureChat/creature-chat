// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.tests;

import com.owlmaddie.buildrec.BuildRecorder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BuildSelectionTests {
    @Test
    public void buildsExistForAllTiersAndLevels() {
        double[] heights = {
                4.0,    // ghast
                3.5,    // wither
                2.9,    // warden
                2.7,    // iron golem
                1.8,    // player reference
                1.75,   // sniffer (adult)
                1.7,    // creeper
                1.4,    // cow / mooshroom
                0.6875, // chicken
                0.3     // silverfish
        };
        for (double h : heights) {
            for (int level = 1; level <= 5; level++) {
                String file = BuildRecorder.randomBuildFile(h, null, level);
                assertNotNull(file, "missing build for height=" + h + " level=" + level);
            }
        }
    }

    @Test
    public void houseBuildsCoverAllLevels() {
        double h = 0.9; // tier 1 covers all house levels
        for (int level = 1; level <= 5; level++) {
            String file = BuildRecorder.randomBuildFile(h, "house", level);
            assertNotNull(file, "missing house build for level=" + level);
        }
    }

    @Test
    public void gardenBuildsCoverAllLevels() {
        double h = 1.5; // any height works
        for (int level = 1; level <= 5; level++) {
            String file = BuildRecorder.randomBuildFile(h, "garden", level);
            assertNotNull(file, "missing garden build for level=" + level);
        }
    }
}
