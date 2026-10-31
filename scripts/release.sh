#!/usr/bin/env bash
# Builds a signed release APK and publishes it as a GitHub Release.
# Usage: scripts/release.sh 0.1.3
#        PRERELEASE=1 scripts/release.sh 0.1.3   (not yet tested in a real car; /releases/latest skips it)
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

# -PpublicBuild: ignore launcher/private-res so the public APK has no personal overrides.
./gradlew --no-daemon -q -PpublicBuild clean assembleRelease
apk="$root/launcher/app/build/outputs/apk/release/app-release.apk"
out="$root/launcher/app/build/EcliptaLauncher-v$version.apk"
cp "$apk" "$out"
(cd "$(dirname "$out")" && shasum -a 256 "$(basename "$out")" > "$(basename "$out").sha256")

git tag -a "v$version" -m "Eclipta Launcher v$version"
git push origin "v$version"
gh release create "v$version" "$out" "$out.sha256" --title "Eclipta Launcher v$version" --generate-notes ${PRERELEASE:+--prerelease}
