# 项目目录重构计划

## 目标

重构当前项目结构，从按技术层面划分转为按业务领域划分，使代码组织更符合领域驱动设计(DDD)原则，提高可维护性和扩展性。

## 当前项目结构问题

1. 所有功能模块平铺在`modules`下，没有按业务领域分组
2. 所有控制器、服务类等按技术层面分组，而不是按业务功能
3. 一些服务类文件过大（如`SalesForecastService`有1227行）
4. 命名不规范，有些目录使用大写开头（如`Config`）

## 新的目录结构

```
src/main/java/com/example/
├── domain/               # 领域层
│   ├── order/            # 订单领域
│   │   ├── controller/   # 控制器
│   │   ├── service/      # 服务
│   │   ├── repository/   # 仓库
│   │   ├── entity/       # 实体
│   │   ├── dto/          # 数据传输对象
│   │   └── mapper/       # 对象映射
│   ├── product/          # 产品领域
│   ├── inventory/        # 库存领域
│   ├── shop/             # 商店领域
│   ├── purchase/         # 采购领域
│   ├── statistics/       # 统计领域
│   ├── price/            # 价格规则领域
│   └── batch/            # 批次领域
├── infrastructure/       # 基础设施层
│   ├── config/           # 配置
│   ├── common/           # 公共组件
│   ├── exception/        # 异常处理
│   └── security/         # 安全相关
├── application/          # 应用服务层（跨领域的服务）
└── interfaces/           # 外部接口层（如API网关）
```

## 重构步骤

### 1. 创建新的目录结构

首先创建主要的目录结构：

```shell
# 创建领域层目录
mkdir -p src/main/java/com/example/domain
mkdir -p src/main/java/com/example/infrastructure/{config,common,exception,security}
mkdir -p src/main/java/com/example/application
mkdir -p src/main/java/com/example/interfaces

# 创建各个领域目录
mkdir -p src/main/java/com/example/domain/order
mkdir -p src/main/java/com/example/domain/product
mkdir -p src/main/java/com/example/domain/inventory
mkdir -p src/main/java/com/example/domain/shop
mkdir -p src/main/java/com/example/domain/purchase
mkdir -p src/main/java/com/example/domain/statistics
mkdir -p src/main/java/com/example/domain/price
mkdir -p src/main/java/com/example/domain/batch

# 为每个领域创建子目录
for domain in order product inventory shop purchase statistics price batch; do
    mkdir -p src/main/java/com/example/domain/$domain/{controller,service,repository,entity,dto,mapper}
done
```

### 2. 移动现有文件

按照业务领域将现有文件移动到新的目录结构中：

#### 订单领域（Order）

```shell
# 移动订单相关控制器
mv src/main/java/com/example/modules/controller/OrderController.java src/main/java/com/example/domain/order/controller/

# 移动订单相关服务
mv src/main/java/com/example/modules/service/OrderService.java src/main/java/com/example/domain/order/service/

# 移动订单相关实体
mv src/main/java/com/example/modules/entity/Order.java src/main/java/com/example/domain/order/entity/
mv src/main/java/com/example/modules/entity/OrderDetail.java src/main/java/com/example/domain/order/entity/
mv src/main/java/com/example/modules/entity/ReturnOrder.java src/main/java/com/example/domain/order/entity/
mv src/main/java/com/example/modules/entity/ReturnOrderDetail.java src/main/java/com/example/domain/order/entity/

# 移动订单相关仓库
# (从repository目录移动相关文件)
```

#### 产品领域（Product）

```shell
# 移动产品相关控制器
mv src/main/java/com/example/modules/controller/ProductController.java src/main/java/com/example/domain/product/controller/
mv src/main/java/com/example/modules/controller/CategoryController.java src/main/java/com/example/domain/product/controller/

# 移动产品相关服务
mv src/main/java/com/example/modules/service/ProductService.java src/main/java/com/example/domain/product/service/
mv src/main/java/com/example/modules/service/CategoryService.java src/main/java/com/example/domain/product/service/

# 移动产品相关实体
mv src/main/java/com/example/modules/entity/Product.java src/main/java/com/example/domain/product/entity/
mv src/main/java/com/example/modules/entity/Category.java src/main/java/com/example/domain/product/entity/
```

依此类推，对其他领域（库存、商店、采购、统计、价格规则、批次）进行相应处理。

### 3. 拆分大型服务类

大型服务类（如`SalesForecastService`）应拆分为更小的服务类：

例如，将`SalesForecastService`拆分为：
- `BasicForecastService` - 基本预测逻辑
- `TimeSeriesAnalysisService` - 时间序列分析
- `ForecastDataPreparationService` - 数据准备
- `ForecastResultService` - 结果处理

### 4. 处理跨领域服务

某些服务可能跨越多个领域，这些应放在`application`层：

```shell
# 创建跨领域服务目录
mkdir -p src/main/java/com/example/application/service
```

### 5. 处理公共组件

将通用工具类、配置、异常处理移至基础设施层：

```shell
# 移动配置类
mv src/main/java/com/example/Config/* src/main/java/com/example/infrastructure/config/

# 移动异常处理
mv src/main/java/com/example/exception/* src/main/java/com/example/infrastructure/exception/

# 移动工具类
mv src/main/java/com/example/modules/utils/* src/main/java/com/example/infrastructure/common/
```

## 代码调整

在移动文件后，需要更新包引用：

1. 更新每个移动文件的`package`声明
2. 更新导入语句中的包引用
3. 确保配置类中的组件扫描路径已更新

## 测试策略

1. 在重构过程中，定期运行测试以确保功能正常
2. 重构完成后，进行全面测试，确保所有功能正常运行

## 渐进式实施

可以按照以下顺序渐进实施重构：

1. 首先重构一个核心领域（如订单或产品）
2. 测试并确保该领域功能正常
3. 逐一重构其他领域
4. 最后重构基础设施层和跨领域组件

这种渐进式方法可以降低风险，确保系统在重构过程中始终保持可用状态。 