package dev.ipseucz.koraquest.listener;

import dev.ipseucz.koraquest.QuestManager;
import dev.ipseucz.koraquest.model.ObjectiveType;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;

public final class QuestListener implements Listener {
    private final QuestManager manager;

    public QuestListener(QuestManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            manager.increment(killer, ObjectiveType.KILL, event.getEntityType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        manager.increment(event.getPlayer(), ObjectiveType.BREAK, event.getBlock().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        manager.increment(event.getPlayer(), ObjectiveType.PLACE, event.getBlockPlaced().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result.getType() == Material.AIR) {
            return;
        }
        manager.increment(player, ObjectiveType.CRAFT, result.getType().name(), Math.max(1, result.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
                && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        Entity caught = event.getCaught();
        String target = "ANY";
        if (caught instanceof Item item) {
            target = item.getItemStack().getType().name();
        } else if (caught != null) {
            target = caught.getType().name();
        }
        manager.increment(event.getPlayer(), ObjectiveType.FISH, target, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        manager.increment(event.getEnchanter(), ObjectiveType.ENCHANT, event.getItem().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getBreeder() instanceof Player player) {
            manager.increment(player, ObjectiveType.BREED, event.getEntityType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTame(EntityTameEvent event) {
        if (event.getOwner() instanceof Player player) {
            manager.increment(player, ObjectiveType.TAME, event.getEntityType().name(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        manager.increment(event.getPlayer(), ObjectiveType.CONSUME, event.getItem().getType().name(), 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSmelt(FurnaceExtractEvent event) {
        manager.increment(event.getPlayer(), ObjectiveType.SMELT, event.getItemType().name(), Math.max(1, event.getItemAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        manager.increment(event.getPlayer(), ObjectiveType.SHEAR, event.getEntity().getType().name(), 1);
    }
}
