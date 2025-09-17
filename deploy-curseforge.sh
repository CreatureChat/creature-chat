#!/bin/bash

set -e

CURSEFORGE_API_KEY=${CURSEFORGE_API_KEY}
CHANGELOG_FILE="./CHANGELOG.md"
API_URL="https://minecraft.curseforge.com/api"
PROJECT_ID=1012118
USER_AGENT="CreatureChat-Minecraft-Mod:curseforge@owlmaddie.com"
SLEEP_DURATION=5

# Function to fetch version types and return the base game type ID
fetch_base_version_id() {
  local base_version=$(echo "$1" | grep -oE '^[0-9]+\.[0-9]+')
  local version_types_cache="/tmp/version_types.json"

  if [ ! -f "$version_types_cache" ]; then
    curl --retry 3 --retry-delay 5 -s -H "X-Api-Token: $CURSEFORGE_API_KEY" "$API_URL/game/version-types" > "$version_types_cache"
  fi

  local version_types_response=$(cat "$version_types_cache")
  local base_version_id=$(echo "$version_types_response" | jq -r --arg base_version "Minecraft $base_version" '.[] | select(.name == $base_version) | .id')

  if [ -z "$base_version_id" ]; then
    echo "ERROR: Base version ID not found."
    exit 1
  fi

  echo "$base_version_id"
}

# Main function to fetch game version IDs
fetch_game_version_ids() {
  local minecraft_version="$1"

  # Fetch the base version ID
  local base_version_id=$(fetch_base_version_id "$minecraft_version")

  # Cache the game versions JSON data
  local game_versions_cache="/tmp/game_versions.json"
  if [ ! -f "$game_versions_cache" ]; then
    curl --retry 3 --retry-delay 5 -s -H "X-Api-Token: $CURSEFORGE_API_KEY" "$API_URL/game/versions" > "$game_versions_cache"
  fi

  local response=$(cat "$game_versions_cache")

  # Find the specific version ID from the cached data
  local minecraft_id=$(echo "$response" | jq -r --arg base_version_id "$base_version_id" --arg full_version "$minecraft_version" '.[] | select(.gameVersionTypeID == ($base_version_id | tonumber) and .name == $full_version) | .id' | head -n 1)

  if [ -z "$minecraft_id" ]; then
    echo "ERROR: Minecraft version ID not found."
    exit 1
  fi

  # Retrieve the other IDs as before
  local client_id=$(echo "$response" | jq -r '.[] | select(.name == "Client") | .id')
  local server_id=$(echo "$response" | jq -r '.[] | select(.name == "Server") | .id')
  local fabric_id=$(echo "$response" | jq -r '.[] | select(.name == "Fabric") | .id')
  local forge_id=$(echo "$response" | jq -r '.[] | select(.name == "Forge") | .id')
  local neoforge_id=$(echo "$response" | jq -r '.[] | select(.name == "NeoForge") | .id')

  if [ -z "$client_id" ] || [ -z "$server_id" ] || ([ -z "$fabric_id" ] && [ -z "$forge_id" ]); then
    echo "ERROR: One or more game version IDs not found."
    exit 1
  fi

  echo "$client_id $server_id $fabric_id $forge_id $minecraft_id $neoforge_id"
}

# Read the first changelog block
CHANGELOG=$(awk '/^## \[/{ if (p) exit; p=1 } p' "$CHANGELOG_FILE")
echo "CHANGELOG:"
echo "$CHANGELOG"
echo ""

# Check if the changelog contains "UNRELEASED"
if echo "$CHANGELOG" | grep -qi "UNRELEASED"; then
  echo "ERROR: Changelog contains UNRELEASED. Please finalize the changelog before deploying."
  exit 1
fi

# Extract the version
VERSION=$(echo "$CHANGELOG" | head -n 1 | sed -n 's/^## \[\(.*\)\] - .*/\1/p')
echo "VERSION:"
echo "$VERSION"
echo ""

