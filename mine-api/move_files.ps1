Write-Host "正在移动文件到新的目录结构..." -ForegroundColor Green

# 定义一个移动文件的函数，如果源文件存在则移动
function Move-FileIfExists {
    param (
        [string]$Source,
        [string]$Destination
    )
    
    if (Test-Path $Source) {
        $destDir = Split-Path -Parent $Destination
        if (-not (Test-Path $destDir)) {
            New-Item -Path $destDir -ItemType Directory -Force | Out-Null
        }
        Move-Item -Path $Source -Destination $Destination -Force
        Write-Host "已移动: $Source -> $Destination" -ForegroundColor Cyan
    } else {
        Write-Host "未找到源文件: $Source" -ForegroundColor Yellow
    }
}

# 移动订单相关文件
Write-Host "移动订单领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\OrderController.java" "src\main\java\com\example\domain\order\controller\OrderController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\OrderService.java" "src\main\java\com\example\domain\order\service\OrderService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Order.java" "src\main\java\com\example\domain\order\entity\Order.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\OrderDetail.java" "src\main\java\com\example\domain\order\entity\OrderDetail.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\ReturnOrder.java" "src\main\java\com\example\domain\order\entity\ReturnOrder.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\ReturnOrderDetail.java" "src\main\java\com\example\domain\order\entity\ReturnOrderDetail.java"

# 移动产品相关文件
Write-Host "移动产品领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\ProductController.java" "src\main\java\com\example\domain\product\controller\ProductController.java"
Move-FileIfExists "src\main\java\com\example\modules\controller\CategoryController.java" "src\main\java\com\example\domain\product\controller\CategoryController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\ProductService.java" "src\main\java\com\example\domain\product\service\ProductService.java"
Move-FileIfExists "src\main\java\com\example\modules\service\CategoryService.java" "src\main\java\com\example\domain\product\service\CategoryService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Product.java" "src\main\java\com\example\domain\product\entity\Product.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Category.java" "src\main\java\com\example\domain\product\entity\Category.java"

# 移动库存相关文件
Write-Host "移动库存领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\InventoryController.java" "src\main\java\com\example\domain\inventory\controller\InventoryController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\InventoryService.java" "src\main\java\com\example\domain\inventory\service\InventoryService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Inventory.java" "src\main\java\com\example\domain\inventory\entity\Inventory.java"
Move-FileIfExists "src\main\java\com\example\modules\controller\InventoryTransactionController.java" "src\main\java\com\example\domain\inventory\controller\InventoryTransactionController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\InventoryTransactionService.java" "src\main\java\com\example\domain\inventory\service\InventoryTransactionService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\InventoryTransaction.java" "src\main\java\com\example\domain\inventory\entity\InventoryTransaction.java"

# 移动商店相关文件
Write-Host "移动商店领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\ShopController.java" "src\main\java\com\example\domain\shop\controller\ShopController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\ShopService.java" "src\main\java\com\example\domain\shop\service\ShopService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Shop.java" "src\main\java\com\example\domain\shop\entity\Shop.java"

# 移动采购相关文件
Write-Host "移动采购领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\PurchaseController.java" "src\main\java\com\example\domain\purchase\controller\PurchaseController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\PurchaseService.java" "src\main\java\com\example\domain\purchase\service\PurchaseService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Purchase.java" "src\main\java\com\example\domain\purchase\entity\Purchase.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\PurchaseDetail.java" "src\main\java\com\example\domain\purchase\entity\PurchaseDetail.java"

# 移动统计相关文件
Write-Host "移动统计领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\StatisticsController.java" "src\main\java\com\example\domain\statistics\controller\StatisticsController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\StatisticsService.java" "src\main\java\com\example\domain\statistics\service\StatisticsService.java"
Move-FileIfExists "src\main\java\com\example\modules\service\SalesForecastService.java" "src\main\java\com\example\domain\statistics\service\SalesForecastService.java"
Move-FileIfExists "src\main\java\com\example\modules\service\ProphetService.java" "src\main\java\com\example\domain\statistics\service\ProphetService.java"

# 移动价格规则相关文件
Write-Host "移动价格规则领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\PriceRuleController.java" "src\main\java\com\example\domain\price\controller\PriceRuleController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\PriceRuleService.java" "src\main\java\com\example\domain\price\service\PriceRuleService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\PriceRule.java" "src\main\java\com\example\domain\price\entity\PriceRule.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\PriceRuleDetail.java" "src\main\java\com\example\domain\price\entity\PriceRuleDetail.java"

# 移动批次相关文件
Write-Host "移动批次领域的文件..." -ForegroundColor Green
Move-FileIfExists "src\main\java\com\example\modules\controller\BatchController.java" "src\main\java\com\example\domain\batch\controller\BatchController.java"
Move-FileIfExists "src\main\java\com\example\modules\service\BatchService.java" "src\main\java\com\example\domain\batch\service\BatchService.java"
Move-FileIfExists "src\main\java\com\example\modules\service\SaleBatchDetailService.java" "src\main\java\com\example\domain\batch\service\SaleBatchDetailService.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\Batch.java" "src\main\java\com\example\domain\batch\entity\Batch.java"
Move-FileIfExists "src\main\java\com\example\modules\entity\SaleBatchDetail.java" "src\main\java\com\example\domain\batch\entity\SaleBatchDetail.java"

# 移动配置相关文件
Write-Host "移动配置相关文件..." -ForegroundColor Green
if (Test-Path "src\main\java\com\example\Config") {
    Get-ChildItem -Path "src\main\java\com\example\Config" -File | ForEach-Object {
        Move-FileIfExists $_.FullName "src\main\java\com\example\infrastructure\config\$($_.Name)"
    }
}

# 移动异常处理相关文件
Write-Host "移动异常处理相关文件..." -ForegroundColor Green
if (Test-Path "src\main\java\com\example\exception") {
    Get-ChildItem -Path "src\main\java\com\example\exception" -File | ForEach-Object {
        Move-FileIfExists $_.FullName "src\main\java\com\example\infrastructure\exception\$($_.Name)"
    }
}

# 移动工具类相关文件
Write-Host "移动工具类相关文件..." -ForegroundColor Green
if (Test-Path "src\main\java\com\example\modules\utils") {
    Get-ChildItem -Path "src\main\java\com\example\modules\utils" -File | ForEach-Object {
        Move-FileIfExists $_.FullName "src\main\java\com\example\infrastructure\common\$($_.Name)"
    }
}

Write-Host "文件移动完成!" -ForegroundColor Green
Write-Host "提示: 您需要手动更新所有移动文件中的package声明和import语句" -ForegroundColor Yellow
Write-Host "按任意键继续..."
$host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") | Out-Null 