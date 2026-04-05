#!/bin/bash

# List of versions to build
VERSIONS=("1.21.4" "1.21.5" "1.21.6" "1.21.7" "1.21.8" "1.21.9" "1.21.10" "1.21.11" "26.1")

# Ensure the output directory exists
mkdir -p build/all_versions

for MC_VERSION in "${VERSIONS[@]}"; do
    echo "=========================================="
    echo "Building for Minecraft $MC_VERSION..."
    echo "=========================================="

    # Update gradle.properties
    sed -i "s/^minecraft_version=.*/minecraft_version=$MC_VERSION/" gradle.properties
    
    # Handle yarn mappings (this might need adjustment per version if build numbers vary)
    if [[ "$MC_VERSION" == "26.1" ]]; then
        sed -i "s/^yarn_mappings=.*/yarn_mappings=$MC_VERSION+build.1/" gradle.properties
    else
        sed -i "s/^yarn_mappings=.*/yarn_mappings=$MC_VERSION+build.1/" gradle.properties
    fi

    # Run build
    ./gradlew clean build

    if [ $? -eq 0 ]; then
        echo "Build successful for $MC_VERSION!"
        # Copy the jar to a separate folder to avoid it being overwritten
        cp build/libs/mikutester-*.jar build/all_versions/mikutester-$MC_VERSION.jar
    else
        echo "Build FAILED for $MC_VERSION. Skipping..."
    fi
done

echo "=========================================="
echo "All builds finished! Check build/all_versions/ for the jars."
echo "=========================================="
