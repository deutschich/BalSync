package com.user404_.balsync;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerEventListener implements Listener {
    private final BalSyncPlugin plugin;
    private final BalanceManager balanceManager;

    public PlayerEventListener(BalSyncPlugin plugin, BalanceManager balanceManager) {
        this.plugin = plugin;
        this.balanceManager = balanceManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 2 Sekunden (40 Ticks) Verzögerung
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                balanceManager.loadPlayerBalance(event.getPlayer());
            }
        }, 300L); // 20 Ticks = 1 Sekunde, 300 Ticks = 15 Sekunden
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getConfigManager().saveOnQuit()) {
            balanceManager.savePlayerBalance(event.getPlayer());
        }
    }
}