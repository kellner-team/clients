#!/bin/sh
set -eu

# Xcode Cloud clones shallow and without tags, but the app version is derived from
# git tags (see iosApp/scripts/set-version.sh). Restore the full history + tags.
git -C "$CI_PRIMARY_REPOSITORY_PATH" fetch --unshallow --tags origin || \
    git -C "$CI_PRIMARY_REPOSITORY_PATH" fetch --tags origin
