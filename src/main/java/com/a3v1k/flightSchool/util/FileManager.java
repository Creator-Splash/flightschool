package com.a3v1k.flightSchool.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;

/**
 * Manages file creation and access.
 */
public class FileManager {

    private File file;
    private String fileName;
    private JavaPlugin javaPlugin;

    /**
     * Constructor for FileManager.
     * @param fileName The name of the file.
     * @param javaPlugin The main plugin instance.
     */
    public FileManager(String fileName, JavaPlugin javaPlugin) {
        this.fileName = fileName;
        this.javaPlugin = javaPlugin;
    }

    /**
     * Creates the file if it doesn't exist.
     * @throws IOException If an I/O error occurs.
     */
    public void createFile() throws IOException {
        if(!javaPlugin.getDataFolder().exists()) {
            javaPlugin.getDataFolder().mkdir();
        }

        this.file = new File(javaPlugin.getDataFolder(), fileName);
        if(!file.exists()) {
            file.createNewFile();
        }
    }


    /**
     * Gets the file.
     * @return The file.
     * @throws IOException If an I/O error occurs.
     */
    public File getFile() throws IOException {
        this.file = new File(javaPlugin.getDataFolder(), fileName);
        if(!this.file.exists()) {
            this.createFile();
        }

        return this.file;
    }

    /**
     * Gets the YAML configuration for the file.
     * @return The YAML configuration.
     */
    public @NotNull YamlConfiguration getConfig() {
        return YamlConfiguration.loadConfiguration(this.file);
    }

    /**
     * Saves the YAML configuration to the file.
     * @param config The YAML configuration to save.
     * @throws IOException If an I/O error occurs.
     */
    public void save(YamlConfiguration config) throws IOException {
        config.save(this.file);
    }
}
