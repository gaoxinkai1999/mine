# 综合部署脚本 - 后端mine-api和前端vite项目

# ====== 可配置参数 ======
$REGISTRY_URL = "crpi-xlafczp8xa83qpr9.cn-beijing.personal.cr.aliyuncs.com"
$NAMESPACE = "gaoxinkai"
$IMAGE_NAME = "myapp"
$SERVER_IP = "101.42.104.145"
$SERVER_USER = "root"
$SERVER_PORT_MAP = "9850:8085"
$ALIYUN_USER = "1102336460@qq.com"
$ALIYUN_PASS = "gxk19990805"

# 腾讯云COS配置 (从.env.cos读取)
$ENV_PATH = Join-Path $PSScriptRoot "vite\.env.cos"
if (Test-Path $ENV_PATH) {
    Get-Content $ENV_PATH | ForEach-Object {
        if ($_ -match "^\s*([\w.-]+)\s*=\s*(.*)\s*$") {
            $key = $matches[1]
            $value = $matches[2]
            Set-Item -Path "env:$key" -Value $value
        }
    }
}
$COS_SECRET_ID = $env:COS_SECRET_ID
$COS_SECRET_KEY = $env:COS_SECRET_KEY
# ========================

# 切换到脚本所在目录
Set-Location $PSScriptRoot

# 从 vite/version.json 中读取版本号
try {
    $VERSION = (Get-Content -Path "vite\version.json" | ConvertFrom-Json).version
    Write-Host "==== 读取版本号 version.json：$VERSION ===="
}
catch {
    Write-Host "错误：无法读取版本号从 vite/version.json - $_" -ForegroundColor Red
    Write-Host "请检查文件是否存在并包含有效的版本信息。" -ForegroundColor Red
    Pause
    exit 1
}

$FULL_IMAGE_TAG = "${REGISTRY_URL}/${NAMESPACE}/${IMAGE_NAME}:${VERSION}"