# Iterate over each jar file in the artifacts
for FILE in creaturepals*.jar; do
  if [ -f "$FILE" ]; then
    echo "--------------$FILE----------------"
    FILE_BASENAME=$(basename "$FILE")
    OUR_VERSION=$(echo "$FILE_BASENAME" | sed -n 's/creaturepals-\(.*\)+.*\.jar/\1/p')
    MINECRAFT_VERSION=$(echo "$FILE_BASENAME" | sed -n 's/.*+\([0-9.]*\)\(-forge\|-neoforge\)*\.jar/\1/p')
    VERSION_NUMBER="$OUR_VERSION-$MINECRAFT_VERSION"

    # Verify that OUR_VERSION and MINECRAFT_VERSION are not empty and OUR_VERSION matches VERSION
    if [ -z "$OUR_VERSION" ] || [ -z "$MINECRAFT_VERSION" ] || [ "$OUR_VERSION" != "$VERSION" ]; then
      echo "ERROR: Version mismatch or missing version information in $FILE_BASENAME. OUR_VERSION: $OUR_VERSION, MINECRAFT_VERSION: $MINECRAFT_VERSION, EXPECTED VERSION: $VERSION"
      exit 1
    fi

    echo "Preparing to upload $FILE_BASENAME as version $VERSION_NUMBER..."

    # Fetch game version IDs
    GAME_TYPE_ID=$(fetch_base_version_id "$MINECRAFT_VERSION")
    GAME_VERSION_IDS=($(fetch_game_version_ids "$MINECRAFT_VERSION"))

    # DEBUG
    echo "Minecraft Type ID: $GAME_TYPE_ID"
    echo "Minecraft Versions IDs (client_id: ${GAME_VERSION_IDS[0]}, server_id: ${GAME_VERSION_IDS[1]}, fabric_id: ${GAME_VERSION_IDS[2]}, forge_id: ${GAME_VERSION_IDS[3]}, minecraft_id: ${GAME_VERSION_IDS[4]}, neoforge_id: ${GAME_VERSION_IDS[5]})"

    # Determine the dependency slugs and gameVersions based on the file name
    if [[ "$FILE_BASENAME" == *"-forge.jar" ]]; then
      DEPENDENCY_SLUGS=("sinytra-connector" "forgified-fabric-api")
      # client, server, forge, minecraft
      GAME_VERSIONS="[${GAME_VERSION_IDS[0]}, ${GAME_VERSION_IDS[1]}, ${GAME_VERSION_IDS[3]}, ${GAME_VERSION_IDS[4]}]"
    elif [[ "$FILE_BASENAME" == *"-neoforge.jar" ]]; then
      DEPENDENCY_SLUGS=("sinytra-connector" "forgified-fabric-api")
      # client, server, neoforge, minecraft
      GAME_VERSIONS="[${GAME_VERSION_IDS[0]}, ${GAME_VERSION_IDS[1]}, ${GAME_VERSION_IDS[5]}, ${GAME_VERSION_IDS[4]}]"
    else
      DEPENDENCY_SLUGS=("fabric-api")
      # client, server, fabric, minecraft
      GAME_VERSIONS="[${GAME_VERSION_IDS[0]}, ${GAME_VERSION_IDS[1]}, ${GAME_VERSION_IDS[2]}, ${GAME_VERSION_IDS[4]}]"
    fi

    # Create dependencies array for payload
    RELATIONS=$(for slug in "${DEPENDENCY_SLUGS[@]}"; do jq -n --arg slug "$slug" '{"slug": $slug, "type": "requiredDependency"}'; done | jq -s .)

    # Create a new version payload
    PAYLOAD=$(jq -n --arg changelog "$CHANGELOG" \
      --arg changelogType "markdown" \
      --arg displayName "$FILE_BASENAME" \
      --argjson gameVersions "$GAME_VERSIONS" \
      --argjson gameVersionTypeIds "[$GAME_TYPE_ID]" \
      --arg releaseType "release" \
      --argjson relations "$RELATIONS" \
      '{
        "changelog": $changelog,
        "changelogType": $changelogType,
        "displayName": $displayName,
        "gameVersions": $gameVersions | map(tonumber),
        "gameVersionTypeIds": $gameVersionTypeIds,
        "releaseType": $releaseType,
        "relations": {
          "projects": $relations
        }
      }')

    # Write the payload to a temporary file to avoid issues with large payloads
    echo "$PAYLOAD" > metadata.json

    # Sleep for the specified duration
    sleep $SLEEP_DURATION

    # Upload the version with the file
    echo "Uploading $FILE_BASENAME as version $VERSION_NUMBER..."
    HTTP_RESPONSE=$(curl --retry 3 --retry-delay 5 --fail -o response.txt -w "\nHTTP Code: %{http_code}\n" -X POST "$API_URL/projects/$PROJECT_ID/upload-file" \
      -H "X-Api-Token: $CURSEFORGE_API_KEY" \
      -H "User-Agent: $USER_AGENT" \
      -F "metadata=<metadata.json;type=application/json" \
      -F "file=@$FILE;type=application/java-archive")

    # Output the response and HTTP code
    echo "Response:"
    cat response.txt
    echo "$HTTP_RESPONSE"

    echo "Uploaded $FILE_BASENAME as version $VERSION_NUMBER."
  fi
done
