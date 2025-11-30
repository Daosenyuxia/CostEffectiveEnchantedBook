package com.nwdxlgzs.costeffectiveenchantedbook;

import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;

import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class CostEffectiveEnchantedBook implements ModInitializer {
    public static final String MOD_ID = "costeffectiveenchantedbook";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Load:薄利多销的附魔书");
        Int2ObjectMap<TradeOffers.Factory[]> librarian = TradeOffers.PROFESSION_TO_LEVELED_TRADE.get(VillagerProfession.LIBRARIAN);
        //只更改新手交易列表，其他等级沿用原版逻辑（万一谁需要低等级附魔书呢，不会吧？）
        TradeOffers.Factory[] librarian_level1 = librarian.get(1);
        int origIdx = -1;
        boolean findSelf = false;
        for (int i = 0; i < librarian_level1.length; ++i) {
            if (librarian_level1[i] instanceof TradeOffers.EnchantBookFactory) {
                origIdx = i;
            } else if (librarian_level1[i] instanceof CostEffectiveEnchantBookFactory) {
                findSelf = true;
            }
        }
        if (!findSelf) {
            CostEffectiveEnchantBookFactory factory = new CostEffectiveEnchantBookFactory(1, EnchantmentTags.TRADEABLE);
            if (origIdx != -1) {
                librarian_level1[origIdx] = factory;
            } else {
                ArrayList<TradeOffers.Factory> list = new ArrayList<>(Arrays.asList(librarian_level1));
                list.add(factory);
                librarian_level1 = list.toArray(new TradeOffers.Factory[0]);
                librarian.put(1, librarian_level1);
            }
        }
    }

    public static class CostEffectiveEnchantBookFactory implements TradeOffers.Factory {
        private final int experience;
        private final TagKey<Enchantment> possibleEnchantments;
        private final int maxLevel;

        public CostEffectiveEnchantBookFactory(int experience, TagKey<Enchantment> possibleEnchantments) {
            this(experience, Integer.MAX_VALUE, possibleEnchantments);
        }

        public CostEffectiveEnchantBookFactory(int experience, int maxLevel, TagKey<Enchantment> possibleEnchantments) {
            this.maxLevel = maxLevel;
            this.experience = experience;
            this.possibleEnchantments = possibleEnchantments;
        }

        public TradeOffer create(ServerWorld world, Entity entity, Random random) {
            Optional<RegistryEntry<Enchantment>> optional = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getRandomEntry(this.possibleEnchantments, random);
            int l;
            ItemStack itemStack;
            if (optional.isPresent()) {
                RegistryEntry<Enchantment> registryEntry = optional.get();
                Enchantment enchantment = registryEntry.value();
                //删除随机等级，默认最高等级
                int k = Math.min(enchantment.getMaxLevel(), this.maxLevel);
                itemStack = EnchantmentHelper.getEnchantedBookWith(new EnchantmentLevelEntry(registryEntry, k));
                //删除随机加价
                l = 2 + 3 * k;
                if (registryEntry.isIn(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                    l *= 2;
                }
                if (l > 64) {
                    l = 64;
                }
            } else {
                l = 1;
                itemStack = new ItemStack(Items.BOOK);
            }
            return new TradeOffer(new TradedItem(Items.EMERALD, l), Optional.of(new TradedItem(Items.BOOK)), itemStack, 12, this.experience, 0.2F);
        }
    }
}
