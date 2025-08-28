#!/usr/bin/env bash
set -euo pipefail

ONLY_VERSION=${ONLY_VERSION:-}
DRY_RUN=${DRY_RUN:-0}
ALLOW_GRADLE_RESTART=${ALLOW_GRADLE_RESTART:-}

# Allow “pattern→empty” instead of “pattern→itself”
shopt -s nullglob

run_build() {
  ./gradlew runDatagen </dev/null
  ./gradlew build -x test -x validateAccessWidener --build-cache --parallel </dev/null && return 0
  [[ -n "$ALLOW_GRADLE_RESTART" ]] || return 1
  echo "Gradle failed; attempting to clear locks and retry..."
  ./gradlew --stop >/dev/null 2>&1 || true
  find .gradle ~/.gradle -type f -name '*.lock' -delete 2>/dev/null || true
  ./gradlew runDatagen </dev/null
  ./gradlew build -x test -x validateAccessWidener --build-cache --parallel </dev/null
}

# Format: minecraft_version  yarn_mappings       loader_version  loom_version      fabric_version
VERSIONS=$(cat <<'EOF'
1.20    1.20+build.1       0.17.2    1.11-SNAPSHOT   0.83.0+1.20
1.20.1  1.20.1+build.10    0.17.2    1.11-SNAPSHOT   0.92.1+1.20.1
1.20.2  1.20.2+build.4     0.17.2    1.11-SNAPSHOT   0.91.6+1.20.2
1.20.3  1.20.3+build.1     0.17.2    1.11-SNAPSHOT   0.91.1+1.20.3
1.20.4  1.20.4+build.3     0.17.2    1.11-SNAPSHOT   0.97.0+1.20.4
1.20.5  1.20.5+build.1     0.17.2    1.11-SNAPSHOT   0.97.8+1.20.5
1.20.6  1.20.6+build.3     0.17.2    1.11-SNAPSHOT   0.100.8+1.20.6
1.21    1.21+build.9       0.17.2    1.11-SNAPSHOT   0.102.0+1.21
1.21.1  1.21.1+build.3     0.17.2    1.11-SNAPSHOT   0.116.3+1.21.1
1.21.2  1.21.2+build.1     0.17.2    1.11-SNAPSHOT   0.106.1+1.21.2
1.21.3  1.21.3+build.2     0.17.2    1.11-SNAPSHOT   0.114.1+1.21.3
1.21.4  1.21.4+build.8     0.17.2    1.11-SNAPSHOT   0.119.3+1.21.4
1.21.5  1.21.5+build.1     0.17.2    1.11-SNAPSHOT   0.128.1+1.21.5
1.21.6  1.21.6+build.1     0.17.2    1.11-SNAPSHOT   0.128.2+1.21.6
1.21.7  1.21.7+build.6     0.17.2    1.11-SNAPSHOT   0.128.2+1.21.7
EOF
)

while read -r mc_version yarn_mapping loader_version loom_version fabric_version; do
  [[ -z "$mc_version" || "$mc_version" =~ ^# ]] && continue
  [[ -n "$ONLY_VERSION" && "$ONLY_VERSION" != "$mc_version" ]] && continue

  echo "****"
  echo "Building for MC $mc_version  Fabric API $fabric_version"
  echo "****"

  if [[ "$DRY_RUN" == "1" ]]; then
    cat <<EOD
[DRY RUN] gradle.properties -> \
minecraft_version=$mc_version, \
yarn_mappings=$yarn_mapping, \
loader_version=$loader_version, \
loom_version=$loom_version, \
fabric_version=$fabric_version
[DRY RUN] fabric.mod.json -> "minecraft": "~$mc_version"
[DRY RUN] run './gradlew build -x test -x validateAccessWidener --build-cache --parallel'
[DRY RUN] download fabric-api-$fabric_version.jar from FabricMC
EOD
    echo
    continue
  fi

  sed -i \
    -e "s/^minecraft_version=.*/minecraft_version=$mc_version/" \
    -e "s/^yarn_mappings=.*/yarn_mappings=$yarn_mapping/" \
    -e "s/^loader_version=.*/loader_version=$loader_version/" \
    -e "s/^loom_version=.*/loom_version=$loom_version/" \
    -e "s/^fabric_version=.*/fabric_version=$fabric_version/" \
    gradle.properties

  sed -i "s/\"minecraft\": \".*\"/\"minecraft\": \"~$mc_version\"/" \
    src/main/resources/fabric.mod.json

  echo "Running build (datagen + mixin target validation)"
  run_build
  if ! compgen -G "build/classes/java/main/*refmap.json" > /dev/null; then
    echo "Error: mixin refmap missing; target validation did not run" >&2
    exit 1
  fi
  find build/libs -name '*sources*.jar' -delete
  mv build/libs/creaturechat-*.jar .

  # Safe Forge/NeoForge packaging
  case "$mc_version" in
    "1.20.1") suffix="forge" ;;
    "1.21.1") suffix="neoforge" ;;
    *)        suffix="" ;;
  esac

  if [[ -n "$suffix" ]]; then
    forge_jars=(creaturechat-*+"$mc_version".jar)
    if (( ${#forge_jars[@]} )); then
      jar="${forge_jars[0]}"
      cp "$jar" "${jar%.jar}-$suffix.jar"
      touch "${suffix^^}"  # Uppercase marker file: FORGE or NEOFORGE
      zip -r "${jar%.jar}-$suffix.jar" "${suffix^^}"
      rm "${suffix^^}"
    else
      echo "Warning: no jar matched for $suffix packaging (creaturechat-*+$mc_version.jar)" >&2
    fi
  fi

  # download Fabric API
  api_jar="fabric-api-${fabric_version}.jar"
  url="https://github.com/FabricMC/fabric/releases/download/${fabric_version//+/%2B}/${api_jar}"
  wget -q -O "$api_jar" "$url"

  echo
done <<< "$VERSIONS"
