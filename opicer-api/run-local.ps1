# Loads opicer-api/.env into the process environment, then runs the API (Windows PowerShell).
# Spring Boot does not read .env files itself; secrets stay in .env only.
$apiDir = $PSScriptRoot
$envFile = Join-Path $apiDir '.env'

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            $value = $matches[2].Trim().Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($matches[1], $value, 'Process')
        }
    }
    Write-Host "Loaded env from $envFile"
} else {
    Write-Host "No .env found at $envFile; using defaults"
}

& (Join-Path $apiDir 'gradlew.bat') -p $apiDir bootRun @args
