# 订单领域重构示例

本文档提供了一个具体的示例，展示如何重构订单领域的代码。它可以作为其他领域重构的参考模板。

## 1. 创建订单领域目录结构

```shell
# 创建订单领域的目录结构
mkdir -p src/main/java/com/example/domain/order/controller
mkdir -p src/main/java/com/example/domain/order/service
mkdir -p src/main/java/com/example/domain/order/repository
mkdir -p src/main/java/com/example/domain/order/entity
mkdir -p src/main/java/com/example/domain/order/dto
mkdir -p src/main/java/com/example/domain/order/mapper
```

## 2. 移动订单控制器

首先，将现有的`OrderController.java`文件从原位置移动到新的订单领域控制器目录：

```shell
mv src/main/java/com/example/modules/controller/OrderController.java src/main/java/com/example/domain/order/controller/
```

然后，修改`OrderController.java`文件中的包声明和导入语句：

**原始包声明：**
```java
package com.example.modules.controller;
```

**新的包声明：**
```java
package com.example.domain.order.controller;
```

**更新导入语句：**

```java
// 原始导入

// 更新为
import com.example.domain.order.service.OrderService;
```

## 3. 移动订单服务

将订单服务类移动到新位置：

```shell
mv src/main/java/com/example/modules/service/OrderService.java src/main/java/com/example/domain/order/service/
```

修改`OrderService.java`文件中的包声明和导入语句：

**原始包声明：**
```java
package com.example.modules.service;
```

**新的包声明：**
```java
package com.example.domain.order.service;
```

**更新导入语句，例如：**

```java
// 原始导入

// 更新为
import com.example.domain.order.repository.OrderRepository;
```

## 4. 移动订单实体类

移动所有订单相关的实体类：

```shell
mv src/main/java/com/example/modules/entity/Order.java src/main/java/com/example/domain/order/entity/
mv src/main/java/com/example/modules/entity/OrderDetail.java src/main/java/com/example/domain/order/entity/
mv src/main/java/com/example/modules/entity/ReturnOrder.java src/main/java/com/example/domain/order/entity/
mv src/main/java/com/example/modules/entity/ReturnOrderDetail.java src/main/java/com/example/domain/order/entity/
```

对每个实体类，修改包声明：

**原始包声明：**
```java
package com.example.modules.entity;
```

**新的包声明：**
```java
package com.example.domain.order.entity;
```

并更新相关的导入语句。

## 5. 移动订单仓库

移动订单相关的仓库接口和实现：

```shell
mv src/main/java/com/example/modules/repository/OrderRepository.java src/main/java/com/example/domain/order/repository/
```

修改包声明和导入语句：

**原始包声明：**
```java
package com.example.modules.repository;
```

**新的包声明：**
```java
package com.example.domain.order.repository;
```

## 6. 创建订单DTO和Mapper

对于DTO（数据传输对象）和Mapper（对象映射器），如果已有，则移动到新位置；如果没有，可以创建新的：

**订单DTO示例：**
```java
package com.example.domain.order.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private LocalDateTime orderDate;
    private BigDecimal totalAmount;
    private String status;
    // 其他必要字段
    
    private List<OrderDetailDTO> orderDetails;
}

@Data
public class OrderDetailDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    // 其他必要字段
}
```

**订单Mapper示例：**
```java
package com.example.domain.order.mapper;

import com.example.domain.order.entity.Order;
import com.example.domain.order.entity.OrderDetail;
import com.example.domain.order.dto.OrderDTO;
import com.example.domain.order.dto.OrderDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "orderNumber", target = "orderNumber")
    @Mapping(source = "orderDate", target = "orderDate")
    @Mapping(source = "totalAmount", target = "totalAmount")
    @Mapping(source = "status", target = "status")
    OrderDTO orderToOrderDTO(Order order);
    
    @Mapping(source = "id", target = "id")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(source = "price", target = "price")
    OrderDetailDTO orderDetailToOrderDetailDTO(OrderDetail orderDetail);
}
```

## 7. 更新服务实现

如果订单服务需要拆分或重构，可以创建更多专注于特定功能的服务类：

```java
package com.example.domain.order.service;

import com.example.domain.order.entity.Order;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {
    // 处理订单流程的方法
    public void processOrder(Order order) {
        // 订单处理逻辑
    }
}

@Service
public class OrderStatusService {
    // 处理订单状态变更的方法
    public void changeOrderStatus(Long orderId, String newStatus) {
        // 状态变更逻辑
    }
}
```

## 8. 测试重构结果

重构完成后，运行测试以确保功能正常：

```shell
# 使用Maven运行测试
mvn test -Dtest=com.example.domain.order.**
```

## 9. 更新Spring组件扫描路径

确保在Spring Boot的主应用类或配置类中更新组件扫描路径：

```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.domain",
    "com.example.infrastructure",
    "com.example.application"
})
public class JunLeBaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(JunLeBaoApplication.class, args);
    }
}
```

## 10. 处理跨领域依赖

如果订单领域依赖其他领域（如产品、库存等），在完成其他领域的重构后，更新相关导入：

```java
// 原始导入（假设产品已经重构）

// 更新为

```

## 注意事项

1. 在移动文件前，先确保对项目有完整备份
2. 每移动一组相关文件后，就更新其中的包引用，并进行编译检查
3. 从简单的领域开始重构，逐步过渡到更复杂的领域
4. 使用IDE的重构功能可以自动更新引用路径

按照这个示例，您可以依次重构其他领域，如产品、库存、采购等，使项目结构更加清晰和易于维护。 