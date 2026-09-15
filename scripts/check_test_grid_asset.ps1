# Quick image check for the test asset used by the Greggory client portal androidTest
param(
    [string]$AssetPath = "C:\Users\Lydia mwanza\OneDrive\Desktop\personal projects\client-portal-The-Greggory-Systems-And-Strategy-firm-company-system\app\src\androidTest\assets\test_grid.jpeg",
    [string]$SourceImage = "C:\Users\Lydia mwanza\Documents\testing grid.jpeg"
)

function Write-Result {
    param([string]$Status, [string]$Message)
    Write-Output "[$Status] $Message"
}

Write-Result INFO "Checking test image asset used by the project."

$sourceExists = Test-Path $SourceImage
if (-not $sourceExists) {
    Write-Result FAIL "Source image not found: $SourceImage"
    exit 1
}
Write-Result OK "Source image found: $SourceImage"

$sourceInfo = Get-Item $SourceImage
Write-Result OK "Source size: $($sourceInfo.Length) bytes"

$assetExists = Test-Path $AssetPath
if (-not $assetExists) {
    Write-Result FAIL "Copied androidTest asset not found: $AssetPath"
    exit 1
}
Write-Result OK "Copied androidTest asset found: $AssetPath"

$assetInfo = Get-Item $AssetPath
Write-Result OK "Asset size: $($assetInfo.Length) bytes"

# Basic JPEG header check without external libs
$bytes = [System.IO.File]::ReadAllBytes($AssetPath)
if ($bytes[0] -ne 0xFF -or $bytes[1] -ne 0xD8) {
    Write-Result FAIL "Asset does not start with JPEG magic bytes."
    exit 1
}
Write-Result OK "Asset begins with valid JPEG marker."

# Basic JFIF/EXIF sanity: file should contain APP0 or APP1 marker after SOI
$hasAppMarker = $false
for ($i = 0; $i -lt [Math]::Min(2048, $bytes.Length - 1); $i++) {
    if ($bytes[$i] -eq 0xFF -and $bytes[$i + 1] -eq 0xE0) {
        $hasAppMarker = $true
        break
    }
    if ($bytes[$i] -eq 0xFF -and $bytes[$i + 1] -eq 0xE1) {
        $hasAppMarker = $true
        break
    }
}
if (-not $hasAppMarker) {
    Write-Result WARN "No APP0/APP1 marker found in first 2KB; file may still be valid JPEG."
} else {
    Write-Result OK "Found JPEG application marker (APP0/APP1)."
}

Write-Result INFO "Test asset is present and appears to be a valid JPEG."
