package com.user404_.balsync;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {
    private final BalSyncPlugin plugin;
    private final String modrinthProjectId = "lWNvJAlY";
    private final String modrinthApiUrl = "https://api.modrinth.com/v2/project/" + modrinthProjectId + "/version";
    private final String projectPageUrl = "https://modrinth.com/plugin/BalSync";

    private volatile boolean updateAvailable = false;
    private volatile boolean compatible = false;
    private volatile String latestVersion;

    public UpdateChecker(BalSyncPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                performCheck();
            }
        }.runTaskAsynchronously(plugin);
    }

    private void performCheck() {
        try {
            URL url = new URL(modrinthApiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "BalSync/" + plugin.getDescription().getVersion() + " (UpdateChecker)");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonArray versionsArray = JsonParser.parseReader(reader).getAsJsonArray();

                    // Find the latest release version (fallback to first if no release)
                    JsonObject latest = null;
                    for (JsonElement element : versionsArray) {
                        JsonObject versionObj = element.getAsJsonObject();
                        String versionType = versionObj.get("version_type").getAsString();
                        if ("release".equals(versionType)) {
                            latest = versionObj;
                            break;
                        }
                    }
                    if (latest == null && versionsArray.size() > 0) {
                        latest = versionsArray.get(0).getAsJsonObject();
                    }

                    if (latest != null) {
                        String latestVer = latest.get("version_number").getAsString();
                        // Check game version compatibility
                        JsonArray gameVersions = latest.getAsJsonArray("game_versions");
                        String serverVersion = getServerMinecraftVersion();
                        boolean compatibleWithServer = false;
                        for (JsonElement gv : gameVersions) {
                            if (gv.getAsString().equals(serverVersion)) {
                                compatibleWithServer = true;
                                break;
                            }
                        }

                        // Check loader compatibility (Bukkit/Spigot/Paper)
                        JsonArray loaders = latest.getAsJsonArray("loaders");
                        boolean compatibleLoader = false;
                        for (JsonElement loader : loaders) {
                            String l = loader.getAsString().toLowerCase();
                            if (l.contains("paper") || l.contains("spigot") || l.contains("bukkit")) {
                                compatibleLoader = true;
                                break;
                            }
                        }

                        compatible = compatibleWithServer && compatibleLoader;

                        // Compare versions (simple string inequality – you may refine this)
                        String currentVersion = plugin.getDescription().getVersion();
                        if (!currentVersion.equalsIgnoreCase(latestVer) && compatible) {
                            updateAvailable = true;
                            latestVersion = latestVer;
                            plugin.getLogger().info("A new update is available: " + latestVer + " (current: " + currentVersion + ")");
                        } else {
                            updateAvailable = false;
                        }
                    }
                }
            } else {
                plugin.getLogger().warning("Failed to check for updates: HTTP " + responseCode);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not check for updates", e);
        }
    }

    private String getServerMinecraftVersion() {
        String bukkitVersion = Bukkit.getBukkitVersion(); // e.g., "1.21-R0.1-SNAPSHOT"
        return bukkitVersion.split("-")[0];
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public boolean isCompatible() {
        return compatible;
    }

    /**
     * Sends a clickable update notification to the player.
     */
    public void sendUpdateMessage(Player player) {
        if (!updateAvailable) return;
        String currentVersion = plugin.getDescription().getVersion();
        String prefix = plugin.getTranslationManager().getMessage("prefix").replace('&', '§');
        String line1 = "§eNew update available! §a(v" + latestVersion + ")§e, you are currently on §av"+ currentVersion + ".";
        String line2 = "§eDownload now: ";

        net.md_5.bungee.api.chat.TextComponent msgPrefix = new net.md_5.bungee.api.chat.TextComponent(prefix);
        net.md_5.bungee.api.chat.TextComponent line1Comp = new net.md_5.bungee.api.chat.TextComponent(line1);
        net.md_5.bungee.api.chat.TextComponent newLine = new net.md_5.bungee.api.chat.TextComponent("\n");
        net.md_5.bungee.api.chat.TextComponent line2Prefix = new net.md_5.bungee.api.chat.TextComponent(line2);
        net.md_5.bungee.api.chat.TextComponent linkComp = new net.md_5.bungee.api.chat.TextComponent("§9§n" + projectPageUrl);
        linkComp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, projectPageUrl));
        linkComp.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("Click to download").create()));

        net.md_5.bungee.api.chat.TextComponent finalMsg = new net.md_5.bungee.api.chat.TextComponent();
        finalMsg.addExtra(msgPrefix);
        finalMsg.addExtra(line1Comp);
        finalMsg.addExtra(newLine);
        finalMsg.addExtra(msgPrefix);
        finalMsg.addExtra(line2Prefix);
        finalMsg.addExtra(linkComp);

        player.spigot().sendMessage(finalMsg);
    }
}