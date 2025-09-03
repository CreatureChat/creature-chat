// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.tests;

import com.owlmaddie.goals.LeadTarget;
import com.owlmaddie.goals.LeadTargetParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LeadTargetParserTests {
    @Test
    public void parsesStructureWithoutPrefix() {
        LeadTarget target = LeadTargetParser.parse("village").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.STRUCTURE, target.getType());
        assertEquals("village", target.getPrimary());
    }

    @Test
    public void parsesStructureWithPrefixKeyword() {
        LeadTarget target = LeadTargetParser.parse("structure:village").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.STRUCTURE, target.getType());
        assertEquals("village", target.getPrimary());
    }

    @Test
    public void parsesBiomeWithPrefix() {
        LeadTarget target = LeadTargetParser.parse("biome:jungle").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.BIOME, target.getType());
        assertEquals("jungle", target.getPrimary());
    }

    @Test
    public void parsesTagWithHash() {
        LeadTarget target = LeadTargetParser.parse("#is_badlands").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.TAG, target.getType());
        assertEquals("is_badlands", target.getPrimary());
    }

    @Test
    public void parsesTagWithHashJungle() {
        LeadTarget target = LeadTargetParser.parse("#is_jungle").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.TAG, target.getType());
        assertEquals("is_jungle", target.getPrimary());
    }

    @Test
    public void parsesTagWithoutIsPrefix() {
        LeadTarget target = LeadTargetParser.parse("#jungle").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.TAG, target.getType());
        assertEquals("is_jungle", target.getPrimary());
    }

    @Test
    public void parsesTagWithPrefix() {
        LeadTarget target = LeadTargetParser.parse("tag:is_forest").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.TAG, target.getType());
        assertEquals("is_forest", target.getPrimary());
    }

    @Test
    public void parsesPoiWithPrefix() {
        LeadTarget target = LeadTargetParser.parse("poi:home").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.POI, target.getType());
        assertEquals("home", target.getPrimary());
    }

    @Test
    public void parsesNamespacedTag() {
        LeadTarget target = LeadTargetParser.parse("#minecraft:is_badlands").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.TAG, target.getType());
        assertEquals("minecraft:is_badlands", target.getPrimary());
    }

    @Test
    public void parsesBiomeWithHashPrefix() {
        LeadTarget target = LeadTargetParser.parse("#biome:jungle").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.BIOME, target.getType());
        assertEquals("jungle", target.getPrimary());
    }

    @Test
    public void parsesStructureWithHashPrefix() {
        LeadTarget target = LeadTargetParser.parse("#structure:village").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.STRUCTURE, target.getType());
        assertEquals("village", target.getPrimary());
    }

    @Test
    public void parsesStructureTag() {
        LeadTarget target = LeadTargetParser.parse("structure:#village").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.TAG, target.getType());
        assertEquals("village", target.getPrimary());
    }

    @Test
    public void parsesResourceWithPrefix() {
        LeadTarget target = LeadTargetParser.parse("resource:ancient_debris").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.RESOURCE, target.getType());
        assertEquals("ancient_debris", target.getPrimary());
    }

    @Test
    public void parsesStructureWithSubtype() {
        LeadTarget target = LeadTargetParser.parse("stronghold:portal_room").orElse(null);
        assertNotNull(target);
        assertEquals(LeadTarget.Type.STRUCTURE, target.getType());
        assertEquals("stronghold", target.getPrimary());
        assertEquals("portal_room", target.getSecondary());
    }

}

