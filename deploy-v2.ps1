#Requires -Version 5.1

<#
.SYNOPSIS
  统一部署脚本，用于构建和部署 Web、Native (Android APK) 和 Backend。

.DESCRIPTION
  根据参数执行版本更新、构建、部署/上传等步骤。
  - 版本信息从项目根目录的 versions.config.js 读取。
  - 使用 update-versions.js 脚本更新版本文件并生成 cdn-version.json。
  - 使用 upload-to-cos.cjs 脚本上传资源到腾讯云 COS。
  - 使用 Docker 和 SSH 部署后端服务。

.PARAMETER Target
  (必需) 指定部署目标: 'web', 'native', 'backend', 'all'.

.PARAMETER Environment
  (可选) 指定构建环境，默认为 'production'. 支持 'development', 'production'.

.PARAMETER SkipVersionUpdate
  (可选) 如果指定，则跳过运行 update-versions.js 脚本。

.PARAMETER SkipBuild
  (可选) 如果指定，则跳过所有构建步骤。

.PARAMETER SkipDeploy
  (可选) 如果指定，则跳过所有部署/上传步骤。

.EXAMPLE
  .\deploy-v2.ps1 -Target all 
  部署所有部分 (Web, Native, Backend) 到生产环境。

.EXAMPLE
  .\deploy-v2.ps1 -Target web -SkipBuild
  只部署 Web 资源 (不重新构建)。

.EXAMPLE
  .\deploy-v2.ps1 -Target native -SkipVersionUpdate -SkipDeploy
  只构建 Native APK (不更新版本，不上传)。
#>
param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('web', 'native', 'backend', 'all')]
    [string]$Target,

    [Parameter()]
    [ValidateSet('development', 'production')]
    [string]$Environment = 'production',

    [Parameter()]
    [switch]$SkipVersionUpdate,

    [Parameter()]
    [switch]$SkipBuild,

    [Parameter()]
    [switch]$SkipDeploy
)

# --- 全局变量 ---
$ErrorActionPreference = "Stop" # 命令出错时停止脚本
$ScriptRoot = $PSScriptRoot
$DistDir = Join-Path $ScriptRoot "dist" # 构建产物和生成文件的目录
$DotEnvPath = Join-Path $ScriptRoot ".env" # .env 文件路径 (虽然不再直接解析，但路径可能仍有用)
# load-env.js 已移除，将在下面直接解析 .env 文件

# --- Helper 函数 ---

function Invoke-CommandWithErrorCheck {
    param(
        [Parameter(Mandatory=$true)]
        [scriptblock]$Command,
        [Parameter(Mandatory=$true)]
        [string]$ErrorMessage
    )
    & $Command
    if ($LASTEXITCODE -ne 0) {
        Write-Error $ErrorMessage
        exit 1
    }
}

# --- 加载配置 (直接解析 .env 文件) ---
Write-Host "直接解析 .env 文件..." -ForegroundColor DarkGray
$psEnvConfig = @{} # 使用 PowerShell 哈希表存储配置
if (-not (Test-Path $DotEnvPath)) {
    Write-Error ".env 文件未找到: $DotEnvPath"
    exit 1
}

try {
    Get-Content $DotEnvPath -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        # 跳过注释和空行
        if ($line -like '#*' -or -not $line) {
            return # 继续下一行
        }
        # 查找第一个 '='
        $equalsIndex = $line.IndexOf('=')
        if ($equalsIndex -gt 0) {
            $key = $line.Substring(0, $equalsIndex).Trim()
            $rawValue = $line.Substring($equalsIndex + 1).Trim()
            # Strip inline comments: take everything before the first '#'
            $value = ($rawValue -split '#', 2)[0].Trim()

            # 移除可能存在的引号 (现在 $value should not contain the comment)
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $psEnvConfig[$key] = $value # Store the cleaned value
        }
    }
} catch {
    Write-Error "解析 .env 文件时出错: $_"
    exit 1
}

# --- 配置变量 (从解析的哈希表获取) ---
$ALIYUN_REGISTRY_URL = $psEnvConfig.ALIYUN_REGISTRY_URL
$ALIYUN_NAMESPACE = $psEnvConfig.ALIYUN_NAMESPACE
$ALIYUN_IMAGE_NAME = $psEnvConfig.ALIYUN_IMAGE_NAME
$ALIYUN_USER = $psEnvConfig.ALIYUN_USER
$ALIYUN_PASS = $psEnvConfig.ALIYUN_PASS

$SERVER_IP = $psEnvConfig.DEPLOY_SERVER_IP
$SERVER_USER = $psEnvConfig.DEPLOY_SERVER_USER
$SERVER_PORT_MAP = $psEnvConfig.DEPLOY_SERVER_PORT_MAP

