package com.godze.fischfishing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = FischMod.MODID)
public class FishEvents {

    // Loot held back until the player wins the minigame
    private static final Map<UUID, List<ItemStack>> PENDING = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (Compat.hasConflict()) return;

        List<ItemStack> loot = new ArrayList<>();
        for (ItemStack s : event.getDrops()) loot.add(s.copy());
        if (loot.isEmpty()) return;

        // Cancel vanilla delivery; the loot is handed out after the minigame
        event.setCanceled(true);
        PENDING.put(player.getUUID(), loot);

        ItemStack rod = player.getMainHandItem().getItem() instanceof FishingRodItem
                ? player.getMainHandItem() : player.getOffhandItem();
        var luckHolder = player.level().registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LUCK_OF_THE_SEA);
        int luck = EnchantmentHelper.getItemEnchantmentLevel(luckHolder, rod);

        // Rarer loot = faster fish. Luck of the Sea = bigger catch bar.
        float speed = 0.8f + loot.get(0).getRarity().ordinal() * 0.5f
                + player.getRandom().nextFloat() * 0.4f;
        float bar = Math.min(0.40f, 0.20f + 0.05f * luck);

        Network.CHANNEL.send(new Network.StartPacket(speed, bar), PacketDistributor.PLAYER.with(player));
    }

    public static void finish(ServerPlayer player, boolean won) {
        List<ItemStack> loot = PENDING.remove(player.getUUID());
        if (loot == null) return; // no active catch, ignore

        if (!won) {
            player.displayClientMessage(Component.literal("The fish got away..."), true);
            return;
        }
        for (ItemStack stack : loot) {
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        player.level().addFreshEntity(new ExperienceOrb(player.level(),
                player.getX(), player.getY(), player.getZ(), 1 + player.getRandom().nextInt(6)));
        player.displayClientMessage(Component.literal("Caught it!"), true);
    }
}