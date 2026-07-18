#!/usr/bin/env bash
set -euo pipefail

# Helper: untrack common generated and IDE files under App/
# Run from repository root or from within App/.

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$ROOT"

echo "This will untrack common generated/IDE files under App/ (keeps local files)."
echo "Review matches first:"
git ls-files | grep -E "(^App/\.idea/|\.iml$|/build/|local.properties$|google-services.json$|\.keystore$|App/\.gradle/|App/captures/)" || true

read -r -p "Proceed to untrack these patterns from Git? [y/N] " ans
if [[ "$ans" != "y" && "$ans" != "Y" ]]; then
  echo "Aborted by user."; exit 1
fi

git rm -r --cached --ignore-unmatch App/.idea || true
git rm --cached --ignore-unmatch "App/*.iml" || true

# Untrack all build/ directories under App
while IFS= read -r -d $'\0' d; do
  git rm -r --cached --ignore-unmatch "$d" || true
done < <(git ls-files -z | grep -z '/build/' || true)

git rm -r --cached --ignore-unmatch App/.gradle || true
git rm -r --cached --ignore-unmatch App/.externalNativeBuild || true
git rm -r --cached --ignore-unmatch App/.cxx || true
git rm -r --cached --ignore-unmatch App/out || true
git rm -r --cached --ignore-unmatch App/captures || true

git rm --cached --ignore-unmatch App/local.properties || true
git rm --cached --ignore-unmatch "App/**/*.keystore" || true
git rm --cached --ignore-unmatch "App/**/google-services.json" || true

echo "Untracking complete. Review with: git status"
exit 0
