# 本地生活点评服务平台(高并发实践)

基于 LBS 的本地生活信息聚合平台,涵盖商户搜索、达人探店、高并发优惠券秒杀等功能。项目重点解决高并发场景下的**数据一致性、超卖及接口性能优化**问题。

## 技术栈

| 组件 | 用途 |
| --- | --- |
| Spring Boot 3.2 | Web 框架 |
| Vue 3 + Vite | 前端页面(附近商户/秒杀/签到/统计) |
| Redis 7 + Lettuce | 缓存、库存、GEO、BitMap、HyperLogLog、Stream 队列 |
| Redisson | 分布式锁(WatchDog 自动续期) |
| MyBatis-Plus | ORM |
| MySQL 8 | 持久化 |
| Docker Compose | 一键启动基础设施 |

## 架构总览

```
                    ┌──────────────┐
   用户请求 ───────> │  Controller  │
                    └──────┬───────┘
          ┌────────────────┼─────────────────────┐
          │                │                     │
 ┌────────▼─────────┐ ┌────▼──────────────┐ ┌────▼──────────────┐
 │ 秒杀下单(核心链路) │ │ 商户查询(GEO+缓存) │ │ 签到/UV(统计链路)  │
 │ Lua原子预扣库存    │ │ Cache Aside       │ │ BitMap / HLL      │
 │ Redis Stream削峰  │ │ 互斥锁防击穿       │ │                   │
 │ 消费者组异步落库   │ │ 空值缓存防穿透     │ │                   │
 └────────┬─────────┘ └────┬──────────────┘ └────┬──────────────┘
          │                │                     │
 ┌────────▼────────────────▼─────────────────────▼──────────────┐
 │              Redis(缓存/库存/GEO/BitMap/HLL/Stream)            │
 └───────────────────────────────────┬──────────────────────────┘
                           ┌─────────▼─────────┐
                           │    MySQL(兜底)     │
                           └───────────────────┘
```

## 快速开始

### 1. 启动基础设施(Docker)

```bash
docker compose up -d
```

会自动创建 `dianping` 库并导入演示数据(8 家北京商户、3 个测试用户、2 张秒杀券)。

> 没有 Docker 时也可用本机 MySQL 8 手动执行 `sql/init.sql`(建库+建表+种子数据),并自行启动 Redis(需 ≥5.0,支持 Stream/GEO)。

### 2. 启动后端

```bash
# Windows
.\mvnw.cmd spring-boot:run

# 或直接运行 jar
.\mvnw.cmd -DskipTests package
java -jar target/dianping-review-1.0.0.jar
```

启动时 `SeckillStockInitializer` 会把数据库中的有效秒杀券库存同步到 Redis。

### 3. 启动前端(可选)

```bash
cd frontend
npm install        # 已配置 npmmirror 镜像
npm run dev        # http://localhost:5173,/api 自动代理到 8081
```

浏览器打开 `http://localhost:5173`,包含四个页面:

- **附近商户**: GEO 附近搜索 + 商户详情(连续查询两次可观察缓存命中带来的响应加速)
- **限时秒杀**: 票券卡片 + 实时库存脉搏条 + 倒计时 + 抢购 + 我的订单
- **每日签到**: BitMap 签到日历 + 连续天数
- **数据统计**: HyperLogLog UV 查询与模拟访客

前端登录:任意 11 位手机号 + 密码(不存在自动注册),或直接用演示账号 13800138000 / 123456。

### 4. 接口测试

测试用户: 13800138000~13800138002,密码 `123456`。

```bash
# 登录(返回用户信息,取 id 作为后续请求的 X-User-Id 头)
curl -X POST http://localhost:8081/api/user/login -H "Content-Type: application/json" -d "{\"phone\":\"13800138000\",\"password\":\"123456\"}"

# 秒杀下单(演示用户也可直接用 X-User-Id: 1/2/3 跳过登录)
curl -X POST http://localhost:8081/api/voucher-order/seckill/2 -H "X-User-Id: 1"

# 商户详情(走缓存)
curl http://localhost:8081/api/shop/1

# 附近商户(天安门坐标,5km内)
curl "http://localhost:8081/api/shop/nearby?x=116.404&y=39.915&distance=5"

# 签到 / 连续签到天数 / 本月签到日历
curl -X POST http://localhost:8081/api/sign -H "X-User-Id: 1"
curl http://localhost:8081/api/sign/count -H "X-User-Id: 1"
curl http://localhost:8081/api/sign/month -H "X-User-Id: 1"

# UV 统计
curl http://localhost:8081/api/stats/uv/1
```

