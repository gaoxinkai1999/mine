# deploy_refactored/config.ps1
# 集中管理部署配置

# ====== 可配置参数 ======
$Script:REGISTRY_URL = "crpi-xlafczp8xa83qpr9.cn-beijing.personal.cr.aliyuncs.com"
$Script:NAMESPACE = "gaoxinkai"
$Script:IMAGE_NAME = "myapp"
$Script:SERVER_IP = "129.226.158.74"
$Script:SERVER_USER = "ubuntu"
$Script:SSH_IDENTITY_FILE = "C:\Users\g\.ssh\m.pem" # 指定 SSH 私钥文件路径
$Script:SERVER_PORT_MAP = "9850:8085"
# $Script:SERVER_PASSWORD = "..." # 服务器密码不再需要，已移除
$Script:ALIYUN_USER = "1102336460@qq.com"
$Script:ALIYUN_PASS = "gxk19990805" # 警告：密码硬编码不安全，建议后续优化

# 腾讯云COS配置 (从.env.cos读取)
# 注意：这里假设 .env.cos 文件位于项目根目录下的 vite 文件夹中
$Script:ENV_PATH = Join-Path $PSScriptRoot "..\\vite\\.env.cos" # 路径相对于 deploy_refactored 目录
if (Test-Path $Script:ENV_PATH) {
    Get-Content $Script:ENV_PATH | ForEach-Object {
        if ($_ -match "^\s*([\w.-]+)\s*=\s*(.*)\s*$") {
            $key = $matches[1]
            $value = $matches[2]
            # 将读取到的环境变量也设置为脚本作用域，以便其他脚本访问
            Invoke-Expression "`$Script:$key = '$value'"
        }
    }
} else {
    Write-Warning "未找到 COS 配置文件: $($Script:ENV_PATH)"
    # 根据需要设置默认值或抛出错误
    $Script:COS_SECRET_ID = $null
    $Script:COS_SECRET_KEY = $null
}

# 确保即使文件不存在或内容不匹配，变量也存在（可能为 $null）
if (-not (Get-Variable -Name "Script:COS_SECRET_ID" -ErrorAction SilentlyContinue)) {
    $Script:COS_SECRET_ID = $null
}
if (-not (Get-Variable -Name "Script:COS_SECRET_KEY" -ErrorAction SilentlyContinue)) {
    $Script:COS_SECRET_KEY = $null
}

# 从 vite/version.json 中读取版本号
# 注意：这里假设 version.json 文件位于项目根目录下的 vite 文件夹中
$Script:VERSION_FILE_PATH = Join-Path $PSScriptRoot "..\\vite\\version.json" # 路径相对于 deploy_refactored 目录
try {
    if (Test-Path $Script:VERSION_FILE_PATH) {
        $Script:VERSION = (Get-Content -Path $Script:VERSION_FILE_PATH | ConvertFrom-Json).version
        Write-Host "==== (Config) 读取版本号 version.json：$($Script:VERSION) ====" -ForegroundColor Cyan
    } else {
        throw "版本文件不存在: $($Script:VERSION_FILE_PATH)"
    }
}
catch {
    Write-Host "错误：(Config) 无法读取版本号从 $($Script:VERSION_FILE_PATH) - $_" -ForegroundColor Red
    Write-Host "请检查文件是否存在并包含有效的版本信息。" -ForegroundColor Red
    # 在配置阶段失败通常是致命的，直接退出
    exit 1
}

# 计算完整的镜像标签
$Script:FULL_IMAGE_TAG = "${Script:REGISTRY_URL}/${Script:NAMESPACE}/${Script:IMAGE_NAME}:${Script:VERSION}"

Write-Host "==== (Config) 配置加载完成 ====" -ForegroundColor Green