#!/usr/bin/env bash
# Usage: ./scripts/release.sh <version>
# Example: ./scripts/release.sh 1.5.0
#
# Does everything needed before the release tag is created:
#   1. Validates the changelog entry exists
#   2. Updates gradle.properties (VERSION_NAME, VERSION_CODE)
#   3. Writes fastlane/metadata/android/en-US/changelogs/{versionCode}.txt
#   4. Commits all changes to main
#   5. Creates and pushes the tag
#
# The GitHub Actions release workflow then builds the APK from this tag.

set -euo pipefail

VERSION="${1:-}"
if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>  (e.g. $0 1.5.0)" >&2
  exit 1
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

# Verify we are on main and it is clean
BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$BRANCH" != "main" ]; then
  echo "Error: must be on main (currently on '$BRANCH')" >&2
  exit 1
fi

if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "Error: working tree has uncommitted changes" >&2
  exit 1
fi

# Verify changelog entry exists
NOTES=$(awk "/^## \[${VERSION}\]/{found=1; next} found && /^## \[/{exit} found{print}" CHANGELOG.md | sed '/^[[:space:]]*$/d')
if [ -z "$NOTES" ]; then
  echo "Error: no entry for [${VERSION}] found in CHANGELOG.md" >&2
  echo "Add a '## [${VERSION}]' section before running this script." >&2
  exit 1
fi

# Detect breaking changes and warn
BREAKING=$(echo "$NOTES" | awk '/^### Breaking/{found=1; next} found && /^###/{exit} found{print}' | sed '/^[[:space:]]*$/d')
if [ -n "$BREAKING" ]; then
  echo ""
  echo "WARNING: BREAKING CHANGES in this release:"
  echo "$BREAKING"
  echo ""
fi

# Calculate next version code (current commit count + 1 for the upcoming commit)
CURRENT_CODE=$(grep '^VERSION_CODE=' gradle.properties | cut -d= -f2)
NEW_CODE=$(( CURRENT_CODE + 1 ))

echo "Releasing v${VERSION} (versionCode ${NEW_CODE})"
echo ""
echo "Changelog:"
echo "$NOTES"
echo ""
read -r -p "Proceed? [y/N] " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
  echo "Aborted." >&2
  exit 1
fi

# Update gradle.properties
sed -i "s/^VERSION_NAME=.*/VERSION_NAME=${VERSION}/" gradle.properties
sed -i "s/^VERSION_CODE=.*/VERSION_CODE=${NEW_CODE}/" gradle.properties

# Write F-Droid changelog (max 500 chars)
FDROID_DIR="fastlane/metadata/android/en-US/changelogs"
mkdir -p "$FDROID_DIR"
printf '%s' "$NOTES" | head -c 490 > "${FDROID_DIR}/${NEW_CODE}.txt"

echo "Written ${FDROID_DIR}/${NEW_CODE}.txt"

# Commit
COMMIT_MSG="Release v${VERSION} (${NEW_CODE})"
[ -n "$BREAKING" ] && COMMIT_MSG="${COMMIT_MSG} [breaking]"
git add gradle.properties "${FDROID_DIR}/${NEW_CODE}.txt"
git commit -S -s -m "$COMMIT_MSG"

# Tag and push
git tag -s "v${VERSION}" -m "v${VERSION}"
git push origin main "v${VERSION}"

echo ""
echo "Done. v${VERSION} pushed — GitHub Actions will build and upload the APKs."
