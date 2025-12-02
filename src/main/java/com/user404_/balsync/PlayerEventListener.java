package com.user404_.balsync;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {
    private final BalSyncPlugin plugin;
    private final BalanceManager balanceManager;
    private final Economy economy;

    public PlayerEventListener(BalSyncPlugin plugin, BalanceManager balanceManager) {
        this.plugin = plugin;
        this.balanceManager = balanceManager;
        this.economy = plugin.getEconomy(); // Economy vom Plugin holen
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 2 Sekunden (40 Ticks) Verzögerung
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                balanceManager.loadPlayerBalance(event.getPlayer());
            }
        }, 40L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getConfigManager().saveOnQuit()) {
            balanceManager.savePlayerBalance(player);
        }

        // Track balance on quit for offline monitoring
        if (plugin.getConfigManager().monitorOfflineChanges() &&
                plugin.getConfigManager().getAutoSaveInterval() == 0) {
            double balance = economy.getBalance(player);
            balanceManager.trackPlayerQuit(player.getUniqueId(), balance);
        }
    }
}