#!/bin/bash

# Script to build MikuTester for a specific Minecraft version
# Usage: ./build.sh [minecraft_version]

MC_VERSION=${1:-"1.21.4"}

echo "Building for Minecraft $MC_VERSION..."

# Update gradle.properties
sed -i "s/^minecraft_version=.*/minecraft_version=$MC_VERSION/" gradle.properties

# If version is 26.x or newer, we might need different yarn mappings pattern
if [[ "$MC_VERSION" == 26* ]]; then
    # Yarn mappings for 26.x might follow a different pattern or require 26.x+build.1
    sed -i "s/^yarn_mappings=.*/yarn_mappings=$MC_VERSION+build.1/" gradle.properties
else
    sed -i "s/^yarn_mappings=.*/yarn_mappings=$MC_VERSION+build.3/" gradle.properties
fi

# Run build
./gradlew clean build

if [ $? -eq 0 ]; then
    echo "Build successful! Check build/libs/ for the jar."
else
    echo "Build failed. Please check the version compatibility."
fi
