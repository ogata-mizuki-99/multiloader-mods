# Economy automated tests (JUnit + optional manual smoke guide)
$ErrorActionPreference = 'Stop'

$economyRoot = $PSScriptRoot
$clientRoot = Resolve-Path (Join-Path $economyRoot '..\..')

Write-Host ''
Write-Host '=== Economy automated tests ===' -ForegroundColor Cyan
Write-Host ''

Push-Location $clientRoot
try {
    Write-Host '[1/2] JUnit' -ForegroundColor Yellow
    & .\gradlew.bat :economy:test --no-daemon -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'JUnit FAILED' -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Write-Host 'JUnit PASSED' -ForegroundColor Green
    Write-Host ''

    Write-Host '[2/2] Manual smoke (optional)' -ForegroundColor Yellow
    Write-Host '  Build JAR:' -ForegroundColor Gray
    Write-Host '    .\gradlew.bat :economy:build' -ForegroundColor Gray
    Write-Host '  In-game checks:' -ForegroundColor Gray
    Write-Host '    - Admin block: balances, master tabs, reset toggles, ranking compile' -ForegroundColor Gray
    Write-Host '    - BUYER shop opens without disconnect (large item list)' -ForegroundColor Gray
    Write-Host '    - ATM deposit/withdraw, sell to buyer NPC, buy from seller NPC' -ForegroundColor Gray
    Write-Host ''
    Write-Host 'All automated checks passed.' -ForegroundColor Green
} finally {
    Pop-Location
}