Write-Host "==== Step 1: 清除并构建后端 Jar 包 ===="
try {
    Push-Location mine-api
    & .\mvnw.cmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        throw "Maven 构建失败"
    }
    Pop-Location
    Write-Host "后端 Jar 包构建完成" -ForegroundColor Green
} catch {
    Write-Host "错误：Maven 构建失败 - $_" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== Step 2: 构建 Docker 镜像 ===="
try {
    docker build -t "${IMAGE_NAME}:${VERSION}" ./mine-api
    if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }
}
catch {
    Write-Host "错误：构建Docker镜像失败 - $_" -ForegroundColor Red
    Write-Host "请检查Docker环境和mine-api目录下的Dockerfile。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== Step 3: 镜像打标签 ===="
try {
    docker tag "${IMAGE_NAME}:${VERSION}" $FULL_IMAGE_TAG
    if ($LASTEXITCODE -ne 0) { throw "Docker tag failed" }
}
catch {
    Write-Host "错误：Docker镜像打标签失败 - $_" -ForegroundColor Red
    Write-Host "请检查Docker镜像是否存在。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== Step 4: 登录阿里云仓库 ===="
try {
    docker login $REGISTRY_URL -u $ALIYUN_USER -p $ALIYUN_PASS
    if ($LASTEXITCODE -ne 0) { throw "Docker login failed" }
}
catch {
    Write-Host "错误：登录阿里云仓库失败 - $_" -ForegroundColor Red
    Write-Host "请检查用户名和密码是否正确，以及网络连接是否正常。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== Step 5: 推送镜像到仓库 ===="
try {
    docker push $FULL_IMAGE_TAG
    if ($LASTEXITCODE -ne 0) { throw "Docker push failed" }
}
catch {
    Write-Host "错误：推送镜像到阿里云仓库失败 - $_" -ForegroundColor Red
    Write-Host "请检查网络连接和镜像标签是否正确。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== Step 6: SSH到服务器，拉取镜像并运行容器 ===="
# 注意：PowerShell中直接执行SSH命令需要安装OpenSSH或者使用第三方工具，这里模拟bash脚本的写法
# 实际执行可能需要调整为使用PowerShell的SSH模块或工具
try {
    ssh "${SERVER_USER}@${SERVER_IP}" "docker login ${REGISTRY_URL} -u ${ALIYUN_USER} -p ${ALIYUN_PASS} && docker pull ${FULL_IMAGE_TAG} && docker stop ${IMAGE_NAME} || true && docker rm ${IMAGE_NAME} || true && docker run -d -p ${SERVER_PORT_MAP} --name ${IMAGE_NAME} ${FULL_IMAGE_TAG}"
    if ($LASTEXITCODE -ne 0) { throw "SSH deployment failed" }
}
catch {
    Write-Host "错误：SSH到服务器部署失败 - $_" -ForegroundColor Red
    Write-Host "请检查SSH连接、服务器环境以及Docker命令是否正确。" -ForegroundColor Red
    Write-Host "注意：如果您的PowerShell环境不支持SSH命令，可能需要安装OpenSSH或使用其他工具。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== 自动化完成，后端镜像版本号：$VERSION，端口映射：$SERVER_PORT_MAP ===="

# 前端vite项目构建和上传到腾讯云COS，基于"构建项目.ps1"逻辑
Write-Host "==== Step 7: 构建vite前端项目并上传到腾讯云COS ===="
Set-Location vite

# 生产环境构建前，自动删除 capacitor.config.json 中的 server.url
Write-Host "移除生产环境下的 Capacitor 远程调试 URL..." -ForegroundColor Cyan
$capConfigPath = "capacitor.config.json"
try {
    $capJson = Get-Content $capConfigPath -Raw | ConvertFrom-Json
    if ($capJson.server.url) {
        $capJson.server.PSObject.Properties.Remove('url')
        $capJson | ConvertTo-Json -Depth 10 | Set-Content $capConfigPath -Encoding UTF8
        Write-Host "已移除 server.url 字段，确保生产环境加载本地资源" -ForegroundColor Green
    } else {
        Write-Host "server.url 字段不存在，无需移除" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "错误：处理 capacitor.config.json 失败 - $_" -ForegroundColor Red
    Write-Host "请检查文件是否存在并包含有效的JSON内容。" -ForegroundColor Red
    Pause
    exit 1
}

# 同步安卓原生版本号和版本名
Write-Host "同步安卓原生版本号..." -ForegroundColor Cyan
$versionJsonPath = "version.json"
try {
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
}
catch {
    Write-Host "错误：同步安卓版本号失败 - $_" -ForegroundColor Red
    Write-Host "请检查version.json和build.gradle文件是否存在并包含有效内容。" -ForegroundColor Red
    Pause
    exit 1
}

# 设置变量
$BuildType = "release"

# 前端构建
Write-Host "正在构建前端代码..." -ForegroundColor Cyan
try {
    npm run build:prod
    if ($LASTEXITCODE -ne 0) { throw "npm build failed" }
}
catch {
    Write-Host "错误：构建前端代码失败 - $_" -ForegroundColor Red
    Write-Host "请检查npm环境和构建脚本是否正确。" -ForegroundColor Red
    Pause
    exit 1
}

# Capacitor同步
Write-Host "正在同步插件和原生代码..." -ForegroundColor Cyan
try {
    npx cap sync
    if ($LASTEXITCODE -ne 0) { throw "cap sync failed" }
}
catch {
    Write-Host "错误：同步Capacitor插件和原生代码失败 - $_" -ForegroundColor Red
    Write-Host "请检查Capacitor环境是否正确配置。" -ForegroundColor Red
    Pause
    exit 1
}

# 构建APK
Write-Host "正在构建Android APK ($BuildType)..." -ForegroundColor Cyan
$OutputApkName = ""
Set-Location android
try {
    ./gradlew assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed" }
}
catch {
    Write-Host "错误：构建Release APK失败 - $_" -ForegroundColor Red
    Write-Host "请检查Android构建环境和Gradle配置。" -ForegroundColor Red
    Set-Location ..
    Pause
    exit 1
}
$SourceApkPath = "./app/build/outputs/apk/release/app-release.apk"
Set-Location ..
# 复制APK到根目录
Copy-Item -Path "android/$SourceApkPath" -Destination "./$OutputApkName"
Write-Host "Release APK已复制到项目根目录: ./$OutputApkName" -ForegroundColor Green

# 上传APK和版本信息到腾讯云COS
Write-Host "开始上传APK和版本信息到腾讯云COS..." -ForegroundColor Cyan
try {
    node upload-to-cos.cjs
    if ($LASTEXITCODE -ne 0) { throw "upload to COS failed" }
    Write-Host "上传完成" -ForegroundColor Green
}
catch {
    Write-Host "错误：上传APK和版本信息到腾讯云COS失败 - $_" -ForegroundColor Red
    Write-Host "请检查网络连接和COS配置是否正确。" -ForegroundColor Red
    Pause
    exit 1
}

# 构建完成后，恢复开发环境的 server.url 为固定地址
try {
    $capConfigPath = "capacitor.config.json"
    $capJsonRestore = Get-Content $capConfigPath -Raw | ConvertFrom-Json
    if ($capJsonRestore.server.PSObject.Properties['url']) {
        $capJsonRestore.server.url = "http://192.168.0.102:5173"
    } else {
        $capJsonRestore.server | Add-Member -MemberType NoteProperty -Name "url" -Value "http://192.168.0.102:5173"
    }
    $capJsonRestore | ConvertTo-Json -Depth 10 | Set-Content $capConfigPath -Encoding UTF8
    Write-Host "已将 capacitor.config.json 中的 server.url 恢复为开发环境地址 http://192.168.0.102:5173" -ForegroundColor Green
}
catch {
    Write-Host "错误：恢复 capacitor.config.json 中的 server.url 失败 - $_" -ForegroundColor Red
    Write-Host "请检查文件是否可写。" -ForegroundColor Red
    Pause
    exit 1
}

Write-Host "==== 全部部署完成，后端版本号：$VERSION，前端APK已构建并上传至腾讯云COS ===="
Set-Location $PSScriptRoot