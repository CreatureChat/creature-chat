# Changelog

All notable changes to **CreatureChat** are documented in this file. The format is based on 
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to 
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Changed
- Large refactor to use ONLY emojis as behaviors: ❤️💔👣🐕🏃‍️🛡️⚔️🚫. Save on tokens, and simplifies behavior support for smaller LLMs.

## [1.2.1] - 2025-01-01

### Changed
- Refactor of EntityChatData constructor (no need for playerName anymore)
- Improved LLM / AI Options in README.md (to more clearly separate free and paid options)
- Improved LLM unit tests for UNFLEE (trying to prevent failures for brave archer)

### Fixed
- Fixed a bug which broadcasts too many death messages (any mob with a custom name). Now it must also have a character sheet.
- Prevent crash due to missing texture when max friend/enemy + right click on entity
- Fixed bug which caused a max friend to interact with both off hand + main hand, causing both a message + riding (only check main hand now)
- Hide auto-generated messages from briefly appearing from the mob (i.e. interact, show, attack, arrival)

## [1.2.0] - 2024-12-28

### Added
- New friendship particles (hearts + fire) to indicate when friendship changes
- Added sound effects for max friendship and max enemy
- New follow, flee, attack, lead, and protect particles & sound effects (for easy confirmation of behaviors)
- New animated lead particle (arrows pointing where they are going)
- New animated attack particles (with random # of particles)
- New sounds and particles when max friendship with EnderDragon (plus XP drop)
- New `/creaturechat story` command to customize the character creation and chat prompts with custom text.

### Changed
- Entity chat data now separates friendship by player and includes timestamps
- When entity conversations switch players, a message is added for clarity (so the entity knows a new player entered the conversation)
- Data is no longer deleted on entity death, and instead a "death" timestamp is recorded
- Removed "pirate" speaking style and a few <non-response> outputs
- Passive entities no longer emit damage particles when attacking, they emit custom attack particles
- Protect now auto sets friendship to 1 (if <= 0), to prevent entity from attacking and protecting at the same time
- Seperated `generateCharacter()` and `generateMessage()` functions for simplicity
- Fixing PACKET_S2C_MESSAGE from crashing a newly logging on player, if they receive that message first.
- Added NULL checks on client message listeners (to prevent crashes for invalid or uninitialized clients)
- Broadcast ALL player friendships with each message update (to keep client in sync with server)

### Fixed
- Fixed a regression caused by adding a "-forge" suffix to one of our builds
- Do not show auto-generated message above the player's head (you have arrived, show item, etc...)

## [1.1.0] - 2024-08-07

### Added
- New LEAD behavior, to guide a player to a random location (and show message when destination is reached)
- Best friends are now rideable! Right click with an empty hand. Excludes tameable entities (dogs, cats, etc...)
- Villager trades are now affected by friendship! Be nice!
- Automatically tame best friends (who are tameable) and un-tame worst enemies!
- Added FORGE deployment into automated deploy script

### Changed
- Improved character creation with more random classes, speaking styles, and alignments.
- Large refactor of how MobEntity avoids targeting players when friendship > 0
- Updated LookControls to support PhantomEntity and made it more generalized (look in any direction)
- Updated FLEE behavior Y movement speed
- Updated unit tests to add new LEAD tests
- Updated README.md to include HTML inside spoiler instructions, and whitelist/blacklist commands

### Fixed
- Entity persistence is now fixed (after creating a character sheet). No more despawning mobs.
- Fixed consuming items when right-clicking on chat bubbles (with something in your hand)
- Fixed crash when PROTECT behavior targets another player
- Fixed error when saving chat data while generating a new chat message

## [1.0.8] - 2024-07-16

### Added
- New **whitelist / blacklist** Minecraft **commands**, to show and hide chat bubbles based on entity type
- New **S2C packets** to send whitelist / blacklist changes on login and after commands are executed
- Added **UNFLEE behavior** (to stop fleeing from a player)
- Added support for **non path aware** entities to **FLEE** (i.e. Ghast)
- Added **new LLM tests** for UNFLEE

### Changed
- Chat Bubble **rendering** & interacting is now dependent on **whitelist / blacklist** config
- Improved client **render performance** (only query nearby entities every 3rd call)
- Fixed a **crash with FLEE** when non-path aware entities (i.e. Ghast) attempted to flee.
- Updated ATTACK **CHARGE_TIME** to be a little **faster** (when non-native attacks are used)
- Extended **click sounds** to 12 blocks away (from 8)
- Fixed certain **behaviors** from colliding with others (i.e. **mutual exclusive** ones)
- Updated README.md with new video thumbnail, and simplified text, added spoiler to install instructions
- Large **refactor** of Minecraft **commands** (and how --config args are parsed)
- Fixed **CurseForge deploy script** to be much faster, and correctly lookup valid Type and Version IDs

## [1.0.7] - 2024-07-03

### Added
- New **PROTECT** behavior: defend a player from attacks
- New **UNPROTECT** behavior: stop defending a player from attacks
- **Native ATTACK abilities** (when using the attack or protect behaviors for hostile mob types)
- **Free The End** triggered by max friendship with the **EnderDragon**!
- Added `PlayerBaseGoal` class to allow **goals/behaviors** to **continue** after a player **respawns** / logs out / logs in

### Changed
- Improved **FLEE** behavior, to make it more reliable and more random.
- Improved **FOLLOW** behavior, support **teleporting** entities (*Enderman, Endermite, and Shulker*)
- Refactored **ATTACK** behavior to allow more flexibility (in order to support PROTECT behavior)
- When chat bubble is **hidden**, do **not shorten** long names
- Updated `ServerEntityFinder::getEntityByUUID` to be more generic and so it can find players and mobs.

## [1.0.6] - 2024-06-17

### Added
- **Naturalist** mod **icon art** and full-support for all entities, expect snails (owlmaddie)
- New **Prompt Testing** module, for faster validation of LLMs and prompt changes
- New `stream = false` parameter to HTTP API requests (since some APIs default to `true`)

### Changed
- **Improvements** to **chat prompt** for more *balanced* dialog and *predictable* behaviors
- Improved **Behavior regex** to include both `<BEHAVIOR arg>` and `*BEHAVIOR arg*` syntax, and ignore unknown behaviors.
- Expanded regex to support args with `+` sign (i.e. `<FRIENDSHIP +1>`) and case-insensitive
- Improved **message cleaning** to remove any remaining `**` and `<>` after parsing behaviors
- Privacy Policy updated

## [1.0.5] - 2024-05-27

### Added
- New automated deployments for Modrinth and CurseForge (GitLab CI Pipeline)
- Death messages added for all named creatures except players and tamed ones (RIP)
- Added Minecraft Forge installation instructions
- Alex's Mobs icon art and full-support for all entities (owlmaddie)

### Fixed
- Fabulous video bug causing chat bubbles to be invisible
- Shader support (i.e. Iris, etc...) for text and rendering
- Water blocking render of chat bubbles
- Parse OpenAI JSON error messages, to display a more readable error message
- Remove quotes from CreatureChat API error messages
- If OpenAI key is set, switch URL automatically back to OpenAI endpoint

## [1.0.4] - 2024-05-15

### Added
- Doubled the number of character personality traits (to balance things out) 
- Added new `/creaturechat timeout set <seconds>` command
- Added support for commands to use different data types (`String`, `Integer`)

### Changed
- All buckets are now ignored for item-giving detection (since the entity is despawned immediately)
- Item giving is now aware if the entity accepts the item. It uses either "shows" or "gives" in the prompt now.
- Updated error messages to `RED` color for maximum attention
- Updated `/creaturechat help` output
- Updated `README.md` with new command documentation

### Fixed
- Bucketing a creature now maintains chat history when respawned
- Chats broken when OS locale is non-English language (i.e. `assistant to ass\u0131stant`)

## [1.0.3] - 2024-05-10

### Changed
- Simplified saving of chat data (no more renaming files)
- If chat data fails to save, send message to all ops (first auto-save happens at 1 minute after launching)
- If /creaturechat commands fail to save, send message to all ops (fail loudly) and display RED error message

## [1.0.2] - 2024-05-07

### Added
- Added support for Minecraft 1.20, 1.20.1, 1.20.2, 1.20.3, and 1.20.4 (new build pipeline)

### Changed
- Replaced calls to getLiteralString() with getString() for wider compatability

## [1.0.1] - 2024-05-06

### Added
- Added support for CreatureChat API (switch URL when a CreatureChat key is detected)
- Upgrade to Fabric Loader: `0.15.10`, Fabric API: `0.97.0+1.20.4`, Fabric Loom: `1.6.5`, Gradle: `8.6`
- New TERMS.md and PRIVACY.md documents (terms of service and privacy policy for CreatureChat)

### Changed
- Improved error messages onscreen (Invalid API Key, No tokens remaining, etc...), for improved troubleshooting
- Improved privacy by not outputting API keys in logs or onscreen
- Updated README.md with improved instructions and info

## [1.0.0] - 2024-05-01

### Added
- First release of CreatureChat
- Dynamic Dialogues: Enables chat interaction with unique Minecraft creatures.
- AI-Generated Chats: Uses LLM for dynamic conversations.
- Custom Behaviors: Creatures can follow, flee, or attack.
- Reactions: Automatic reactions to damage or item reception.
- Friendship Status: Tracks relationships on a scale from friend to foe.
- Auto Name Tag: All characters receive unique name tags (when interacted with).
- Custom UI Artwork: Includes hand-drawn entity icons and chat bubbles.
- Multi-Player Interaction: Syncs conversations across server and players.
- Personalized Memory: Creatures recall past interactions.
- Model Support: Supports various GPT versions and open-source LLM models.
- Commands: Easily set your API key, model, and url to connect your LLM.
- Language Support: Supports all Minecraft-supported languages.
- Auto Save: Chat data is automatically saved periodically.
