# deploy_refactored/deploy-all-refactored.ps1
# 一键部署入口脚本，按顺序调用后端部署、前端构建和应用发布脚本

Write-Host "==== 开始执行一键部署 ====" -ForegroundColor Magenta

# 确保我们在脚本所在的目录执行，以便相对路径正确
Push-Location $PSScriptRoot

# 定义脚本路径
$BackendScript = Join-Path $PSScriptRoot "deploy-backend.ps1"
$FrontendScript = Join-Path $PSScriptRoot "build-frontend.ps1"
$PublishScript = Join-Path $PSScriptRoot "publish-app.ps1"

# 定义一个辅助函数来执行脚本并检查错误
function Invoke-DeployScript {
    param(
        [Parameter(Mandatory=$true)]
        [string]$ScriptPath,
        [Parameter(Mandatory=$true)]
        [string]$StepName
    )
    Write-Host "==== 开始执行步骤: $StepName ($ScriptPath) ====" -ForegroundColor Cyan
    try {
        # 使用 & 调用操作符执行脚本
        & $ScriptPath
        # 检查 $LASTEXITCODE。如果子脚本通过 exit 1 退出，这里会捕获到非零值。
        # 注意：如果子脚本内部发生未捕获的异常但没有显式 exit 1，这里可能无法捕获。
        # 更健壮的方式是在子脚本中使用 try/catch 并确保失败时 exit 1。
        if ($LASTEXITCODE -ne 0) {
            throw "脚本 $ScriptPath 执行失败，退出码: $LASTEXITCODE"
        }
        Write-Host "==== 步骤完成: $StepName ====" -ForegroundColor Green
    } catch {
        Write-Host "错误：在执行步骤 '$StepName' 时发生错误: $_" -ForegroundColor Red
        Write-Host "部署中止。" -ForegroundColor Red
        Pop-Location # 确保返回原始目录
        # 可以选择 Pause 或直接退出
        Pause
        exit 1 # 中止整个部署流程
    }
}

# --- 执行部署步骤 ---

# 1. 部署后端
Invoke-DeployScript -ScriptPath $BackendScript -StepName "后端部署"

# 2. 构建前端和 APK
Invoke-DeployScript -ScriptPath $FrontendScript -StepName "前端构建和打包"

# 3. 发布应用 (上传 APK)
Invoke-DeployScript -ScriptPath $PublishScript -StepName "应用发布 (上传到COS)"

# --- 全部完成 ---
Pop-Location # 返回执行前的目录

# 从 config.ps1 获取版本号用于最终消息
$config = . "$PSScriptRoot/config.ps1" # 再次加载以获取配置哈希表

Write-Host "==== 全部部署步骤成功完成！后端版本号：$($config.VERSION)，前端APK已构建并上传至腾讯云COS ====" -ForegroundColor Magenta # 使用 $config.VERSION
Write-Host "请注意检查各个步骤的详细日志输出。"