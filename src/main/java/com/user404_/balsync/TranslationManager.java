package com.user404_.balsync;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private final BalSyncPlugin plugin;
    private final Map<String, String> messages;
    private String locale;

    public TranslationManager(BalSyncPlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
    }

    public void loadMessages() {
        messages.clear();
        locale = plugin.getConfigManager().getLocale();

        // First, load default messages from JAR
        try (Reader reader = new InputStreamReader(
                plugin.getResource("messages_en.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(reader);
            loadConfigIntoMap(defaultConfig);
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Failed to load default messages!");
        }

        // Load locale-specific file if different from English
        if (!locale.equalsIgnoreCase("en")) {
            String fileName = "messages_" + locale.toLowerCase() + ".yml";
            File localeFile = new File(plugin.getDataFolder(), fileName);

            // Copy from resources if doesn't exist
            if (!localeFile.exists()) {
                plugin.saveResource(fileName, false);
            }

            // Load the file
            if (localeFile.exists()) {
                YamlConfiguration localeConfig = YamlConfiguration.loadConfiguration(localeFile);
                loadConfigIntoMap(localeConfig);
            }
        }

        // Finally, load user overrides from data folder
        File userFile = new File(plugin.getDataFolder(), "messages.yml");
        if (userFile.exists()) {
            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(userFile);
            loadConfigIntoMap(userConfig);
        }

        plugin.getPluginLogger().info("Loaded messages for locale: " + locale);
    }

    private void loadConfigIntoMap(YamlConfiguration config) {
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public String formatMessage(String key, Object... args) {
        String message = getMessage(key);
        String prefix = getMessage("prefix");

        if (args.length > 0) {
            try {
                message = MessageFormat.format(message, args);
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Failed to format message: " + key);
            }
        }

        return prefix + message.replace('&', '§');
    }

    public String getLocale() {
        return locale;
    }
}