# 检查必要的配置是否存在 (在解析 .env 后检查)
if (-not ($ALIYUN_REGISTRY_URL -and $ALIYUN_NAMESPACE -and $ALIYUN_IMAGE_NAME -and $ALIYUN_USER -and $ALIYUN_PASS -and $SERVER_IP -and $SERVER_USER -and $SERVER_PORT_MAP)) {
    Write-Error "错误：未能从 .env 文件获取所有必要的部署配置。请检查 .env 文件内容。"
    exit 1
}
Write-Host "部署配置加载成功。" -ForegroundColor Green
# 注意: COS 配置由 upload-to-cos.cjs 脚本自行从 .env 加载 (假设它也能自行解析)

# --- 主逻辑 ---
Write-Host "=================================================="
Write-Host "开始部署..."
Write-Host "目标: $Target | 环境: $Environment"
Write-Host "跳过版本更新: $SkipVersionUpdate | 跳过构建: $SkipBuild | 跳过部署: $SkipDeploy"
Write-Host "=================================================="

# 定义版本变量
$WebVersion = $null
$NativeVersionName = $null
$NativeVersionCode = $null
$ApkFileName = $null

# --- 步骤 0: 版本更新 ---
Write-Host "`n=== 步骤 0: 版本更新 ===" -ForegroundColor Cyan
if (-not $SkipVersionUpdate) {
    Write-Host "运行 update-versions.js 脚本..."
    Invoke-CommandWithErrorCheck -Command { node (Join-Path $ScriptRoot "update-versions.js") } -ErrorMessage "update-versions.js 脚本执行失败!"
    
    # 重新读取配置以获取更新后的版本 (Node.js 脚本可能修改了文件)
    # PowerShell 无法直接 `require` js 文件，这里用一种间接方式读取
    # 或者让 node 脚本输出 json, PowerShell 解析
    # 简单起见，这里假设 node 脚本成功更新了 build.gradle，我们从 config 文件读取
    try {
         # 清除 require 缓存 (如果 PowerShell 缓存了的话，虽然不太可能)
         # Remove-Module versions.config -Force -ErrorAction SilentlyContinue 
         # $config = Import-PowerShellDataFile (Join-Path $ScriptRoot "versions.config.js") # 这不适用于 module.exports
         # 改用 node 执行来获取配置值
         $configJson = Invoke-CommandWithErrorCheck -Command { node -p "JSON.stringify(require('./versions.config.js'))" } -ErrorMessage "无法读取 versions.config.js"
         $config = $configJson | ConvertFrom-Json
         $WebVersion = $config.webVersion
         $NativeVersionName = $config.nativeVersionName
         $NativeVersionCode = $config.nativeVersionCode
         $ApkFileName = $config.apkFileNamePattern.Replace('{versionName}', $NativeVersionName)
         Write-Host "读取版本信息: Web=$WebVersion, Native=$NativeVersionName ($NativeVersionCode), APK=$ApkFileName" -ForegroundColor Green
    } catch {
         Write-Error "无法从 versions.config.js 读取版本信息: $_"
         exit 1
    }
    Write-Host "版本文件更新完成。" -ForegroundColor Green
} else {
    Write-Host "跳过版本文件更新。" -ForegroundColor Yellow
    # 如果跳过更新，仍需读取版本用于后续步骤
     try {
         $configJson = Invoke-CommandWithErrorCheck -Command { node -p "JSON.stringify(require('./versions.config.js'))" } -ErrorMessage "无法读取 versions.config.js"
         $config = $configJson | ConvertFrom-Json
         $WebVersion = $config.webVersion
         $NativeVersionName = $config.nativeVersionName
         $NativeVersionCode = $config.nativeVersionCode
         $ApkFileName = $config.apkFileNamePattern.Replace('{versionName}', $NativeVersionName)
         Write-Host "读取当前版本信息: Web=$WebVersion, Native=$NativeVersionName ($NativeVersionCode), APK=$ApkFileName" -ForegroundColor Green
    } catch {
         Write-Error "无法从 versions.config.js 读取版本信息: $_"
         exit 1
    }
}

# 检查是否成功读取版本信息
if (-not ($WebVersion -and $NativeVersionName -and $NativeVersionCode -and $ApkFileName)) {
     Write-Error "未能成功读取所有必要的版本信息，脚本终止。"
     exit 1
}

