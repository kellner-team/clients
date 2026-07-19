#!/bin/sh
set -eu

# Sets the app version, asking axion-release for it via the `currentVersion` Gradle
# task — the exact same source of truth the Android/Desktop clients use, so the
# derivation rules live in exactly one place (see README "Versioning schema").
#
# Runs as the last build phase and patches the *built* Info.plist, so no version
# is ever hardcoded in the repo and the working tree stays clean.
#
#   CFBundleShortVersionString  major.minor.patch  (Apple only accepts numbers here)
#   VERSION_SUFFIX              "" on a release tag, "lava-<shortHash>" otherwise.
#                               Globals.swift appends it to build the full version
#                               name (e.g. 3.1.2-lava-a1b2c3d) for Sentry & co.
#   CFBundleVersion             Xcode Cloud's auto incremented build number
#                               ($CI_BUILD_NUMBER), 1 for local builds.

# e.g. "3.1.2-lava-a1b2c3d". Other Gradle output (warnings) is filtered out.
fullVersion=$("$SRCROOT/../gradlew" -q -p "$SRCROOT/.." currentVersion |
    sed -n 's/^Project version: //p' | tail -n 1)

if [ -z "$fullVersion" ]; then
    echo "error: could not determine version from './gradlew currentVersion'" >&2
    exit 1
fi

# Apple only accepts numbers in CFBundleShortVersionString, so split the axion
# version into its numeric part and the lava suffix.
version=${fullVersion%%-*}
suffix=${fullVersion#"$version"}
suffix=${suffix#-}

plist="$TARGET_BUILD_DIR/$INFOPLIST_PATH"
buildNumber="${CI_BUILD_NUMBER:-1}"

/usr/libexec/PlistBuddy -c "Set :CFBundleShortVersionString $version" "$plist"
/usr/libexec/PlistBuddy -c "Set :CFBundleVersion $buildNumber" "$plist"
/usr/libexec/PlistBuddy -c "Set :VERSION_SUFFIX $suffix" "$plist"

echo "Set version to $version${suffix:+-$suffix} ($buildNumber)"
