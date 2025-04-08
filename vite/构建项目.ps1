# 在脚本开头添加这行，设置PowerShell输出编码

# 生产环境构建前，自动删除 capacitor.config.json 中的 server.url
Write-Host "移除生产环境下的 Capacitor 远程调试 URL..." -ForegroundColor Cyan
$capConfigPath = "capacitor.config.json"
$capJson = Get-Content $capConfigPath -Raw | ConvertFrom-Json
if ($capJson.server.url) {
    $capJson.server.PSObject.Properties.Remove('url')
    $capJson | ConvertTo-Json -Depth 10 | Set-Content $capConfigPath -Encoding UTF8
    Write-Host "已移除 server.url 字段，确保生产环境加载本地资源" -ForegroundColor Green
} else {
    Write-Host "server.url 字段不存在，无需移除" -ForegroundColor Yellow
}
# 同步安卓原生版本号和版本名
Write-Host "同步安卓原生版本号..." -ForegroundColor Cyan
$versionJsonPath = "version.json"
$versionData = Get-Content $versionJsonPath -Raw | ConvertFrom-Json
$versionName = $versionData.version

# 计算 versionCode
$versionParts = $versionName.Split('.')
$major = [int]$versionParts[0]
$minor = if ($versionParts.Length -ge 2) { [int]$versionParts[1] } else { 0 }
$patch = if ($versionParts.Length -ge 3) { [int]$versionParts[2] } else { 0 }
$versionCode = $major * 10000 + $minor * 100 + $patch

$gradleFilePath = "android/app/build.gradle"
$gradleContent = Get-Content $gradleFilePath -Raw

# 替换 versionName
$gradleContent = $gradleContent -replace 'versionName\s*".*"', "versionName `"$versionName`""

# 替换 versionCode
$gradleContent = $gradleContent -replace 'versionCode\s*\d+', "versionCode $versionCode"

# 保存修改
Set-Content $gradleFilePath $gradleContent -Encoding UTF8
Write-Host "已将安卓原生 versionName 同步为 $versionName，versionCode 同步为 $versionCode" -ForegroundColor Green

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

# 上传APK和版本信息到腾讯云COS
Write-Host "开始上传APK和版本信息到腾讯云COS..." -ForegroundColor Cyan
node upload-to-cos.cjs
Write-Host "上传完成" -ForegroundColor Green


# 构建完成后，恢复开发环境的 server.url 为固定地址
$capConfigPath = "capacitor.config.json"
$capJsonRestore = Get-Content $capConfigPath -Raw | ConvertFrom-Json
if ($capJsonRestore.server.PSObject.Properties['url']) {
    $capJsonRestore.server.url = "http://192.168.0.102:5173"
} else {
    $capJsonRestore.server | Add-Member -MemberType NoteProperty -Name "url" -Value "http://192.168.0.102:5173"
}
$capJsonRestore | ConvertTo-Json -Depth 10 | Set-Content $capConfigPath -Encoding UTF8
Write-Host "已将 capacitor.config.json 中的 server.url 恢复为开发环境地址 http://192.168.0.102:5173" -ForegroundColor Green