# --- 步骤 1: 构建后端 ---
Write-Host "`n=== 步骤 1: 构建后端 ===" -ForegroundColor Cyan
if (-not $SkipBuild -and ($Target -eq 'backend' -or $Target -eq 'all')) {
    $backendDir = Join-Path $ScriptRoot "mine-api"
    Write-Host "进入目录: $backendDir"
    Push-Location $backendDir
    
    Write-Host "运行 Maven 构建..."
    # 使用 Maven Wrapper (mvnw.cmd) 进行构建
    Invoke-CommandWithErrorCheck -Command { .\mvnw.cmd clean package -DskipTests } -ErrorMessage "Maven Wrapper 构建失败!"
    
    # 使用从 .env 加载的镜像名，并用 ${} 包裹变量
    $dockerImageTag = "$($ALIYUN_IMAGE_NAME):${NativeVersionName}"
    Write-Host "构建 Docker 镜像: $dockerImageTag ..."
    Invoke-CommandWithErrorCheck -Command { docker build -t $dockerImageTag . } -ErrorMessage "Docker build 失败!"
    
    Write-Host "后端构建完成。" -ForegroundColor Green
    Pop-Location
} else {
    Write-Host "跳过后端构建。" -ForegroundColor Yellow
}

# --- 步骤 2: 构建前端 ---
Write-Host "`n=== 步骤 2: 构建前端 ===" -ForegroundColor Cyan
if (-not $SkipBuild -and ($Target -eq 'web' -or $Target -eq 'all')) {
    $frontendDir = Join-Path $ScriptRoot "vite"
    Write-Host "进入目录: $frontendDir"
    Push-Location $frontendDir

    Write-Host "运行 npm install (确保依赖最新)..." # 添加 npm install
    Invoke-CommandWithErrorCheck -Command { npm install } -ErrorMessage "npm install 失败!"

    $buildScript = "build:${Environment}" # 使用花括号明确变量名
    Write-Host "运行前端构建脚本: npm run $buildScript ..."
    Invoke-CommandWithErrorCheck -Command { npm run $buildScript } -ErrorMessage "前端构建 (npm run $buildScript) 失败!"
    
    Write-Host "前端构建完成。" -ForegroundColor Green
    Pop-Location
} else {
    Write-Host "跳过前端构建。" -ForegroundColor Yellow
}

# --- 步骤 3: 构建原生 APK ---
Write-Host "`n=== 步骤 3: 构建原生 APK ===" -ForegroundColor Cyan
if (-not $SkipBuild -and ($Target -eq 'native' -or $Target -eq 'all')) {
    $androidDir = Join-Path $ScriptRoot "vite/android"
    Write-Host "进入目录: $androidDir"
    Push-Location $androidDir

    Write-Warning "请确保 Android SDK 和 Gradle 环境配置正确，并且签名配置已完成！"
    Write-Host "运行 Gradle 构建 (assembleRelease)..."
    # 在 Windows 上，通常直接运行 gradlew.bat
    Invoke-CommandWithErrorCheck -Command { .\gradlew.bat assembleRelease } -ErrorMessage "Gradle assembleRelease 失败!" 
    
    # 复制 APK
    $sourceApk = Join-Path $androidDir "app/build/outputs/apk/release/app-release.apk"
    $destApk = Join-Path $DistDir $ApkFileName # 使用从 config 生成的文件名
    if (Test-Path $sourceApk) {
        Write-Host "复制 APK 到 $destApk ..."
        Copy-Item -Path $sourceApk -Destination $destApk -Force
        Write-Host "APK 复制完成。" -ForegroundColor Green
    } else {
        Write-Error "构建的 APK 文件未找到: $sourceApk"
        Pop-Location
        exit 1
    }

    Pop-Location
} else {
    Write-Host "跳过原生 APK 构建。" -ForegroundColor Yellow
}

