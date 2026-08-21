package com.nwdxlgzs.costeffectiveenchantedbook;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * 薄利多销的附魔书！
 * <p>
 * 26.1 起村民交易改为数据驱动（data/{namespace}/villager_trade + trade_set + tags/villager_trade），
 * 本 Mod 不再修改 TradeOffers 代码表，而是：
 * 1. 在代码中注册自定义战利品函数 costeffectiveenchantedbook:enchant_book_max_level，
 *    该函数从 #minecraft:tradeable 随机选择附魔并固定为最高等级，
 *    同时把价格固定为最低正常价格：2 + 3 * 等级（double_trade_price 翻倍，上限 64）。
 * 2. 通过内嵌数据包把图书管理员的 1 级附魔书交易替换为该自定义交易。
 */
public class CostEffectiveEnchantedBook implements ModInitializer {
	public static final String MOD_ID = "costeffectiveenchantedbook";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 26.2 中战利品函数类型直接注册 MapCodec（不再有 LootItemFunctionType 包装类）
		Registry.register(
			BuiltInRegistries.LOOT_FUNCTION_TYPE,
			Identifier.fromNamespaceAndPath(MOD_ID, "enchant_book_max_level"),
			EnchantBookMaxLevelFunction.MAP_CODEC
		);
		LOGGER.info("Load:薄利多销的附魔书 (Minecraft 26.2)");
	}

	/**
	 * 类似原版 enchant_randomly，但附魔等级固定为该附魔的最高等级，
	 * 并把 ADDITIONAL_TRADE_COST 固定为最低价格：2 + 3 * 等级，
	 * 若附魔位于 #minecraft:double_trade_price 则翻倍，最终上限 64。
	 */
	public static class EnchantBookMaxLevelFunction extends LootItemConditionalFunction {
		public static final MapCodec<EnchantBookMaxLevelFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
			commonFields(instance).and(instance.group(
				RegistryCodecs.homogeneousList(Registries.ENCHANTMENT)
					.optionalFieldOf("options")
					.forGetter(function -> function.options),
				Codec.INT.optionalFieldOf("max_level", Integer.MAX_VALUE)
					.forGetter(function -> function.maxLevel),
				Codec.BOOL.optionalFieldOf("include_additional_cost_component", true)
					.forGetter(function -> function.includeAdditionalCostComponent)
			)).apply(instance, EnchantBookMaxLevelFunction::new)
		);

		private final Optional<HolderSet<Enchantment>> options;
		private final int maxLevel;
		private final boolean includeAdditionalCostComponent;

		private EnchantBookMaxLevelFunction(
			List<LootItemCondition> conditions,
			Optional<HolderSet<Enchantment>> options,
			int maxLevel,
			boolean includeAdditionalCostComponent
		) {
			super(conditions);
			this.options = options;
			this.maxLevel = maxLevel;
			this.includeAdditionalCostComponent = includeAdditionalCostComponent;
		}

		@Override
		protected ItemStack run(ItemStack stack, LootContext context) {
			// 选择候选附魔：优先使用 options（我们的交易传 #minecraft:tradeable），否则全部附魔
			HolderSet<Enchantment> candidates = this.options.orElseGet(() ->
				context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(EnchantmentTags.TRADEABLE)
			);

			Optional<Holder<Enchantment>> optional = candidates.getRandomElement(context.getRandom());
			if (optional.isEmpty()) {
				// 与原版逻辑一致：找不到可用附魔时，卖一本普通书，价格固定为 1 绿宝石
				ItemStack fallback = new ItemStack(Items.BOOK);
				if (this.includeAdditionalCostComponent
					&& context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)) {
					fallback.set(DataComponents.ADDITIONAL_TRADE_COST, 1);
				}
				return fallback;
			}

			Holder<Enchantment> enchantment = optional.get();
			// 删除随机等级，固定为最高正常等级
			int level = Math.min(enchantment.value().getMaxLevel(), this.maxLevel);

			ItemStack result = stack.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : stack;
			result.enchant(enchantment, level);

			// 删除随机加价：价格固定为 2 + 3 * 等级（double_trade_price 翻倍，上限 64）
			if (this.includeAdditionalCostComponent
				&& context.hasParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED)) {
				int cost = 2 + 3 * level;
				if (enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
					cost *= 2;
				}
				result.set(DataComponents.ADDITIONAL_TRADE_COST, Math.min(cost, 64));
			}
			return result;
		}

		@Override
		public MapCodec<? extends LootItemConditionalFunction> codec() {
			return MAP_CODEC;
		}
	}
}
