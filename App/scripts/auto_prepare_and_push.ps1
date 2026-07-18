<#
Non-interactive helper to:
 - untrack common generated and IDE files under App/
 - commit .gitignore
 - commit removal of tracked generated files
 - set remote to https://github.com/Priyanshuu-31/forgemind-qc.git
 - attempt to push branch main

This script makes destructive Git index changes (untracking) but keeps files on disk.
Run from any location; it resolves the repo root automatically.
#>
try {
  $repoRoot = (git rev-parse --show-toplevel).Trim()
} catch {
  $repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
  $repoRoot = Resolve-Path "$repoRoot\.."
}
Set-Location $repoRoot

Write-Host "Repository root: $repoRoot"

function TryRun([string]$cmd) {
  Write-Host "> $cmd"
  $out = & cmd /c $cmd 2>&1
  Write-Host $out
}

Write-Host "Adding .gitignore and committing (if changes)..."
TryRun "git add App\.gitignore"
TryRun "git commit -m ""chore: add Android Studio .gitignore"" || echo 'No .gitignore changes to commit.'"

Write-Host "Untracking IDE and generated patterns..."
TryRun "git rm -r --cached --ignore-unmatch App/.idea" 2>$null
TryRun "git rm --cached --ignore-unmatch \"App/*.iml\"" 2>$null

# Untrack all tracked paths that include '/build/'
$buildPaths = git ls-files | Select-String '/build/' | ForEach-Object { $_.Line }
foreach ($p in $buildPaths) {
  TryRun "git rm -r --cached --ignore-unmatch `"$p`"" 2>$null
}

TryRun "git rm -r --cached --ignore-unmatch App/.gradle" 2>$null
TryRun "git rm -r --cached --ignore-unmatch App/.externalNativeBuild" 2>$null
TryRun "git rm -r --cached --ignore-unmatch App/.cxx" 2>$null
TryRun "git rm -r --cached --ignore-unmatch App/out" 2>$null
TryRun "git rm -r --cached --ignore-unmatch App/captures" 2>$null

TryRun "git rm --cached --ignore-unmatch App/local.properties" 2>$null
TryRun "git rm --cached --ignore-unmatch \"App/**/*.keystore\"" 2>$null
TryRun "git rm --cached --ignore-unmatch \"App/**/google-services.json\"" 2>$null

Write-Host "Staging and committing removal of generated files (if any)..."
TryRun "git add -A"
TryRun "git commit -m ""chore: remove generated/IDE files from tracking"" || echo 'No tracked generated files to commit.'"

Write-Host "Ensuring branch 'main' exists and is checked out..."
TryRun "git branch -M main" 2>$null

Write-Host "Setting remote origin to your GitHub repository..."
TryRun "git remote remove origin || true"
TryRun "git remote add origin https://github.com/Priyanshuu-31/forgemind-qc.git"

Write-Host "Attempting to push to origin main (this may prompt for credentials)."
TryRun "git push -u origin main"

Write-Host "Done. Check 'git status' and remote repository on GitHub to confirm." 
