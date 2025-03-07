Write-Host "正在创建项目DDD目录结构..." -ForegroundColor Green

# 创建主要目录结构
New-Item -Path "src\main\java\com\example\domain" -ItemType Directory -Force
New-Item -Path "src\main\java\com\example\infrastructure" -ItemType Directory -Force
New-Item -Path "src\main\java\com\example\application" -ItemType Directory -Force
New-Item -Path "src\main\java\com\example\interfaces" -ItemType Directory -Force

# 创建领域层各业务领域目录
$domains = @("order", "product", "inventory", "shop", "purchase", "statistics", "price", "batch")
foreach ($domain in $domains) {
    New-Item -Path "src\main\java\com\example\domain\$domain" -ItemType Directory -Force
}

# 为每个业务领域创建子目录
$subDirs = @("controller", "service", "repository", "entity", "dto", "mapper")
foreach ($domain in $domains) {
    foreach ($subDir in $subDirs) {
        New-Item -Path "src\main\java\com\example\domain\$domain\$subDir" -ItemType Directory -Force
    }
}

# 为统计领域额外创建forecast目录
New-Item -Path "src\main\java\com\example\domain\statistics\service\forecast" -ItemType Directory -Force

# 创建基础设施层子目录
$infraDirs = @("config", "common", "exception", "security")
foreach ($infraDir in $infraDirs) {
    New-Item -Path "src\main\java\com\example\infrastructure\$infraDir" -ItemType Directory -Force
}

# 创建应用服务层目录
New-Item -Path "src\main\java\com\example\application\service" -ItemType Directory -Force

Write-Host "目录结构创建完成!" -ForegroundColor Green
Write-Host "按任意键继续..."
$host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") | Out-Null