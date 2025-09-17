#!/bin/bash

set -e

MODRINTH_API_KEY=${MODRINTH_API_KEY}
CHANGELOG_FILE="./CHANGELOG.md"
API_URL="https://api.modrinth.com/v2"
USER_AGENT="CreatureChat-Minecraft-Mod:modrinth@owlmaddie.com"
PROJECT_ID="rvR0de1E"
AUTHOR_ID="k6RiShdd"
SLEEP_DURATION=5

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
    VERSION_NUMBER="$OUR_VERSION+$MINECRAFT_VERSION"

    # Verify that OUR_VERSION and MINECRAFT_VERSION are not empty and OUR_VERSION matches VERSION
    if [ -z "$OUR_VERSION" ] || [ -z "$MINECRAFT_VERSION" ] || [ "$OUR_VERSION" != "$VERSION" ]; then
      echo "ERROR: Version mismatch or missing version information in $FILE_BASENAME. OUR_VERSION: $OUR_VERSION, MINECRAFT_VERSION: $MINECRAFT_VERSION, EXPECTED VERSION: $VERSION"
      exit 1
    fi

    # Determine the loaders and dependencies based on the file name
    if [[ "$FILE_BASENAME" == *"-forge.jar" ]]; then
      LOADERS='["forge"]'
      DEPENDENCIES='[{"project_id": "u58R1TMW", "dependency_type": "required"}, {"project_id": "Aqlf1Shp", "dependency_type": "required"}]'
    elif [[ "$FILE_BASENAME" == *"-neoforge.jar" ]]; then
      LOADERS='["neoforge"]'
      DEPENDENCIES='[{"project_id": "u58R1TMW", "dependency_type": "required"}, {"project_id": "Aqlf1Shp", "dependency_type": "required"}]'
    else
      LOADERS='["fabric"]'
      DEPENDENCIES='[{"project_id": "P7dR8mSH", "dependency_type": "required"}]'
    fi

    # Calculate file hashes
    SHA512_HASH=$(sha512sum "$FILE" | awk '{ print $1 }')
    SHA1_HASH=$(sha1sum "$FILE" | awk '{ print $1 }')
    FILE_SIZE=$(stat -c%s "$FILE")

    # Create a new version payload
    PAYLOAD=$(jq -n --arg version_number "$VERSION_NUMBER" \
      --arg changelog "$CHANGELOG" \
      --argjson dependencies "$DEPENDENCIES" \
      --argjson game_versions '["'"$MINECRAFT_VERSION"'"]' \
      --argjson loaders "$LOADERS" \
      --arg project_id "$PROJECT_ID" \
      --arg name "CreatureChat $VERSION_NUMBER" \
      --argjson file_parts '["file"]' \
      --arg requested_status "listed" \
      '{
        "game_versions": $game_versions,
        "loaders": $loaders,
        "project_id": $project_id,
        "name": $name,
        "version_number": $version_number,
        "changelog": $changelog,
        "version_type": "release",
        "featured": false,
        "dependencies": $dependencies,
        "file_parts": $file_parts,
        "requested_status": $requested_status
      }')

    # Write the payload to a temporary file to avoid issues with large payloads
    echo "$PAYLOAD" > metadata.json

    # Sleep for the specified duration
    sleep $SLEEP_DURATION

    # Upload the version with the file
    echo "Uploading $FILE_BASENAME as version $VERSION_NUMBER..."
    HTTP_RESPONSE=$(curl --retry 3 --retry-delay 5 --fail -o response.txt -w "\nHTTP Code: %{http_code}\n" -X POST "$API_URL/version" \
      -H "Authorization: $MODRINTH_API_KEY" \
      -H "User-Agent: $USER_AGENT" \
      -F "data=@metadata.json;type=application/json;filename=metadata.json" \
      -F "file=@$FILE;type=application/java-archive;filename=$FILE_BASENAME")

    # Output the response and HTTP code
    echo "Response:"
    cat response.txt
    echo "$HTTP_RESPONSE"

    # Check if the response contains errors
    if [ $? -ne 0 ]; then
      echo "ERROR: Failed to upload $FILE_BASENAME as version $VERSION_NUMBER. Response:"
      exit 1
    fi

    echo "Uploaded $FILE_BASENAME as version $VERSION_NUMBER."
  fi
done
