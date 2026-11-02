# The Greggory Client Portal - Local Release Build
# Published releases are created by the tagged GitHub Actions workflow.

Write-Host "🚀 Starting Deployment Process..." -ForegroundColor Cyan

# 1. Build Release APK
Write-Host "📦 Building Release APK..." -ForegroundColor Yellow
./gradlew assembleRelease

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed. Please check your keystore settings." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 2. Identify and verify the APK
# Note: Root build.gradle redirects builds to C:/android-builds/ on Windows
$apkPath = "C:/android-builds/The-Greggory-Client-Portal/app/outputs/apk/release/app-release.apk"
if (-not (Test-Path $apkPath)) {
    $apkPath = "app/build/outputs/apk/release/app-release.apk"
}

if (Test-Path $apkPath) {
    $hash = (Get-FileHash -LiteralPath $apkPath -Algorithm SHA256).Hash
    Write-Host "✅ Local signed APK built and verified." -ForegroundColor Green
    Write-Host "APK: $apkPath"
    Write-Host "SHA-256: $hash"
    Write-Host "To publish the APK and matching update manifest, push a vMAJOR.MINOR.PATCH tag from a commit on main."
} else {
    Write-Host "❌ Could not find the built APK at $apkPath" -ForegroundColor Red
    exit 1
}