# --- 步骤 4: 部署后端 ---
Write-Host "`n=== 步骤 4: 部署后端 ===" -ForegroundColor Cyan
if (-not $SkipDeploy -and ($Target -eq 'backend' -or $Target -eq 'all')) {
    # 使用从 .env 加载的镜像名，并用 ${} 包裹变量
    $localDockerImage = "$($ALIYUN_IMAGE_NAME):${NativeVersionName}" # 确保这里也一致
    $aliyunImageTag = "$ALIYUN_REGISTRY_URL/$ALIYUN_NAMESPACE/$($ALIYUN_IMAGE_NAME):${NativeVersionName}" # 使用 ${} 包裹变量

    Write-Host "登录 Aliyun Docker Registry..."
    Invoke-CommandWithErrorCheck -Command { docker login $ALIYUN_REGISTRY_URL -u $ALIYUN_USER -p $ALIYUN_PASS } -ErrorMessage "Docker login 失败!"
    
    Write-Host "标记 Docker 镜像: $aliyunImageTag ..."
    Invoke-CommandWithErrorCheck -Command { docker tag $localDockerImage $aliyunImageTag } -ErrorMessage "Docker tag 失败!"

    Write-Host "推送 Docker 镜像到 Aliyun..."
    Invoke-CommandWithErrorCheck -Command { docker push $aliyunImageTag } -ErrorMessage "Docker push 失败!"

    Write-Host "通过 SSH 在服务器 ($SERVER_IP) 上部署..."
    # 在 SSH 命令字符串中使用 ${} 包裹变量更安全
    $sshCommand = "docker login $ALIYUN_REGISTRY_URL -u $ALIYUN_USER -p $ALIYUN_PASS && " +
                  "docker pull ${aliyunImageTag} && " +
                  "docker stop ${ALIYUN_IMAGE_NAME} || true && " + # 忽略停止失败
                  "docker rm ${ALIYUN_IMAGE_NAME} || true && " +   # 忽略删除失败
                  # Reverted single quotes around port mapping, keep for others
                  "docker run -d -p $SERVER_PORT_MAP --name '${ALIYUN_IMAGE_NAME}' '${aliyunImageTag}'"
    Write-Host "执行 SSH 命令: $sshCommand"
    # 注意：确保 SSH 客户端已安装并配置好，或者使用 PowerShell SSH 模块
    Invoke-CommandWithErrorCheck -Command { ssh "$SERVER_USER@$SERVER_IP" $sshCommand } -ErrorMessage "SSH 部署失败!"

    Write-Host "后端部署完成。" -ForegroundColor Green
} else {
    Write-Host "跳过后端部署。" -ForegroundColor Yellow
}

# --- 步骤 5: 部署/上传前端和配置 ---
Write-Host "`n=== 步骤 5: 部署/上传前端资源 ===" -ForegroundColor Cyan
if (-not $SkipDeploy) {
    $uploadScript = Join-Path $ScriptRoot "vite/upload-to-cos.cjs"
    
    if ($Target -eq 'web' -or $Target -eq 'all') {
        Write-Host "上传 Web 静态资源 (vite/dist/)..."
        $webSource = "vite/dist"
        $webTarget = "/" # 上传到 Bucket 根目录 (根据需要调整)
        $ArgumentList = @("`"$uploadScript`"", "--type", "web", "--source", "`"$webSource`"", "--target", "`"$webTarget`"")
        # Write-Host "执行: node $($ArgumentList -join ' ')" # 移除调试输出
        Start-Process node -ArgumentList $ArgumentList -Wait -NoNewWindow
        if ($LASTEXITCODE -ne 0) { Write-Error "上传 Web 资源失败!"; exit 1 }
        Write-Host "Web 资源上传完成。" -ForegroundColor Green
    }
    
    if ($Target -eq 'native' -or $Target -eq 'all') {
        Write-Host "上传 APK 文件 ($ApkFileName)..."
        $apkSource = Join-Path $DistDir $ApkFileName # Use $DistDir
        $apkTarget = "/$ApkFileName" # 上传到 Bucket 根目录下的同名文件 (根据需要调整)
        $ArgumentList = @("`"$uploadScript`"", "--type", "apk", "--source", "`"$apkSource`"", "--target", "`"$apkTarget`"")
        # Write-Host "执行: node $($ArgumentList -join ' ')" # 移除调试输出
        Start-Process node -ArgumentList $ArgumentList -Wait -NoNewWindow
        if ($LASTEXITCODE -ne 0) { Write-Error "上传 APK 文件失败!"; exit 1 }
        Write-Host "APK 文件上传完成。" -ForegroundColor Green
    }

    if ($Target -eq 'web' -or $Target -eq 'native' -or $Target -eq 'all') {
         Write-Host "上传版本配置文件 (dist/cdn-version.json) 到 CDN (/version.json)..."
         $versionSource = Join-Path $DistDir "cdn-version.json" # Use $DistDir
         $versionTarget = "/version.json" # CDN 上的目标路径
         $ArgumentList = @("`"$uploadScript`"", "--type", "version", "--source", "`"$versionSource`"", "--target", "`"$versionTarget`"")
         # Write-Host "执行: node $($ArgumentList -join ' ')" # 移除调试输出
         Start-Process node -ArgumentList $ArgumentList -Wait -NoNewWindow
         $exitCode = $LASTEXITCODE # Assign to temporary variable
         if ($exitCode -ne 0) { Write-Error "上传版本配置文件失败! (退出码: $exitCode)"; exit 1 } # Check temporary variable
         Write-Host "版本配置文件上传完成。" -ForegroundColor Green
    }

} else {
    Write-Host "跳过部署/上传步骤。" -ForegroundColor Yellow
}

Write-Host "`n=================================================="
Write-Host "部署脚本执行完毕！" -ForegroundColor Green
Write-Host "=================================================="
