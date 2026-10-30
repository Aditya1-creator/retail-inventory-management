param([switch]$Demo)
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot 'Inventory')
$previousPassword = $env:DB_PASSWORD
$previousUsername = $env:DB_USERNAME
try {
    if ($Demo) {
        Write-Host 'Starting temporary demonstration database on http://localhost:8082'
        Write-Host 'Demo records are lost when this backend stops.'
        & .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
    } else {
        if (-not $env:DB_USERNAME) { $env:DB_USERNAME = 'root' }
        if (-not $env:DB_PASSWORD) {
            $localPassword = Read-Host "Enter the MySQL password for $env:DB_USERNAME (hidden)" -AsSecureString
            $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $localPassword).Password
        }
        & .\mvnw.cmd spring-boot:run
    }
    if ($LASTEXITCODE -ne 0) { throw 'Backend did not start successfully. Check the message above.' }
} finally {
    $env:DB_PASSWORD = $previousPassword
    $env:DB_USERNAME = $previousUsername
    Pop-Location
}
