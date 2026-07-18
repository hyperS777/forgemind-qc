<#
  PowerShell helper to untrack common generated and IDE files under App\
  Run from repository root (recommended) in PowerShell (Windows).
#>
Param(
  [switch]$WhatIf
)

try { $root = (git rev-parse --show-toplevel).Trim() } catch { $root = Get-Location }
Set-Location $root

Write-Host "Listing tracked files matching common generated/IDE patterns..."
git ls-files | Select-String -Pattern '(^App/\.idea/)|(^App/.*\.iml$)|(/build/)|local.properties$|google-services.json$|\.keystore$' -AllMatches | ForEach-Object { $_.Line }

if (-not $WhatIf) {
  $confirm = Read-Host "Proceed to untrack these files from Git? Type 'yes' to continue"
  if ($confirm -ne 'yes') { Write-Host 'Aborted.'; exit 1 }
}

# Untrack common patterns
git rm -r --cached --ignore-unmatch App/.idea 2>$null
git rm --cached --ignore-unmatch "App/*.iml" 2>$null

# Untrack build directories under App
Get-ChildItem -Path App -Recurse -Directory -Filter build -ErrorAction SilentlyContinue | ForEach-Object {
  git rm -r --cached --ignore-unmatch ($_.FullName) 2>$null
}

git rm -r --cached --ignore-unmatch App/.gradle 2>$null
git rm -r --cached --ignore-unmatch App/.externalNativeBuild 2>$null
git rm -r --cached --ignore-unmatch App/.cxx 2>$null
git rm -r --cached --ignore-unmatch App/out 2>$null
git rm -r --cached --ignore-unmatch App/captures 2>$null

git rm --cached --ignore-unmatch App/local.properties 2>$null
git rm --cached --ignore-unmatch "App/**/*.keystore" 2>$null
git rm --cached --ignore-unmatch "App/**/google-services.json" 2>$null

Write-Host "Untracking complete. Review with: git status"
