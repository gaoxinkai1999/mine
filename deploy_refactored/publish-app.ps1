# deploy_refactored/publish-app.ps1
# 负责将构建好的 APK 和版本信息上传到腾讯云 COS

# 加载配置并捕获返回的哈希表
$config = . "$PSScriptRoot/config.ps1"

# 检查必要的配置是否存在 (从哈希表读取)
if (-not $config.VERSION) { Write-Host "错误：版本号未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.COS_SECRET_ID -or -not $config.COS_SECRET_KEY) {
    Write-Host "错误：COS Secret ID 或 Secret Key 未在 config.ps1 中加载或从 .env.cos 文件读取。" -ForegroundColor Red
    Write-Host "请确保 $($config.ENV_PATH) 文件存在且包含正确的 COS_SECRET_ID 和 COS_SECRET_KEY。" -ForegroundColor Red # 使用 $config.ENV_PATH
    Pause
    exit 1
}

# 定义项目根目录和前端目录（相对于此脚本）
$ProjectRoot = Join-Path $PSScriptRoot ".."
$FrontendPath = Join-Path $ProjectRoot "vite"

# 确定期望的 APK 文件路径 (基于 build-frontend.ps1 的输出)
$ApkName = "app-release-v$($config.VERSION).apk" # 使用 $config.VERSION
$ApkPath = Join-Path $FrontendPath $ApkName

Write-Host "==== 开始上传应用到腾讯云 COS (版本: $($config.VERSION)) ====" -ForegroundColor Yellow # 使用 $config.VERSION

# 检查 APK 文件是否存在
if (-not (Test-Path $ApkPath)) {
    Write-Host "错误：未找到需要上传的 APK 文件: $ApkPath" -ForegroundColor Red
    Write-Host "请先确保 build-frontend.ps1 成功执行并生成了 APK 文件。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "准备上传文件: $ApkPath"

# 上传APK和版本信息到腾讯云COS
Write-Host "==== Step 7.7: 开始上传APK和版本信息到腾讯云COS... ====" -ForegroundColor Cyan
try {
    Push-Location $FrontendPath
    Write-Host "执行 node upload-to-cos.cjs in $(Get-Location)"
    # 确保 Node.js 环境可用，并且 upload-to-cos.cjs 脚本能正确读取环境变量或配置
    # Node.js 脚本可能需要直接读取 .env 文件或依赖父进程传递的环境变量
    # 或者更健壮的方式是修改 upload-to-cos.cjs 让它接受参数或读取标准 .env 文件

    # 传递环境变量给 Node 进程 (从哈希表读取)
    $env:COS_SECRET_ID = $config.COS_SECRET_ID
    $env:COS_SECRET_KEY = $config.COS_SECRET_KEY
    $env:APP_VERSION = $config.VERSION # 如果 Node 脚本需要版本号
    $env:APK_PATH = $ApkPath # 如果 Node 脚本需要 APK 路径

    node upload-to-cos.cjs

    # 检查 Node 脚本的退出码
    if ($LASTEXITCODE -ne 0) { throw "upload to COS failed (Node script exited with code $LASTEXITCODE)" }

    Write-Host "上传完成" -ForegroundColor Green
    Pop-Location
}
catch {
    Write-Host "错误：上传APK和版本信息到腾讯云COS失败 - $_" -ForegroundColor Red
    Write-Host "请检查网络连接、COS配置 ($($config.ENV_PATH)) 以及 Node.js 脚本 (upload-to-cos.cjs) 是否正确。" -ForegroundColor Red # 使用 $config.ENV_PATH
    if (Get-Location | Where-Object {$_.Path -eq $FrontendPath}) { Pop-Location } # 确保从 vite 目录返回
    Pause
    exit 1
}
finally {
    # 清理传递给 Node 进程的环境变量（可选）
    Remove-Item Env:\COS_SECRET_ID -ErrorAction SilentlyContinue
    Remove-Item Env:\COS_SECRET_KEY -ErrorAction SilentlyContinue
    Remove-Item Env:\APP_VERSION -ErrorAction SilentlyContinue
    Remove-Item Env:\APK_PATH -ErrorAction SilentlyContinue
}

Write-Host "==== 应用上传完成 ====" -ForegroundColor Yellow