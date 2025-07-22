package me.fiveave.untenshi;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static me.fiveave.untenshi.cmds.errorLog;
import static me.fiveave.untenshi.cmds.generalMsg;

class abstractfile {
    protected final main plugin;
    final FileConfiguration oldconfig;
    FileConfiguration dataconfig;
    private File file;

    abstractfile(main plugin, String fileName) {
        this.plugin = plugin;
        file = new File(plugin.getDataFolder(), fileName);
        dataconfig = YamlConfiguration.loadConfiguration(file);
        oldconfig = YamlConfiguration.loadConfiguration(file);
        saveDefaultConfig();
        reloadConfig();
    }

    void reloadConfig() {
        // Default config from plugin itself
        // dataconfig and oldconfig from local files
        InputStream stream = plugin.getResource(file.getName());
        if (stream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
            if (file.exists() && !dataconfig.getKeys(true).containsAll(defaultConfig.getKeys(true))) {
                plugin.saveResource(file.getName(), true);
                // Get actual data
                dataconfig = YamlConfiguration.loadConfiguration(file);
                // Whole required list
                Set<String> setstr = new HashSet<>(oldconfig.getKeys(true));
                // 1st node != not 1st node then remove 1st node
                if (!oldconfig.getKeys(true).equals(oldconfig.getKeys(false))) {
                    setstr.removeAll(oldconfig.getKeys(false));
                }
                // Add back defaults
                dataconfig.addDefaults(defaultConfig);
                // Put new data config to default values
                setstr.forEach(str -> {
                    if (!Objects.equals(oldconfig.get(str), dataconfig.get(str))) {
                        if (oldconfig.get(str) != null) {
                            dataconfig.set(str, oldconfig.get(str));
                        }
                    }
                });
                // Save file
                plugin.saveResource(file.getName(), true);
                try {
                    dataconfig.save(file);
                } catch (IOException e) {
                    errorLog(e, "abstractfile.reloadConfig");
                }
                generalMsg(Bukkit.getConsoleSender(), ChatColor.YELLOW, file.getName() + " has been updated due to missing content");
            }
        }
    }

    void save() {
        if (dataconfig == null || file == null) {
            return;
        }
        try {
            dataconfig.save(file);
        } catch (IOException e) {
            errorLog(e, "abstractfile.save");
        }
    }

    void saveDefaultConfig() {
        if (file == null) {
            assert false;
            file = new File(plugin.getDataFolder(), file.getName());
        }
        if (!file.exists()) {
            plugin.saveResource(file.getName(), false);
            dataconfig = YamlConfiguration.loadConfiguration(file);
        }
    }
}