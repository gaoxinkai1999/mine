@echo off
echo 正在创建项目DDD目录结构...

REM 创建主要目录结构
mkdir src\main\java\com\example\domain
mkdir src\main\java\com\example\infrastructure
mkdir src\main\java\com\example\application
mkdir src\main\java\com\example\interfaces

REM 创建领域层各业务领域目录
mkdir src\main\java\com\example\domain\order
mkdir src\main\java\com\example\domain\product
mkdir src\main\java\com\example\domain\inventory
mkdir src\main\java\com\example\domain\shop
mkdir src\main\java\com\example\domain\purchase
mkdir src\main\java\com\example\domain\statistics
mkdir src\main\java\com\example\domain\price
mkdir src\main\java\com\example\domain\batch

REM 为每个业务领域创建子目录
REM 订单领域
mkdir src\main\java\com\example\domain\order\controller
mkdir src\main\java\com\example\domain\order\service
mkdir src\main\java\com\example\domain\order\repository
mkdir src\main\java\com\example\domain\order\entity
mkdir src\main\java\com\example\domain\order\dto
mkdir src\main\java\com\example\domain\order\mapper

REM 产品领域
mkdir src\main\java\com\example\domain\product\controller
mkdir src\main\java\com\example\domain\product\service
mkdir src\main\java\com\example\domain\product\repository
mkdir src\main\java\com\example\domain\product\entity
mkdir src\main\java\com\example\domain\product\dto
mkdir src\main\java\com\example\domain\product\mapper

REM 库存领域
mkdir src\main\java\com\example\domain\inventory\controller
mkdir src\main\java\com\example\domain\inventory\service
mkdir src\main\java\com\example\domain\inventory\repository
mkdir src\main\java\com\example\domain\inventory\entity
mkdir src\main\java\com\example\domain\inventory\dto
mkdir src\main\java\com\example\domain\inventory\mapper

REM 商店领域
mkdir src\main\java\com\example\domain\shop\controller
mkdir src\main\java\com\example\domain\shop\service
mkdir src\main\java\com\example\domain\shop\repository
mkdir src\main\java\com\example\domain\shop\entity
mkdir src\main\java\com\example\domain\shop\dto
mkdir src\main\java\com\example\domain\shop\mapper

REM 采购领域
mkdir src\main\java\com\example\domain\purchase\controller
mkdir src\main\java\com\example\domain\purchase\service
mkdir src\main\java\com\example\domain\purchase\repository
mkdir src\main\java\com\example\domain\purchase\entity
mkdir src\main\java\com\example\domain\purchase\dto
mkdir src\main\java\com\example\domain\purchase\mapper

REM 统计领域
mkdir src\main\java\com\example\domain\statistics\controller
mkdir src\main\java\com\example\domain\statistics\service
mkdir src\main\java\com\example\domain\statistics\service\forecast
mkdir src\main\java\com\example\domain\statistics\repository
mkdir src\main\java\com\example\domain\statistics\entity
mkdir src\main\java\com\example\domain\statistics\dto
mkdir src\main\java\com\example\domain\statistics\mapper

REM 价格规则领域
mkdir src\main\java\com\example\domain\price\controller
mkdir src\main\java\com\example\domain\price\service
mkdir src\main\java\com\example\domain\price\repository
mkdir src\main\java\com\example\domain\price\entity
mkdir src\main\java\com\example\domain\price\dto
mkdir src\main\java\com\example\domain\price\mapper

REM 批次领域
mkdir src\main\java\com\example\domain\batch\controller
mkdir src\main\java\com\example\domain\batch\service
mkdir src\main\java\com\example\domain\batch\repository
mkdir src\main\java\com\example\domain\batch\entity
mkdir src\main\java\com\example\domain\batch\dto
mkdir src\main\java\com\example\domain\batch\mapper

REM 创建基础设施层子目录
mkdir src\main\java\com\example\infrastructure\config
mkdir src\main\java\com\example\infrastructure\common
mkdir src\main\java\com\example\infrastructure\exception
mkdir src\main\java\com\example\infrastructure\security

REM 创建应用服务层目录
mkdir src\main\java\com\example\application\service

echo 目录结构创建完成!
pause 