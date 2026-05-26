# Mini-Mall 迷你商城后端

> 从零搭建的 Spring Boot 3 + MyBatis-Plus + Redis 电商后端学习项目。
> 包含用户、商品、购物车、订单、支付、收藏、热搜等核心模块，覆盖 **31 个接口**。

---

## 🛠 技术栈

| 类别       | 技术                                          |
| ---------- | --------------------------------------------- |
| 框架       | Spring Boot 3.3.5                             |
| ORM        | MyBatis-Plus 3.5.9                            |
| 数据库     | MySQL 8                                       |
| 缓存       | Redis（Cache Aside + ZSet 热搜 + Set 收藏）   |
| 鉴权       | JWT (jjwt 0.12.6) + BCrypt                    |
| 接口文档   | Knife4j 4.5.0 (OpenAPI 3, Jakarta)            |
| 定时任务   | Spring `@Scheduled`（每分钟扫描超时订单）     |
| 构建       | Maven                                         |
| JDK        | 17                                            |

---

## 📦 模块设计

```
mini-mall/
├── controller/      ← REST 接口层
├── service/         ← 业务逻辑层（含事务、缓存、状态机）
├── mapper/          ← 数据访问层（MyBatis-Plus）
├── entity/          ← 数据库实体
├── dto/             ← 入参对象
├── vo/              ← 出参对象（与 Entity 隔离）
├── common/          ← Result、BusinessException、常量
├── config/          ← Redis / MyBatis-Plus / 拦截器配置
├── interceptor/     ← JWT 拦截器
├── task/            ← 定时任务（订单超时关单）
└── utils/           ← JWT / 用户上下文工具
```

---

## ✨ 核心亮点

### 1. 防超卖
下单走 `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?`，依赖**数据库行锁**杜绝超卖。

### 2. 反向事务 + 状态机
取消订单走 `@Transactional`，状态置为 `CANCELLED` 后**还库存**；状态字段用常量 `OrderStatus.UNPAID/PAID/...` 杜绝魔法数字。

### 3. 越权防护四段式
所有"操作他人资源"的接口统一顺序：**判空 → 鉴权 → 业务规则 → 执行**。

### 4. Cache Aside 模式
商品详情 → 读先查 Redis 缓存，写删缓存。
LocalDateTime 通过 `JavaTimeModule` 修复序列化。

### 5. 热搜榜（Redis ZSet）
搜索时 `INCR` 关键词分数，`getHotSearch()` 取 Top N。

### 6. 收藏功能（Redis Set）
收藏列表存在 `favorite:user:{userId}`，去重天然由 Set 保证。

### 7. 订单超时关单
`@Scheduled(cron = "0 * * * * *")` 每分钟扫描超过 30 分钟未支付的订单，自动关闭并还库存。

### 8. 快照字段
`order_item` 表保存下单时的商品名、价格快照，商品后续改价不影响历史订单。

---

## 🚀 启动方式

### 1. 环境准备
- JDK 17+
- MySQL 8（默认端口 3306）
- Redis（默认端口 6379）

### 2. 建库
执行 `schema.sql` 创建数据库 `mini_mall` 和所有表。

### 3. 配置敏感信息
复制配置模板并改成自己的密码：
```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
```
编辑 `application.yml`，改 `spring.datasource.password` 和 `jwt.secret`。

### 4. 启动
```bash
mvn spring-boot:run
```
默认端口 **9000**。

### 5. 接口文档
浏览器打开：
```
http://localhost:9000/doc.html
```
（Knife4j UI，含 31 个接口的在线调试）

---

## 📚 学习收获

- Spring Boot 3 + Jakarta EE 9（包名 `javax` → `jakarta`）的迁移
- `@Transactional` 的传播、回滚边界、反向事务
- DTO / VO / Entity 三层分离的必要性
- `BeanUtils.copyProperties` 静默跳过类型不匹配字段的坑
- HTTP 方法语义：POST 创建有副作用 / PUT 修改幂等 / DELETE 删除 / GET 查询
- Cache Aside 的读写顺序与脏数据规避
- Redis 数据结构选型：String / ZSet / Set / Hash 的适用场景

---

## 📝 License

学习项目，仅供参考。
