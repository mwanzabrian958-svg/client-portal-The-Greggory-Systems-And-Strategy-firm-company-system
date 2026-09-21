# The Greggory Client Portal - Deployment Script
# This script builds the release APK and copies it to the docs folder for website download

Write-Host "🚀 Starting Deployment Process..." -ForegroundColor Cyan

# 1. Build Release APK
Write-Host "📦 Building Release APK..." -ForegroundColor Yellow
./gradlew assembleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed. Please check your keystore settings." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 2. Identify APK Path
# Note: Root build.gradle redirects builds to C:/android-builds/ on Windows
$apkPath = "C:/android-builds/The-Greggory-Client-Portal/app/outputs/apk/release/app-release.apk"
if (-not (Test-Path $apkPath)) {
    $apkPath = "app/build/outputs/apk/release/app-release.apk"
}

if (Test-Path $apkPath) {
    # 3. Copy to docs folder
    Write-Host "🚚 Copying APK to docs/GSSF-client-portal.apk..." -ForegroundColor Yellow
    Copy-Item $apkPath "docs/GSSF-client-portal.apk" -Force

    # 4. Verify Download URL in version.json
    $versionFile = "docs/version.json"
    $expectedUrl = "https://github.com/mwanzabrian958-svg/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/raw/main/docs/GSSF-client-portal.apk"
    Write-Host "🔗 Verifying download URL in version.json..." -ForegroundColor Yellow
    $json = Get-Content $versionFile | ConvertFrom-Json
    if ($json.url -ne $expectedUrl) {
        $json.url = $expectedUrl
        $json | ConvertTo-Json -Depth 10 | Set-Content $versionFile
        Write-Host "✅ Updated version.json URL to match the release link." -ForegroundColor Cyan
    }

    Write-Host "✅ Deployment Ready!" -ForegroundColor Green
    Write-Host "Next steps:"
    Write-Host "1. Git add docs/GSSF-client-portal.apk"
    Write-Host "2. Git commit and push to main"
    Write-Host "3. Your app will be live at: https://mwanzabrian958-svg.github.io/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/"
} else {
    Write-Host "❌ Could not find the built APK at $apkPath" -ForegroundColor Red
}
