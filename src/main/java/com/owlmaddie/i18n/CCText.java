// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.i18n;

import java.util.List;

/**
 * Central translation buckets for CreatureChat.
 */
public class CCText {
    // UI text
    public static final TR UI_CHAT_TITLE = new TR("ui.chat_title", "CreatureChat");
    public static final TR UI_ENTER_MESSAGE = new TR("ui.enter_message", "Enter your message:");
    public static final List<TR> UI_TEXT = List.of(
            UI_CHAT_TITLE,
            UI_ENTER_MESSAGE
    );

    // Configuration command text
    public static final TR CONFIG_CHATBUBBLE_ENABLED = new TR("config.chatbubble.enabled", "Player chat bubbles have been enabled.");
    public static final TR CONFIG_CHATBUBBLE_DISABLED = new TR("config.chatbubble.disabled", "Player chat bubbles have been disabled.");
    public static final TR CONFIG_CHATBUBBLE_UPDATE_FAILED = new TR("config.chatbubble.update_failed", "Failed to update player chat bubble setting.");
    public static final TR CONFIG_HELP = new TR("config.help",
            "Commands:\n" +
            " /creaturechat key set <value> [--config default|server]\n" +
            " /creaturechat url set <value> [--config default|server]\n" +
            " /creaturechat model set <value> [--config default|server]\n" +
            " /creaturechat timeout set <value> [--config default|server]\n" +
            " /creaturechat story set <value> [--config default|server]\n" +
            " /creaturechat story clear [--config default|server]\n" +
            " /creaturechat story show\n" +
            " /creaturechat whitelist <entity|all|clear> [--config default|server]\n" +
            " /creaturechat blacklist <entity|all|clear> [--config default|server]\n" +
            " /creaturechat chatbubble set <on|off> [--config default|server]\n" +
            "Optional: Append [--config default | server] to any command to specify configuration scope.\n\n" +
            "Security: Level 4 permission required.");
    public static final TR CONFIG_STORY_SET_SUCCESS = new TR("config.story.set_success", "Story set successfully: %s");
    public static final TR CONFIG_STORY_SET_FAILED = new TR("config.story.set_failed", "Failed to set story!");
    public static final TR CONFIG_STORY_CLEARED_SUCCESS = new TR("config.story.cleared_success", "Story cleared successfully!");
    public static final TR CONFIG_STORY_CLEARED_FAILED = new TR("config.story.cleared_failed", "Failed to clear story!");
    public static final TR CONFIG_STORY_NOT_SET = new TR("config.story.not_set", "No story is currently set.");
    public static final TR CONFIG_STORY_SHOW = new TR("config.story.current", "Current story: %s");
    public static final TR CONFIG_INVALID_SETTING_TYPE = new TR("config.invalid_setting_type", "Invalid type for setting %s");
    public static final TR CONFIG_UNKNOWN_SETTING = new TR("config.unknown_setting", "Unknown configuration setting: %s");
    public static final TR CONFIG_TIMEOUT_INVALID_TYPE = new TR("config.timeout_invalid_type", "Invalid type for timeout, must be Integer.");
    public static final TR CONFIG_SETTING_SET_SUCCESS = new TR("config.setting.set_success", "%s Set Successfully!");
    public static final TR CONFIG_SETTING_SET_FAILED = new TR("config.setting.set_failed", "%s Set Failed!");
    public static final TR CONFIG_LIST_UPDATE_SUCCESS = new TR("config.list.update_success", "Successfully updated %s with %s");
    public static final TR CONFIG_LIST_UPDATE_FAILED = new TR("config.list.update_failed", "Failed to update %s");
    public static final List<TR> CONFIG_TEXT = List.of(
            CONFIG_CHATBUBBLE_ENABLED,
            CONFIG_CHATBUBBLE_DISABLED,
            CONFIG_CHATBUBBLE_UPDATE_FAILED,
            CONFIG_HELP,
            CONFIG_STORY_SET_SUCCESS,
            CONFIG_STORY_SET_FAILED,
            CONFIG_STORY_CLEARED_SUCCESS,
            CONFIG_STORY_CLEARED_FAILED,
            CONFIG_STORY_NOT_SET,
            CONFIG_STORY_SHOW,
            CONFIG_INVALID_SETTING_TYPE,
            CONFIG_UNKNOWN_SETTING,
            CONFIG_TIMEOUT_INVALID_TYPE,
            CONFIG_SETTING_SET_SUCCESS,
            CONFIG_SETTING_SET_FAILED,
            CONFIG_LIST_UPDATE_SUCCESS,
            CONFIG_LIST_UPDATE_FAILED
    );
}