## API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | /api/user/login | 登录(不存在自动注册) |
| GET | /api/shop/{id} | 商户详情(旁路缓存+穿透防护+UV记录) |
| GET | /api/shop/mutex/{id} | 商户详情(互斥锁防击穿) |
| GET | /api/shop/nearby?x=&y=&distance= | 附近商户搜索(GEO) |
| POST | /api/shop | 新增商户(写入GEO) |
| PUT | /api/shop | 更新商户(主动删缓存) |
| POST | /api/voucher/seckill | 添加秒杀券(同步库存到Redis) |
| GET | /api/voucher/list/{shopId} | 商户优惠券列表 |
| GET | /api/voucher/seckill/stock/{id} | 查询Redis剩余库存 |
| POST | /api/voucher-order/seckill/{voucherId} | 秒杀下单(需 X-User-Id 头) |
| GET | /api/voucher-order/user | 我的订单 |
| POST | /api/sign | 签到(BitMap) |
| GET | /api/sign/count | 连续签到天数 |
| GET | /api/sign/month | 本月签到日历 |
| POST | /api/stats/uv/record/{shopId} | 记录UV(HyperLogLog) |
| GET | /api/stats/uv/{shopId} | 查询UV |

## 核心设计(对应五大技术点)

### 1. 高并发秒杀优化 —— Redis + Lua 预扣库存 + Stream 异步队列

`VoucherOrderServiceImpl#seckillVoucher` + `resources/seckill.lua`

- **Lua 脚本原子执行**: 活动时间校验 → 查库存 → 查重(Set) → 扣库存 → 记录已购,一次网络往返完成全部校验,DB 压力全部转移至 Redis;
- **异步削峰**: 请求侧只做缓存扣减 + 生成雪花订单号 + 投递 Redis Stream,立即返回,吞吐量不受 DB 写瓶颈限制;
- **消费者组异步落库**: `SeckillOrderConsumer` 通过 `XREADGROUP` 拉取消息,落库 + MySQL 扣库存(`WHERE stock > 0` 数据库层兜底防超卖);
- **可靠消费**: 失败消息携带 retry 计数重新入队,超限回滚 Redis 库存;定时任务读取 PEL 恢复宕机未确认消息;`(user_id, voucher_id)` 唯一索引 + 主键去重保证幂等。

### 2. 分布式锁 —— Redisson 一人一单 + WatchDog

`VoucherOrderServiceImpl#fallbackSyncOrder`

- Redis 链路异常时降级为同步下单路径,`lock:order:{userId}` 维度加锁,集群模式下同样保证一人一单;
- `tryLock(3, SECONDS)` 不显式传 leaseTime,业务阻塞时 **WatchDog 看门狗每 10s 自动续期**(默认锁超时 30s 的 1/3),避免锁提前释放;
- 锁内双重校验订单 + `isHeldByCurrentThread` 判断后解锁。

### 3. 多级缓存 —— Cache Aside + 互斥锁防击穿

`utils/CacheClient`

- **旁路缓存**: 读 → 缓存 miss 回源 DB → 写缓存;**更新走"先更新 DB 再删缓存"**,解决双写一致性;
- **防穿透**: DB 查不到时缓存空串(短TTL),拦截恶意不存在的 id;
- **防击穿**: `queryWithMutex` 用 SETNX 互斥锁 + 双重检查 + 重试,热点 key 失效时只有一个线程回源;
- **防雪崩**: 缓存 TTL 叠加随机值;
- 解锁使用 Lua 校验令牌,避免误删他人锁。

### 4. 海量数据处理 —— BitMap 签到 + HyperLogLog UV

`SignServiceImpl` / `StatsServiceImpl`

- 签到: `SETBIT sign:{userId}:{yyyyMM} {day-1} 1`,一年 365 位 ≈ 46B/用户,比传统表存储节省 90%+ 内存;
- 连续天数: `BITFIELD GET u{day} 0` 取位掩码后逐位右移统计连续 1;
- UV: `PFADD` / `PFCOUNT`,单日千万级 UV 仅占 12KB,误差 0.81%。

### 5. 附近商户搜索 —— Redis GEO

`ShopServiceImpl#queryNearby` / `saveShop`

- 商户经纬度 `GEOADD geo:shops x y shopId`;
- `GEORADIUS` 按半径检索 + 距离升序,相比"全量查库+内存算距离"大幅降低 IO 与计算量;
- 命中商户批量回查 DB 并回填缓存。

## 压测指南

Windows 下执行(20 用户并发 × 每人 5 次重复抢购):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\seckill-load-test.ps1 -Users 20 -Rounds 5 -VoucherId 2
```

预期结果: 每个用户最多成功 1 次(一人一单),总成功数不超过券库存(200);后续可用 JMeter 加线程组做 QPS 对比(直连 DB 下单 vs Redis+Lua+异步队列)。

## 数据一致性说明

秒杀链路为**最终一致性**模型: Redis 预扣为"快速权威",MySQL 库存由消费者异步追平;任何一步失败均通过重试或回滚(INCR 归还库存 + SREM 解除占用)兜底,配合唯一索引保证不超卖、不重复。
