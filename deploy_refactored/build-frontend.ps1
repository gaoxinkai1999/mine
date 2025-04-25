# deploy_refactored/build-frontend.ps1
# 负责 vite 前端项目构建、Capacitor 同步和 Android APK 打包

# 加载配置并捕获返回的哈希表
$config = . "$PSScriptRoot/config.ps1"

# 检查必要的配置是否存在 (从哈希表读取)
if (-not $config.VERSION) { Write-Host "错误：版本号未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
# COS 配置检查将在 publish-app.ps1 中进行

# 定义项目根目录和前端目录（相对于此脚本）
$ProjectRoot = Join-Path $PSScriptRoot ".."
$FrontendPath = Join-Path $ProjectRoot "vite"
$AndroidPath = Join-Path $FrontendPath "android"

Write-Host "==== 开始前端构建 (版本: $($config.VERSION)) ====" -ForegroundColor Yellow # 使用 $config.VERSION
Set-Location $FrontendPath
Write-Host "当前目录: $(Get-Location)"

# 生产环境构建前，自动删除 capacitor.config.json 中的 server.url
Write-Host "==== Step 7.1: 移除生产环境下的 Capacitor 远程调试 URL... ====" -ForegroundColor Cyan
$capConfigPath = Join-Path $FrontendPath "capacitor.config.json"
try {
    $capJson = Get-Content $capConfigPath -Raw | ConvertFrom-Json
    if ($capJson.PSObject.Properties['server'] -and $capJson.server.PSObject.Properties['url']) {
        # 检查 server 对象和 url 属性是否存在
        $capJson.server.PSObject.Properties.Remove('url')
        $capJson | ConvertTo-Json -Depth 10 | Set-Content $capConfigPath -Encoding UTF8
        Write-Host "已移除 server.url 字段，确保生产环境加载本地资源" -ForegroundColor Green
    } else {
        Write-Host "server 或 server.url 字段不存在于 $capConfigPath，无需移除" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "错误：处理 $capConfigPath 失败 - $_" -ForegroundColor Red
    Write-Host "请检查文件是否存在并包含有效的JSON内容。" -ForegroundColor Red
    Pause
    Set-Location $ProjectRoot # 确保退出前返回根目录
    exit 1
}

# 同步安卓原生版本号和版本名
Write-Host "==== Step 7.2: 同步安卓原生版本号... ====" -ForegroundColor Cyan
$versionJsonPath = Join-Path $FrontendPath "version.json" # 已经在 $FrontendPath 目录
$gradleFilePath = Join-Path $AndroidPath "app/build.gradle"
try {
    $versionData = Get-Content $versionJsonPath -Raw | ConvertFrom-Json
    $versionName = $versionData.version

    # 计算 versionCode
    $versionParts = $versionName.Split('.')
    $major = [int]$versionParts[0]
    $minor = if ($versionParts.Length -ge 2) { [int]$versionParts[1] } else { 0 }
    $patch = if ($versionParts.Length -ge 3) { [int]$versionParts[2] } else { 0 }
    $versionCode = $major * 10000 + $minor * 100 + $patch

    $gradleContent = Get-Content $gradleFilePath -Raw

    # 替换 versionName 和 versionCode
    $gradleContent = $gradleContent -replace 'versionName\s*".*"', "versionName `"$versionName`""
    $gradleContent = $gradleContent -replace 'versionCode\s*\d+', "versionCode $versionCode"

    # 保存修改
    Set-Content $gradleFilePath $gradleContent -Encoding UTF8
    Write-Host "已将安卓原生 versionName 同步为 $versionName，versionCode 同步为 $versionCode" -ForegroundColor Green
}
catch {
    Write-Host "错误：同步安卓版本号失败 - $_" -ForegroundColor Red
    Write-Host "请检查 $versionJsonPath 和 $gradleFilePath 文件是否存在并包含有效内容。" -ForegroundColor Red
    Pause
    Set-Location $ProjectRoot
    exit 1
}

# 前端构建
Write-Host "==== Step 7.3: 正在构建前端代码... ====" -ForegroundColor Cyan
try {
    # 确保在 vite 目录下执行 npm 命令
    Write-Host "执行 npm run build:prod in $(Get-Location)"
    npm run build:prod
    if ($LASTEXITCODE -ne 0) { throw "npm build failed" }
    Write-Host "前端代码构建完成" -ForegroundColor Green
}
catch {
    Write-Host "错误：构建前端代码失败 - $_" -ForegroundColor Red
    Write-Host "请检查npm环境和构建脚本是否正确。" -ForegroundColor Red
    Pause
    Set-Location $ProjectRoot
    exit 1
}

# Capacitor同步
Write-Host "==== Step 7.4: 正在同步插件和原生代码... ====" -ForegroundColor Cyan
try {
    # 确保在 vite 目录下执行 npx 命令
    Write-Host "执行 npx cap sync in $(Get-Location)"
    npx cap sync
    if ($LASTEXITCODE -ne 0) { throw "cap sync failed" }
    Write-Host "Capacitor 同步完成" -ForegroundColor Green
}
catch {
    Write-Host "错误：同步Capacitor插件和原生代码失败 - $_" -ForegroundColor Red
    Write-Host "请检查Capacitor环境是否正确配置。" -ForegroundColor Red
    Pause
    Set-Location $ProjectRoot
    exit 1
}

# 构建APK
Write-Host "==== Step 7.5: 正在构建Android APK (release)... ====" -ForegroundColor Cyan
$OutputApkName = "app-release-v$($config.VERSION).apk" # 定义输出文件名，包含版本号 (使用 $config.VERSION)
$SourceApkPath = "./app/build/outputs/apk/release/app-release.apk" # 相对于 android 目录
$DestinationApkPath = Join-Path $FrontendPath $OutputApkName # 目标路径在 vite 目录下

try {
    Push-Location $AndroidPath
    Write-Host "执行 ./gradlew assembleRelease in $(Get-Location)"
    ./gradlew assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed" }
    Write-Host "Android Release APK 构建完成" -ForegroundColor Green

    # 检查源 APK 文件是否存在
    if (-not (Test-Path $SourceApkPath)) {
        throw "构建成功但未找到源 APK 文件: $SourceApkPath"
    }

    # 复制APK到 vite 目录
    Copy-Item -Path $SourceApkPath -Destination $DestinationApkPath -Force
    Write-Host "Release APK 已复制到: $DestinationApkPath" -ForegroundColor Green
    Pop-Location # 返回 vite 目录
}
catch {
    Write-Host "错误：构建或复制 Release APK 失败 - $_" -ForegroundColor Red
    Write-Host "请检查Android构建环境、Gradle配置以及文件路径。" -ForegroundColor Red
    if (Get-Location | Where-Object {$_.Path -eq $AndroidPath}) { Pop-Location } # 确保从 android 目录返回
    Pause
    Set-Location $ProjectRoot
    exit 1
}
finally {
     # 确保无论成功或失败都尝试恢复 capacitor.config.json
     Write-Host "==== Step 7.6: 恢复开发环境的 Capacitor URL... ====" -ForegroundColor Cyan
     try {
         $capJsonRestore = Get-Content $capConfigPath -Raw | ConvertFrom-Json
         # 检查 server 对象是否存在，如果不存在则创建
         if (-not $capJsonRestore.PSObject.Properties['server']) {
             $capJsonRestore | Add-Member -MemberType NoteProperty -Name "server" -Value (New-Object PSObject)
         }
         # 设置或更新 url 属性
         $capJsonRestore.server | Add-Member -MemberType NoteProperty -Name "url" -Value "http://192.168.0.102:5173" -Force
         $capJsonRestore | ConvertTo-Json -Depth 10 | Set-Content $capConfigPath -Encoding UTF8
         Write-Host "已将 $capConfigPath 中的 server.url 恢复为开发环境地址 http://192.168.0.102:5173" -ForegroundColor Green
     }
     catch {
         Write-Host "警告：恢复 $capConfigPath 中的 server.url 失败 - $_" -ForegroundColor Yellow
         Write-Host "请检查文件是否可写。" -ForegroundColor Yellow
         # 这里只给警告，不中断流程
     }
}


# 返回项目根目录
Set-Location $ProjectRoot
Write-Host "==== 前端构建完成，APK 生成于: $DestinationApkPath ====" -ForegroundColor Yellow

# 将生成的 APK 路径传递给后续脚本（如果需要）
# 可以通过环境变量或者写入一个临时文件的方式
# 这里我们假设 deploy-all-refactored.ps1 会知道 APK 的命名规则和位置