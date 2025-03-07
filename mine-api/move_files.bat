@echo off
echo 正在移动文件到新的目录结构...

REM 移动订单相关文件
echo 移动订单领域的文件...
move src\main\java\com\example\modules\controller\OrderController.java src\main\java\com\example\domain\order\controller\
move src\main\java\com\example\modules\service\OrderService.java src\main\java\com\example\domain\order\service\
move src\main\java\com\example\modules\entity\Order.java src\main\java\com\example\domain\order\entity\
move src\main\java\com\example\modules\entity\OrderDetail.java src\main\java\com\example\domain\order\entity\
move src\main\java\com\example\modules\entity\ReturnOrder.java src\main\java\com\example\domain\order\entity\
move src\main\java\com\example\modules\entity\ReturnOrderDetail.java src\main\java\com\example\domain\order\entity\

REM 移动产品相关文件
echo 移动产品领域的文件...
move src\main\java\com\example\modules\controller\ProductController.java src\main\java\com\example\domain\product\controller\
move src\main\java\com\example\modules\controller\CategoryController.java src\main\java\com\example\domain\product\controller\
move src\main\java\com\example\modules\service\ProductService.java src\main\java\com\example\domain\product\service\
move src\main\java\com\example\modules\service\CategoryService.java src\main\java\com\example\domain\product\service\
move src\main\java\com\example\modules\entity\Product.java src\main\java\com\example\domain\product\entity\
move src\main\java\com\example\modules\entity\Category.java src\main\java\com\example\domain\product\entity\

REM 移动库存相关文件
echo 移动库存领域的文件...
move src\main\java\com\example\modules\controller\InventoryController.java src\main\java\com\example\domain\inventory\controller\
move src\main\java\com\example\modules\service\InventoryService.java src\main\java\com\example\domain\inventory\service\
move src\main\java\com\example\modules\entity\Inventory.java src\main\java\com\example\domain\inventory\entity\
move src\main\java\com\example\modules\controller\InventoryTransactionController.java src\main\java\com\example\domain\inventory\controller\
move src\main\java\com\example\modules\service\InventoryTransactionService.java src\main\java\com\example\domain\inventory\service\
move src\main\java\com\example\modules\entity\InventoryTransaction.java src\main\java\com\example\domain\inventory\entity\

REM 移动商店相关文件
echo 移动商店领域的文件...
move src\main\java\com\example\modules\controller\ShopController.java src\main\java\com\example\domain\shop\controller\
move src\main\java\com\example\modules\service\ShopService.java src\main\java\com\example\domain\shop\service\
move src\main\java\com\example\modules\entity\Shop.java src\main\java\com\example\domain\shop\entity\

REM 移动采购相关文件
echo 移动采购领域的文件...
move src\main\java\com\example\modules\controller\PurchaseController.java src\main\java\com\example\domain\purchase\controller\
move src\main\java\com\example\modules\service\PurchaseService.java src\main\java\com\example\domain\purchase\service\
move src\main\java\com\example\modules\entity\Purchase.java src\main\java\com\example\domain\purchase\entity\
move src\main\java\com\example\modules\entity\PurchaseDetail.java src\main\java\com\example\domain\purchase\entity\

REM 移动统计相关文件
echo 移动统计领域的文件...
move src\main\java\com\example\modules\controller\StatisticsController.java src\main\java\com\example\domain\statistics\controller\
move src\main\java\com\example\modules\service\StatisticsService.java src\main\java\com\example\domain\statistics\service\
move src\main\java\com\example\modules\service\SalesForecastService.java src\main\java\com\example\domain\statistics\service\
move src\main\java\com\example\modules\service\ProphetService.java src\main\java\com\example\domain\statistics\service\

REM 移动价格规则相关文件
echo 移动价格规则领域的文件...
move src\main\java\com\example\modules\controller\PriceRuleController.java src\main\java\com\example\domain\price\controller\
move src\main\java\com\example\modules\service\PriceRuleService.java src\main\java\com\example\domain\price\service\
move src\main\java\com\example\modules\entity\PriceRule.java src\main\java\com\example\domain\price\entity\
move src\main\java\com\example\modules\entity\PriceRuleDetail.java src\main\java\com\example\domain\price\entity\

REM 移动批次相关文件
echo 移动批次领域的文件...
move src\main\java\com\example\modules\controller\BatchController.java src\main\java\com\example\domain\batch\controller\
move src\main\java\com\example\modules\service\BatchService.java src\main\java\com\example\domain\batch\service\
move src\main\java\com\example\modules\service\SaleBatchDetailService.java src\main\java\com\example\domain\batch\service\
move src\main\java\com\example\modules\entity\Batch.java src\main\java\com\example\domain\batch\entity\
move src\main\java\com\example\modules\entity\SaleBatchDetail.java src\main\java\com\example\domain\batch\entity\

REM 移动配置相关文件
echo 移动配置相关文件...
move src\main\java\com\example\Config\* src\main\java\com\example\infrastructure\config\

REM 移动异常处理相关文件
echo 移动异常处理相关文件...
move src\main\java\com\example\exception\* src\main\java\com\example\infrastructure\exception\

REM 移动工具类相关文件
echo 移动工具类相关文件...
move src\main\java\com\example\modules\utils\* src\main\java\com\example\infrastructure\common\

echo 文件移动完成!
echo 提示: 您需要手动更新所有移动文件中的package声明和import语句
pause 