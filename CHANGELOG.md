# 更新日志 / Changelog

本文件记录 CostEffectiveEnchantedBook 的版本变更。
格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号遵循语义化版本。

## [1.2.0] - 2026-11-26

### 升级到 Minecraft 26.3（Fabric）

- 支持版本：**Minecraft 26.3**（`fabric.mod.json` 中 `minecraft: ~26.3`），需要 **Fabric Loader ≥ 0.19.5**、**Java 25**。
- 依赖：Fabric API **0.167.0+26.3**。
- 构建工具链：Gradle **9.6.0**、Fabric Loom **1.17.21**（游戏自 26.1 起不再混淆，仍不需要 mappings）。

### 数据包格式迁移（26.3 破坏性变更）

26.3 重做了战利品函数与村民交易的 JSON 结构，旧格式不会报错但会失效，本版本已全部迁移：

- 村民交易：`given_item_modifiers`（数组）→ **`given_item_modifier`**（单个 modifier，值仍可写成函数数组）。
- 战利品函数：类型字段 `"function"` → **`"type"`**。
- 数字提供器拆分为整型/浮点：`max_uses`、`xp`、`wants.count` 改为整数型，JSON 中直接写整数（`12.0` → `12`）。
- `reputation_discount`、`additional_wants` 等其余字段含义不变。

### 代码适配（26.3 API 变更）

- `RegistryCodecs` 从 `net.minecraft.core` 移到 `net.minecraft.core.registries.codec`，`homogeneousList` 更名为 **`holderSet`**。
- `LootItemConditionalFunction` 的条件参数由 `List<LootItemCondition>` 改为 **`Optional<Holder<LootItemCondition>>`**。
- 新增 `getReferencedContextParams()`，声明使用 `ADDITIONAL_COST_COMPONENT_ALLOWED`，与原版 `enchant_randomly` 保持一致。
- 战利品函数注册方式（`BuiltInRegistries.LOOT_FUNCTION_TYPE` 直接注册 `MapCodec`）在 26.3 保持不变。

### 行为（与 26.2 版一致）

- 图书管理员 1 级交易中的附魔书固定为 **最高等级**，价格固定为最低正常价格 **`2 + 3 × 等级`**；
  `#minecraft:double_trade_price` 中的附魔翻倍，最终上限 64（26.3 的价格本身也会被钳制到物品最大堆叠数）。
- 找不到可用附魔时回退为 1 绿宝石 + 1 本书购买 1 本普通书。
- 原版纸张、书架交易保留。

### 验证

- `gradlew clean build` 通过，产物 `costeffectiveenchantedbook-1.2.0.jar`。
- 在 26.3 开发服务端实测：连续生成 100 个 1 级图书管理员的交易表（共 200 条交易，其中 73 条附魔书交易，5 条命中翻倍附魔），
  附魔等级与价格断言 **0 处不一致**。

### English (26.3 port)

- Updated to Minecraft **26.3** (Fabric Loader ≥ 0.19.5, Java 25, Fabric API 0.167.0+26.3).
- Toolchain: Gradle 9.6.0, Fabric Loom 1.17.21.
- Migrated data pack format: `given_item_modifier`, loot function `type` instead of `function`, integer number providers for `max_uses` / `xp` / `wants.count`.
- API changes: `RegistryCodecs.holderSet`, `Optional<Holder<LootItemCondition>>` conditions, `getReferencedContextParams()`.
- Behaviour unchanged: librarian level-1 enchanted books are max level and cost the minimum normal price `2 + 3 × level` (doubled for `#minecraft:double_trade_price`, capped at 64).
- Verified on a dev server: 100 generated trade sets, 0 level/price mismatches.

## [1.1.0]

### 26.2 移植版

- 支持 Minecraft **26.2**（Fabric）。
- 自 26.1 起村民交易改为数据驱动，本版改为：
  - 注册自定义战利品函数 `costeffectiveenchantedbook:enchant_book_max_level`（随机附魔、固定最高等级、固定最低价格）；
  - 用内嵌数据包覆盖 `#minecraft:librarian/level_1` 交易标签，替换原版随机附魔书交易。
- 不再需要 Mixin / 修改 `TradeOffers` 代码表。
