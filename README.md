# CostEffectiveEnchantedBook
薄利多销的附魔书！让新手图书管理员可以买的附魔书固定为最低正常价格和最好正常附魔等级。

本人非常喜欢这个Mod，但它暂时没有比较新的版本，于是升级将nwdxlgzs的CostEffectEnchantedBook升级到了最新版本，非常感谢原作者nwdxlgzs制作出这个优质的Mod!

## 支持版本
- Minecraft **1.21.8-26.3** (Fabric)

## 更新日志
见 [CHANGELOG.md](CHANGELOG.md)。

## 26.x移植说明
自 Minecraft 26.1 起，村民交易机制已改为数据驱动（`data/<namespace>/villager_trade`、`trade_set`、`tags/villager_trade`），
且游戏不再混淆（Yarn 映射停止维护，改用官方命名，Loom 新插件 `net.fabricmc.fabric-loom` 不再重映射）。

本 Mod 的 26.3 版本采用以下实现：
1. **代码**：注册自定义战利品函数 `costeffectiveenchantedbook:enchant_book_max_level`
   - 从 `#minecraft:tradeable` 随机选择附魔，等级固定为该附魔的最高等级
   - 价格固定为最低正常价格 `2 + 3 * 等级`（`#minecraft:double_trade_price` 附魔翻倍，上限 64）
   - 找不到附魔时回退为 1 绿宝石 + 1 本书购买 1 本普通书
2. **内嵌数据包**：
   - `data/costeffectiveenchantedbook/villager_trade/librarian/1/max_enchanted_book.json`：新交易定义
   - `data/minecraft/tags/villager_trade/librarian/level_1.json`：覆盖图书管理员 1 级交易标签
     （替换原版随机附魔书交易，保留纸张和书架交易）

### 26.3 适配要点
- 战利品函数 JSON 的类型字段由 `function` 改为 `type`
- 村民交易 JSON 的 `given_item_modifiers` 数组改为单个 `given_item_modifier`（值仍可用数组表示函数序列）
- `max_uses`、`xp`、`wants.count` 改用整数型数字提供器，JSON 中直接写整数
- `RegistryCodecs` 迁移到 `net.minecraft.core.registries.codec`，`homogeneousList` 更名为 `holderSet`
- `LootItemConditionalFunction` 的条件参数改为 `Optional<Holder<LootItemCondition>>`
- 工具链：Java 25、Gradle 9.6.0、Loom 1.17.21、Fabric Loader 0.19.5、Fabric API 0.167.0+26.3