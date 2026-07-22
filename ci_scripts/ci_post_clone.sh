#!/bin/sh
set -eu

# Xcode Cloud clones shallow and without tags, but the app version is derived from
# git tags (see iosApp/scripts/set-version.sh). Restore the full history + tags.
git -C "$CI_PRIMARY_REPOSITORY_PATH" fetch --unshallow --tags origin || \
    git -C "$CI_PRIMARY_REPOSITORY_PATH" fetch --tags origin

# Xcode Cloud images ship no JDK, but the Xcode build phases invoke ./gradlew to
# build the Kotlin `shared` framework. Install one into the derived data directory:
# it persists between builds (so this is a one-time download) and we can write to
# it without sudo, which Xcode Cloud does not grant.
#
# JAVA_HOME must be set as an environment variable on the workflow itself --
# exports from this script do not reach the build-phase shells:
#   JAVA_HOME        = /Volumes/workspace/DerivedData/JDK/Contents/Home
#   GRADLE_USER_HOME = /Volumes/workspace/DerivedData/.gradle
# Pinned to a major version only; the Adoptium API resolves the latest GA patch
# release for it. The version is read from the root .java-version, which is the
# single source of truth shared with Gradle and the GitHub workflows.
JDK_MAJOR="$(tr -d '[:space:]' < "${CI_PRIMARY_REPOSITORY_PATH}/.java-version")"
JDK_DIR="${CI_DERIVED_DATA_PATH}/JDK"
# Marker records which major version is installed, so a bump reinstalls it.
JDK_MARKER="${JDK_DIR}/.installed-${JDK_MAJOR}"

if [ -f "$JDK_MARKER" ]; then
    echo "JDK ${JDK_MAJOR} already present in derived data, skipping install"
else
    echo "Installing JDK ${JDK_MAJOR} into ${JDK_DIR}"

    # Xcode Cloud workers are all Apple Silicon.
    url="https://api.adoptium.net/v3/binary/latest/${JDK_MAJOR}/ga/mac/aarch64/jdk/hotspot/normal/eclipse"

    curl -fsSL "$url" -o /tmp/jdk.tar.gz

    rm -rf "$JDK_DIR"
    mkdir -p "$JDK_DIR"
    # Strip the versioned `jdk-X.Y.Z` wrapper so the path stays version-agnostic.
    tar xzf /tmp/jdk.tar.gz -C "$JDK_DIR" --strip-components=1
    rm /tmp/jdk.tar.gz

    touch "$JDK_MARKER"
fi

"${JDK_DIR}/Contents/Home/bin/java" -version
