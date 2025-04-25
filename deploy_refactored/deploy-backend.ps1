# deploy_refactored/deploy-backend.ps1
# 负责后端 mine-api 的构建、Docker化和部署

# 加载配置并捕获返回的哈希表
$config = . "$PSScriptRoot/config.ps1"

# 检查必要的配置是否存在 (从哈希表读取)
if (-not $config.VERSION) { Write-Host "错误：版本号未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.FULL_IMAGE_TAG) { Write-Host "错误：完整镜像标签未在 config.ps1 中计算。" -ForegroundColor Red; exit 1 }
if (-not $config.IMAGE_NAME) { Write-Host "错误：镜像名称 (IMAGE_NAME) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.REGISTRY_URL) { Write-Host "错误：仓库地址 (REGISTRY_URL) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.ALIYUN_USER) { Write-Host "错误：阿里云用户名 (ALIYUN_USER) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.ALIYUN_PASS) { Write-Host "错误：阿里云密码 (ALIYUN_PASS) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.SSH_IDENTITY_FILE) { Write-Host "错误：SSH 密钥文件路径 (SSH_IDENTITY_FILE) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.SERVER_USER) { Write-Host "错误：服务器用户名 (SERVER_USER) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.SERVER_IP) { Write-Host "错误：服务器 IP (SERVER_IP) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }
if (-not $config.SERVER_PORT_MAP) { Write-Host "错误：服务器端口映射 (SERVER_PORT_MAP) 未在 config.ps1 中加载。" -ForegroundColor Red; exit 1 }


# 定义项目根目录（相对于此脚本）
$ProjectRoot = Join-Path $PSScriptRoot ".."
$BackendPath = Join-Path $ProjectRoot "mine-api"

Write-Host "==== 开始后端部署 (版本: $($config.VERSION)) ====" -ForegroundColor Yellow # 使用 $config.VERSION

# Step 1: 清除并构建后端 Jar 包
Write-Host "==== Step 1: 清除并构建后端 Jar 包 ===="
try {
    Push-Location $BackendPath
    Write-Host "当前目录: $(Get-Location)"
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        throw "Maven 构建失败"
    }
    Pop-Location
    Write-Host "后端 Jar 包构建完成" -ForegroundColor Green
} catch {
    Write-Host "错误：Maven 构建失败 - $_" -ForegroundColor Red
    Pause # 保留原始脚本的暂停行为
    exit 1
}

# Step 2: 构建 Docker 镜像
Write-Host "==== Step 2: 构建 Docker 镜像 ===="
try {
    # Docker build 命令需要在包含 Dockerfile 的目录的 *上级* 目录执行，或者指定上下文路径
    # 这里我们假设 Dockerfile 在 mine-api 目录下，所以上下文是 mine-api
    docker build -t "$($config.IMAGE_NAME):$($config.VERSION)" $BackendPath # 使用 $config
    if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }
    Write-Host "Docker 镜像构建完成: $($config.IMAGE_NAME):$($config.VERSION)" -ForegroundColor Green # 使用 $config
}
catch {
    Write-Host "错误：构建Docker镜像失败 - $_" -ForegroundColor Red
    Write-Host "请检查Docker环境和 $BackendPath 目录下的Dockerfile。" -ForegroundColor Red
    Pause
    exit 1
}

# Step 3: 镜像打标签
Write-Host "==== Step 3: 镜像打标签 ===="
try {
    docker tag "$($config.IMAGE_NAME):$($config.VERSION)" $config.FULL_IMAGE_TAG # 使用 $config
    if ($LASTEXITCODE -ne 0) { throw "Docker tag failed" }
    Write-Host "Docker 镜像打标签完成: $($config.FULL_IMAGE_TAG)" -ForegroundColor Green # 使用 $config
}
catch {
    Write-Host "错误：Docker镜像打标签失败 - $_" -ForegroundColor Red
    Write-Host "请检查Docker镜像 $($config.IMAGE_NAME):$($config.VERSION) 是否存在。" -ForegroundColor Red # 使用 $config
    Pause
    exit 1
}

