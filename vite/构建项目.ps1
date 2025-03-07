# 在脚本开头添加这行，设置PowerShell输出编码

# 设置变量
$BuildType = "release"  # 可以是 "debug" 或 "release"
$CurrentDate = Get-Date -Format "yyyyMMdd_HHmmss"

# 前端构建
Write-Host "正在构建前端代码..." -ForegroundColor Cyan
npm run build

# Capacitor同步
Write-Host "正在同步插件和原生代码..." -ForegroundColor Cyan
npx cap sync

# 构建APK
Write-Host "正在构建Android APK ($BuildType)..." -ForegroundColor Cyan
$OutputApkName = ""

if ($BuildType -eq "debug") {
    cd android
    ./gradlew assembleDebug
    $SourceApkPath = "./app/build/outputs/apk/debug/app-debug.apk"
    cd ..

    # 复制APK到根目录
    Copy-Item -Path "android/$SourceApkPath" -Destination "./$OutputApkName"
    Write-Host "Debug APK已复制到项目根目录: ./$OutputApkName" -ForegroundColor Green
}
elseif ($BuildType -eq "release") {
    cd android
    ./gradlew assembleRelease
    $SourceApkPath = "./app/build/outputs/apk/release/app-release.apk"
    cd ..

    # 复制APK到根目录
    Copy-Item -Path "android/$SourceApkPath" -Destination "./$OutputApkName"
    Write-Host "Release APK已复制到项目根目录: ./$OutputApkName" -ForegroundColor Green
}

