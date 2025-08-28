# Changelog

All notable changes to **CreatureChat™** are documented in this file. The format is based on 
[Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres to 
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added
- Document SPDX header and changelog requirements in AGENTS.md for contributors

### Changed
- Convert PNG screenshots to JPEG, compress, and remove less useful ones (smaller jar)
- Compressed all textures from 32-bit color to 4-bit indexed color, reduced size massively.


## [3.0.0] - 2025-08-27

### Added
- **Inventories** for all mobs with chat data (Shift+Right Click, or Press 'E' while riding)
    - Random loot added to inventory on character creation (biome-specific loot tables for inventories)
    - You can borrow items from friends, or steal items from enemies
    - Entities will react to changes in inventory
    - Integrates with existing inventories for Piglin, Pillager, Allay, and Villager
    - Press 'E' while riding a mob with max friendship to show inventory
    - Imports chest-based mobs (donkey with chest, llama with chest, etc...)
    - Integrated Fabric DataGen for building compatible loot-tables (1.20 to 1.21.7 support)
    - Assign inventory into random slots
    - Main/Off hand support for max friendship (uses bottom left 2 slots for main/off hand)
- **Advancements** (30 tasks, goals, and challenges)
    - **CreatureChat** (root): Your world just got way more alive.
    - **Ice Breaker**: Cold Open.
    - **First Impressions**: Make a friend.
    - **No Hard Feelings**: Regain a friend.
    - **Open Sesame**: Your stuff is my stuff.
    - **Tag Along**: Follow me, bro.
    - **Lead The Way**: Where are we going?
    - **Calm The Storm**: Chill out, man.
    - **Stand Your Ground**: Why are you running?
    - **Sworn Oath**: I will protect you.
    - **Wordsmith**: From rocky start to best friends.
    - **True Companion**: Did we just become best friends?
    - **Sleight of Hand**: Try my sword.
    - **Shared Stash**: Share the loot.
    - **Social Butterfly**: Love conquers all.
    - **Inner Circle**: Gathered round the fire.
    - **Popular Opinion**: Sway the Crowd.
    - **Drama Llama**: Best friends for never.
    - **Love Hate Relationship**: It’s Complicated.
    - **Arch Nemesis**: Keep your enemies closer.
    - **Friend Or Foe**: Remember the good times?
    - **Finder’s Keepers**: Borrowed forever.
    - **Guided Tour**: Lost, together.
    - **The NeverEnding Story**: Every real story is a never ending story.
    - **Grand Gesture**: You got rizz.
    - **A Legend**: <HIDDEN>
    - **Potato War**: <HIDDEN>
    - **The Heist**: The perfect crime.
    - **Ender Escort**: Together to the End.
    - **Pacifist Route**: The best ending.
- **Translations** (18 Languages):
  - Translating all visible text (Advancements, ChatScreen, Inventory, Commands, Errors, No Responses)
  - Adding DataGen provider for translations (en_us.json)
  - Languages:
      - German (Germany): `de_de`
      - Spanish (Spain): `es_es`
      - Spanish (Mexico): `es_mx`
      - French (France): `fr_fr`
      - Hindi (India): `hi_in`
      - Japanese (Japan): `ja_jp`
      - Korean (South Korea): `ko_kr`
      - Dutch (Netherlands): `nl_nl`
      - Polish (Poland): `pl_pl`
      - Portuguese (Brazil): `pt_br`
      - Portuguese (Portugal): `pt_pt`
      - Swedish (Sweden): `sv_se`
      - Turkish (Turkey): `tr_tr`
      - Ukrainian (Ukraine): `uk_ua`
      - Russian (Russia): `ru_ru`
      - Chinese (Simplified, China): `zh_cn`
      - Chinese (Traditional, Taiwan): `zh_tw`
      - Indonesian (Indonesia): `id_id`
- Solutions to common errors are now displayed on screen (i.e. more helpful)
- New HTTP keep alive and accept HTTP headers
- Unit tests for all LLM request failure scenarios + solutions
- Validation of mixin targets (on build)

### Changed
- Expanded & translated '<no response>' messages
- Improved rate-limits for automatic LLM requests (show item, inventory, attack)
    - 10 automatic LLM messages per user, cooldown +1 every 3 seconds
    - 3 automatic LLM message per entity, cooldown +1 every 3 seconds
- Adding specific **error messages** for specific LLM request status codes (i.e. more helpful)
- Output more detailed error message from LLM APIs
- Leaving HTTP connection open (better connection pooling)

### Fixed
- Fixed constant death messages which appeared on each attack (for Minecraft 1.21.2+)
- Line wrapping for all languages improved (especially noticeable for certain wide-character languages)
- Support NBT format from 1.20 to 1.21.4 in newer versions (migrate CCUID)
- Fixed "entity is null" errors when killing mobs (usually when killing lots of mobs)
- Fixed names that were not appearing in the End and the Nether.
- Fixed Z-fighting on message text and entity name (on chat bubble)
- Fixed PlayerData from using UUID (changed back to PlayerName - to allow for roleplay)

## [2.5.0] - 2025-07-07

### Added
- Support for Minecraft 1.21.5, 1.21.6, and 1.21.7
- Initial NeoForge support for Minecraft 1.21.1
- New icons: Happy Ghast, Pig (cold & warm variants)

### Changed
- Major refactor to support Minecraft 1.21.5+ (new NBT format, rendering pipeline, and updated APIs)
- Migrated to official Mojang mappings due to missing Yarn support in 1.21.5+ (huge internal rewrite)
- Updated font color to `0xFFFFFFFF` (now ARGB as required by 1.21.6)
- Changed `serverLevel()` to `level()` for broader version compatibility
- Switched `FlyingMob` to `FlyingAnimal`, and added Phantom entity

### Fixed
- Removed duplicate log messages for missing textures
- Fixed broken icons: Chicken (temperate), Cow (cold, temperate, warm)
- Fixed HappyGhast behavior in LookControls (supports lead/follow logic)
- Disabled `validateAccessWidener` during build to prevent build failures

## [2.0.0] - 2025-06-30

### Added
- Compatibility with Minecraft 1.20.5, 1.20.6, 1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4
- New Chat UI (with image buttons + hover + new positioning)
- Creaking support for spawn and despawn without loosing chat history
- Added new entity icons (armadillo, bogged, creaking, breeze, and wolves)
- Wither now drops a Nether Star at max friendship (for pacifists)
- Added Entity Maturity (baby or adult) into system-chat prompt
- Added many new speaking styles (minimalist, nerdy, stupid, gen-z, old timer, boomer, etc...)
- Check friendship direction (+ or -) in LLM unit tests (to verify friendship is output correctly)
- Added LLM Comparison HTML Output (for human eval of different LLMs with CreatureChat)
- Rate limiter for LLM unit tests (to prevent rate limit issues from certain providers when running all tests)
- Include all markdown files in JAR (LICENSE.md, LICENSE-ASSETS.md, TERMS.md, and so on)

### Fixed
- Bees no longer forget their chat data when entering/leaving hives (writeNbt & readNbt modified)
- Vexes no longer take damage when chat data exists
- Wandering Trader no longer despawns if it has chat data
- Removed randomized error messages from chat history (so it doesn't break the chat history when errors are shown)
- Reduced death message output in logs to use DEBUG log level
- Fixed unit tests for friendship (some were being skipped)

### Changed
- Updated hundreds of entity chat icons (updating color palette and style, new license for non-code: CC-BY-NC-SA-4.0)
- Simplified system-chat prompt (less tokens), rounded health & hunger values, and improved variety of examples (less tokens)
- Improved error handling to prevent broken "..." pending chat status. (HTTP and message processing is more protected)
- Broadcasting and receiving chat messages now ignores if the UUID is valid (to keep data synced)
- Removed a few variables from the chat context (creative mode, hardcore, difficulty)
- Replacing isIndirect() usage with a more generic version
- Replacing teleport() usage with a more generic override (more compatible with later versions of Minecraft)
- Refactored render methods (vertex, bufferBuilder, Tessellator, getTexture) into QuadBuffer class
- Refactored squid swimming (helper method, interface)
- Refactored damage functions (onDamage, and applying damage)
- Refactored "Use Item" methods (show item & use item)
- Updated docs & asset licensing to use CC-BY-NC-SA-4.0 and added SPDX headers to all source-code files
- Integrated reuse licensing checking into build pipeline, to ensure 100% coverage of copyright and licensing info
- Updated [TERMS](TERMS.md) with new section related to AI generated content, and updated eligibility and licenses sections.
- Improved LLM unit tests to check for both a positive and negative behaviors (i.e. FOLLOW and not LEAD, ATTACK and not FLEE, etc...)
- Updated Gradle to 8.12 (fabric-loom to 1.10.1)

## [1.3.0] - 2025-01-14

### Added
- In-game chat messages are now displayed in chat bubbles above players heads!
- Custom player icons (icons can be embedded in player skin file)
  - Step-by-Step **Icon** Tutorial: [ICON.md](ICONS.md)
  - Mixin to extend PlayerSkinTexture to make a copy of the NativeImage + pixel toggle to enable
- New command `/creaturechat chatbubbles set <on | off>` to show or hide player chat messages in bubbles
- Improved LLM Unit tests (to prevent rate limit issues from certain providers when running all tests)
  - Check friendship direction (+ or -) in LLM unit tests (to verify friendship direction is output correctly)

### Changed
- Seperated Player and Entity message broadcasts (different packets for simplicity)
- Reduced size of player skin face on chat bubble, to match sizes of custom icons (for consistency)
- Updated entity icons for allay, creeper, and pig

### Fixed
- Hide death messages for mobs with no chat data
- Fixed transparent background behind chat screen for Minecraft 1.20 and 1.20.1.
- Removed extra message broadcast (which was unnecessary)

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
- Name tags were hidden for entities with no character sheet (they are now rendered)

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
