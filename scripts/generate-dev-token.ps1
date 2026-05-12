param(
    [string]$UserId = "550e8400-e29b-41d4-a716-446655440000",
    [string]$Email = "dev@example.com",
    [string[]]$Roles = @("user", "author"),
    [int]$TtlMinutes = 60,
    [string]$Issuer = "auth-service",
    [string]$Secret = $env:ACCESS_TOKEN_SECRET,
    [string]$OutputPath = ".dev/dev-token.txt",
    [switch]$Raw
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($Secret)) {
    $Secret = "dev-access-token-secret-change-me"
}

function ConvertTo-Base64Url {
    param([byte[]]$Bytes)

    return [Convert]::ToBase64String($Bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

try {
    [void][Guid]::Parse($UserId)
} catch {
    throw "UserId must be a valid UUID. Received: $UserId"
}

$normalizedRoles = $Roles |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
    ForEach-Object { $_.Trim().ToLowerInvariant() } |
    Select-Object -Unique

if ($normalizedRoles.Count -eq 0) {
    throw "At least one role is required"
}

$header = @{
    alg = "HS256"
    typ = "JWT"
} | ConvertTo-Json -Compress

$payload = @{
    user_id = $UserId
    email = $Email
    roles = @($normalizedRoles)
    iss = $Issuer
    exp = [DateTimeOffset]::UtcNow.AddMinutes($TtlMinutes).ToUnixTimeSeconds()
} | ConvertTo-Json -Compress

$encodedHeader = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($header))
$encodedPayload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payload))
$data = "$encodedHeader.$encodedPayload"

$hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
$signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($data)))
$token = "$data.$signature"

if ($Raw) {
    Write-Output $token
    exit 0
}

$resolvedOutputPath = $null
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $parent = Split-Path -Parent $OutputPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }

    $expiresAt = [DateTimeOffset]::FromUnixTimeSeconds(($payload | ConvertFrom-Json).exp).ToLocalTime()
    @(
        "Dev JWT for podcast-core"
        ""
        "GeneratedAt: $([DateTimeOffset]::Now.ToString("u"))"
        "ExpiresAt:   $($expiresAt.ToString("u"))"
        "UserId:      $UserId"
        "Email:       $Email"
        "Roles:       $($normalizedRoles -join ",")"
        ""
        "Swagger Authorize value:"
        "Bearer $token"
        ""
        "Raw token:"
        $token
    ) | Set-Content -Path $OutputPath -Encoding UTF8

    $resolvedOutputPath = (Resolve-Path $OutputPath).Path
}

Write-Output "Dev JWT:"
Write-Output $token
Write-Output ""
Write-Output "Use in Swagger Authorize as:"
Write-Output "Bearer $token"
Write-Output ""
if ($resolvedOutputPath) {
    Write-Output "Saved to local git-ignored file:"
    Write-Output $resolvedOutputPath
    Write-Output ""
}
Write-Output "podcast-core must be started with the same secret:"
Write-Output "`$env:ACCESS_TOKEN_SECRET = `"$Secret`""
