package com.godze.fischfishing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Replaces the vanilla fishing rod tooltip with a tier name and a colored stat list. */
@Mod.EventBusSubscriber(modid = FischMod.MODID, value = Dist.CLIENT)
public class RodTooltip {

    // ---- Edit these ----
    private static final String TIER_NAME = "Basic Rod";
    private static final int TIER_NUMBER = 1;
    private static final String TIER_LABEL = "Basic";
    private static final int MAX_WEIGHT_KG = 10;
    private static final int LURE_PER_LEVEL = 15;  // % per Lure level
    private static final int LUCK_PER_LEVEL = 10;  // % per Luck of the Sea level

    private static final int CYAN = 0x55FFFF;
    private static final int GREEN = 0x55FF55;
    private static final int YELLOW = 0xFFFF55;
    private static final int RED = 0xFF5555;
    private static final int PINK_PURPLE = 0xE070FF;

    private static MutableComponent line(String text, int rgb) {
        return Component.literal(" " + text).withStyle(Style.EMPTY.withColor(rgb));
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof FishingRodItem)) return;

        List<Component> tip = event.getToolTip();
        if (tip.isEmpty()) return;

        // Enchantment-based stats (zero for a plain rod)
        int lure = 0, luck = 0;
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var enchants = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            lure = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.LURE), stack) * LURE_PER_LEVEL;
            luck = EnchantmentHelper.getItemEnchantmentLevel(enchants.getOrThrow(Enchantments.LUCK_OF_THE_SEA), stack) * LUCK_PER_LEVEL;
        }

        // Line 0: the name, shown as the tier in gray (a renamed rod keeps its own name)
        if (!stack.has(DataComponents.CUSTOM_NAME)) {
            tip.set(0, Component.literal(TIER_NAME).withStyle(ChatFormatting.GRAY));
        }

        List<Component> block = new ArrayList<>();
        block.add(Component.literal(" Tier " + TIER_NUMBER + " (" + TIER_LABEL + ")").withStyle(ChatFormatting.GRAY));
        block.add(line(lure + "% Lure Speed", CYAN));
        block.add(line(luck + "% Luck", GREEN));
        block.add(line("0 Control", YELLOW));
        block.add(line("0% Resilience", RED));
        block.add(Component.empty());
        block.add(line(MAX_WEIGHT_KG + "kg Max Weight", PINK_PURPLE));

        // Insert right under the name, before enchantment and durability lines
        tip.addAll(1, block);
    }
}