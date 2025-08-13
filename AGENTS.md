# CreatureChat™ Agent Instructions

This fabric mod allows players to chat with any mob in Java Minecraft! All creatures can talk & react using AI!

## 🛠️ Build Options

Use `build.sh` to build the mod:

```bash
./build.sh                        # Build ALL versions
ONLY_VERSION=1.20.1 ./build.sh    # Build a SPECIFIC version
DRY_RUN=1 ./build.sh              # PREVIEW build details, commands, and dependencies (no changes)
ONLY_VERSION=1.20.1 DRY_RUN=1 ./build.sh  # PREVIEW a SPECIFIC version
```

> ⚠️ This script may temporarily modify `gradle.properties` and `fabric.mod.json`. Run it only when you're ready to test builds.

---

## 🧭 Coding Guidelines

- **Target Minecraft versions**: `1.20` to `1.21.7`. If `./build.sh` succeeds, then all versions are successfully built. 
- **Mappings**: Uses **official Mojang mappings** (not Yarn).
- **Simple & Precise**: Use surgical precision when editing (avoid unnecessary changes, keep things simple and clean).
- **Version-Specific**: If you there are API changes causing issues, use /vs/ folder to override specific versions. If this results in large files being duplicated, try and refactor out the broken functionality into a helper class, and then use /vs/ versions for your helper class.

---

## 📁 Folder Structure

| Path                    | Description                                |
|-------------------------|--------------------------------------------|
| `src/main`              | Core mod logic (shared / server-side)      |
| `src/client`            | Client-side code (UI, rendering, etc.)     |
| `src/test`              | LLM-powered unit tests                     |
| `src/vs/vX_Y_Z/`        | Version-specific overrides (if needed)     |

Not all Minecraft versions require a `src/vs/` folder. These folders contain targeted overrides applied **in order**, 
from oldest to newest, where the folder version is `<=` the Minecraft version being built. 
Later folders override files from earlier ones. 

NOTE: Try not to duplicate large files, and instead create helper classes 
which can be easily made version-specific.