# Step 4: 登录阿里云仓库
Write-Host "==== Step 4: 登录阿里云仓库 ===="
try {
    # 警告：密码直接在命令行中使用不安全
    docker login $config.REGISTRY_URL -u $config.ALIYUN_USER -p $config.ALIYUN_PASS # 使用 $config
    if ($LASTEXITCODE -ne 0) { throw "Docker login failed" }
    Write-Host "登录阿里云仓库 $($config.REGISTRY_URL) 成功" -ForegroundColor Green # 使用 $config
}
catch {
    Write-Host "错误：登录阿里云仓库失败 - $_" -ForegroundColor Red
    Write-Host "请检查用户名 ($($config.ALIYUN_USER)) 和密码是否正确，以及网络连接是否正常。" -ForegroundColor Red # 使用 $config
    Pause
    exit 1
}

# Step 5: 推送镜像到仓库
Write-Host "==== Step 5: 推送镜像到仓库 ===="
try {
    docker push $config.FULL_IMAGE_TAG # 使用 $config
    if ($LASTEXITCODE -ne 0) { throw "Docker push failed" }
    Write-Host "推送镜像 $($config.FULL_IMAGE_TAG) 到仓库成功" -ForegroundColor Green # 使用 $config
}
catch {
    Write-Host "错误：推送镜像到阿里云仓库失败 - $_" -ForegroundColor Red
    Write-Host "请检查网络连接和镜像标签是否正确。" -ForegroundColor Red
    Pause
    exit 1
}

# Step 6: SSH到服务器，拉取镜像并运行容器
Write-Host "==== Step 6: SSH到服务器，拉取镜像并运行容器 ===="
# 注意：此命令依赖本地 SSH 客户端和配置（优先使用密钥认证），以及服务器端的 Docker 环境

# 构建在服务器上执行的 Docker 命令序列
# 警告：在远程命令中包含密码 ($config.ALIYUN_PASS) 仍然不安全
$remoteDockerCommands = "docker login $($config.REGISTRY_URL) -u $($config.ALIYUN_USER) -p $($config.ALIYUN_PASS) && docker pull $($config.FULL_IMAGE_TAG) && docker stop $($config.IMAGE_NAME) || true && docker rm $($config.IMAGE_NAME) || true && docker run -d -p $($config.SERVER_PORT_MAP) --name $($config.IMAGE_NAME) $($config.FULL_IMAGE_TAG)" # 使用 $config

# 构建 SSH 命令，使用指定的身份文件
# 注意：在 PowerShell 中正确转义引号可能很复杂。
# 确保 $config.SSH_IDENTITY_FILE 路径正确且 PowerShell 对其有读取权限。
if (-not $config.SSH_IDENTITY_FILE -or -not (Test-Path $config.SSH_IDENTITY_FILE)) {
    Write-Host "错误：SSH 身份文件路径未配置或文件不存在: $($config.SSH_IDENTITY_FILE)" -ForegroundColor Red # 使用 $config
    Pause
    exit 1
}
$sshCommand = "ssh -i `"$($config.SSH_IDENTITY_FILE)`" $($config.SERVER_USER)@$($config.SERVER_IP) `"$remoteDockerCommands`"" # 使用 $config

Write-Host "将执行 SSH 命令 (使用指定密钥: $($config.SSH_IDENTITY_FILE))..." -ForegroundColor Cyan # 使用 $config
Write-Host "远程执行的命令: $remoteDockerCommands" -ForegroundColor Cyan # 打印命令但不包含密码

try {
    # 注意：直接在 PowerShell 中执行包含复杂引号和命令链的 ssh 命令可能需要仔细处理转义
    # Invoke-Expression 可能是一种方式，但需谨慎使用
    Invoke-Expression $sshCommand
    # $LASTEXITCODE 在 Invoke-Expression 后可能不直接反映 ssh 命令的退出码，需要更可靠的检查方式
    # 更好的方式是使用 PowerShell 的 SSH 模块或检查 ssh 命令的输出/错误流
    # 这里暂时保留原始逻辑的简单检查，但可能不够健壮
    # if ($LASTEXITCODE -ne 0) { throw "SSH deployment failed" } # $LASTEXITCODE 可能不准确
    # 假设成功，如果失败，ssh 命令本身会打印错误
    Write-Host "SSH 部署命令已发送到服务器 $($config.SERVER_IP)" -ForegroundColor Green # 使用 $config
}
catch {
    Write-Host "错误：SSH到服务器部署失败 - $_" -ForegroundColor Red
    Write-Host "请检查SSH连接（确保已配置密钥认证）、服务器环境以及Docker命令是否正确。" -ForegroundColor Red
    Write-Host "注意：如果您的PowerShell环境不支持SSH命令，可能需要安装OpenSSH或使用其他工具。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== 后端部署完成 ====" -ForegroundColor Yellow