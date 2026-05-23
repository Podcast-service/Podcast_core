param(
    [switch]$Raw
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$script = Join-Path $root "scripts\generate-dev-token.ps1"

$params = @{
    UserId = "00000000-0000-0000-0000-000000000001"
    Email = "dev.user@example.local"
    Roles = @("user")
    TtlMinutes = 10080
    OutputPath = ".dev/jwt/user-token.txt"
}

if ($Raw) {
    $params.Raw = $true
}

& $script @params
