# deploy_refactored/config.ps1
# 集中管理部署配置

# Create a hashtable to store configuration data
$configData = @{
    REGISTRY_URL      = "crpi-xlafczp8xa83qpr9.cn-beijing.personal.cr.aliyuncs.com"
    NAMESPACE         = "gaoxinkai"
    IMAGE_NAME        = "myapp"
    SERVER_IP         = "101.42.104.145"
    SERVER_USER       = "root"
    SSH_IDENTITY_FILE = "C:\Users\g\.ssh\id_rsa" # 指定 SSH 私钥文件路径
    SERVER_PORT_MAP   = "9850:8085"
    ALIYUN_USER       = "1102336460@qq.com"
    ALIYUN_PASS       = "gxk19990805" # 警告：密码硬编码不安全，建议后续优化
    COS_SECRET_ID     = $null # Initialize to null
    COS_SECRET_KEY    = $null # Initialize to null
    VERSION           = $null # Initialize to null
    FULL_IMAGE_TAG    = $null # Initialize to null
    # Add ENV_PATH for potential use in error messages later
    ENV_PATH          = $null
}

# 腾讯云COS配置 (从.env.cos读取)
$envPath = Join-Path $PSScriptRoot "..\\vite\\.env.cos" # 路径相对于 deploy_refactored 目录
$configData.ENV_PATH = $envPath # Store the calculated path
Write-Host "==== (Debug) [Config] Calculated ENV_PATH: $($envPath) ====" -ForegroundColor Gray
Write-Host "==== (Debug) [Config] PSScriptRoot: $PSScriptRoot ====" -ForegroundColor Gray
if (Test-Path $envPath) {
    Write-Host "==== (Debug) [Config] Found .env.cos file at $($envPath) ====" -ForegroundColor Gray
    Get-Content $envPath | ForEach-Object {
        if ($_ -match "^\s*([\w.-]+)\s*=\s*(.*)\s*$") {
            $key = $matches[1]
            $value = $matches[2]
            Write-Host "==== (Debug) [Config] Reading from .env.cos: $key = *** (value hidden) ***" -ForegroundColor Gray # 不打印真实值
            # Store in the hashtable if the key exists
            if ($configData.ContainsKey($key)) {
                $configData[$key] = $value
                Write-Host "==== (Debug) [Config] Stored '$key' in configData" -ForegroundColor Gray
            } else {
                 Write-Host "==== (Debug) [Config] Key '$key' not expected in configData, ignoring." -ForegroundColor Yellow
            }
        }
    }
    # 在确认文件读取逻辑完成后，再检查一次最终的变量状态
    Write-Host "==== (Debug) [Config] Final check after processing .env.cos:" -ForegroundColor Gray
    Write-Host "==== (Debug) [Config] Final configData.COS_SECRET_ID is null or empty: $($null -eq $configData.COS_SECRET_ID -or [string]::IsNullOrEmpty($configData.COS_SECRET_ID))" -ForegroundColor Gray
    Write-Host "==== (Debug) [Config] Final configData.COS_SECRET_KEY is null or empty: $($null -eq $configData.COS_SECRET_KEY -or [string]::IsNullOrEmpty($configData.COS_SECRET_KEY))" -ForegroundColor Gray
} else {
    Write-Warning "未找到 COS 配置文件: $($envPath)"
    Write-Host "==== (Debug) [Config] .env.cos file NOT found at $($envPath) ====" -ForegroundColor Red
    # Values remain null as initialized
}

# 从 vite/version.json 中读取版本号
$versionFilePath = Join-Path $PSScriptRoot "..\\vite\\version.json" # 路径相对于 deploy_refactored 目录
try {
    if (Test-Path $versionFilePath) {
        $version = (Get-Content -Path $versionFilePath | ConvertFrom-Json).version
        $configData.VERSION = $version # Store in hashtable
        Write-Host "==== (Config) 读取版本号 version.json：$($configData.VERSION) ====" -ForegroundColor Cyan
    } else {
        throw "版本文件不存在: $($versionFilePath)"
    }
}
catch {
    Write-Host "错误：(Config) 无法读取版本号从 $($versionFilePath) - $_" -ForegroundColor Red
    Write-Host "请检查文件是否存在并包含有效的版本信息。" -ForegroundColor Red
    # 在配置阶段失败通常是致命的，直接退出
    exit 1
}

# 计算完整的镜像标签
if ($configData.VERSION) {
    $configData.FULL_IMAGE_TAG = "$($configData.REGISTRY_URL)/$($configData.NAMESPACE)/$($configData.IMAGE_NAME):$($configData.VERSION)"
} else {
     Write-Host "错误：(Config) 无法计算 FULL_IMAGE_TAG 因为 VERSION 未加载。" -ForegroundColor Red
     exit 1
}

Write-Host "==== (Config) 配置加载完成 ====" -ForegroundColor Green

# Return the configuration hashtable
return $configData