#!/usr/bin/env bash
# Builds a signed release APK and publishes it as a GitHub Release.
# Usage: scripts/release.sh 0.1.3
# Needs launcher/keystore.properties (git-ignored) with:
#   storeFile=/absolute/path/outside/repo/a3launcher-release.jks
#   storePassword=...
#   keyAlias=a3launcher
#   keyPassword=...
set -euo pipefail

version="${1:?usage: scripts/release.sh X.Y.Z}"
root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root/launcher"

[ -f keystore.properties ] || { echo "Missing launcher/keystore.properties"; exit 1; }
grep -q "versionName '$version'" app/build.gradle || { echo "app/build.gradle versionName != $version"; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "Working tree not clean"; exit 1; }
[ ! -d private-res ] || { echo "launcher/private-res/ exists: move it away so the public APK has no private assets"; exit 1; }

./gradlew --no-daemon -q clean assembleRelease
apk="$root/launcher/app/build/outputs/apk/release/app-release.apk"
out="$root/launcher/app/build/A3Launcher-v$version.apk"
cp "$apk" "$out"
(cd "$(dirname "$out")" && shasum -a 256 "$(basename "$out")" > "$(basename "$out").sha256")

git tag -a "v$version" -m "A3 Launcher v$version"
git push origin "v$version"
gh release create "v$version" "$out" "$out.sha256" --title "A3 Launcher v$version" --generate-notes
