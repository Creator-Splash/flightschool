package com.a3v1k.flightSchool.platform.paper.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

public final class BundledResourceExporter {

    private static final List<String> BETTER_HUD_RESOURCES = List.of(
        "betterhud/assets/csflight/bounties/bounties_bg.png",
        "betterhud/assets/csflight/elimination/gun.png",
        "betterhud/assets/csflight/elimination/supersoakers.png",
        "betterhud/assets/csflight/icons/dolphin.png",
        "betterhud/assets/csflight/icons/jellyfish.png",
        "betterhud/assets/csflight/icons/octupus.png",
        "betterhud/assets/csflight/icons/orcas.png",
        "betterhud/assets/csflight/icons/right_click.png",
        "betterhud/assets/csflight/icons/seahorse.png",
        "betterhud/assets/csflight/icons/starfish.png",
        "betterhud/assets/csflight/icons/stingray.png",
        "betterhud/assets/csflight/icons/swordfish.png",
        "betterhud/assets/csflight/icons/turtle.png",
        "betterhud/assets/csflight/machinegun.png",
        "betterhud/assets/csflight/powerups/burbuja1.png",
        "betterhud/assets/csflight/powerups/burbuja2.png",
        "betterhud/assets/csflight/powerups/burbuja3.png",
        "betterhud/assets/csflight/powerups/burbuja4.png",
        "betterhud/assets/csflight/powerups/burbuja5.png",
        "betterhud/assets/csflight/powerups/burbuja6.png",
        "betterhud/assets/csflight/powerups/burbuja7.png",
        "betterhud/assets/csflight/powerups/burbuja8.png",
        "betterhud/assets/csflight/powerups/burbuja9.png",
        "betterhud/assets/csflight/rifle.png",
        "betterhud/assets/csflight/sniper.png",
        "betterhud/assets/csflight/splashvoting.png",
        "betterhud/assets/csflight/splashvoting_1.png",
        "betterhud/assets/csflight/splashvoting_2.png",
        "betterhud/assets/csflight/submachine.png",
        "betterhud/assets/csflight/weapon1.png",
        "betterhud/backgrounds/csflight/try.yml",
        "betterhud/backgrounds/csflight/try/body.png",
        "betterhud/backgrounds/csflight/try/left.png",
        "betterhud/backgrounds/csflight/try/right.png",
        "betterhud/fonts/csflight/font.ttf",
        "betterhud/fonts/csflight/KR.ttf",
        "betterhud/fonts/csflight/upheavtt.ttf",
        "betterhud/heads/csflight/heads.yml",
        "betterhud/huds/csflight/centerhud.yml",
        "betterhud/huds/csflight/scoreboard-hud.yml",
        "betterhud/images/csflight/bounties.yml",
        "betterhud/images/csflight/powerups-images.yml",
        "betterhud/images/csflight/splashvoting-image.yml",
        "betterhud/images/csflight/supersoakers-image.yml",
        "betterhud/lang/csflight/de_DE.json",
        "betterhud/lang/csflight/en_US.json",
        "betterhud/lang/csflight/ja_JP.json",
        "betterhud/lang/csflight/ko_KR.json",
        "betterhud/lang/csflight/vi_VN.json",
        "betterhud/lang/csflight/zh_CN.json",
        "betterhud/lang/csflight/zh_TW.json",
        "betterhud/layouts/csflight/bounties-panel.yml",
        "betterhud/layouts/csflight/center_texts.yml",
        "betterhud/layouts/csflight/powerups-layout.yml",
        "betterhud/layouts/csflight/splashvoting-layout.yml",
        "betterhud/layouts/csflight/supersoakers-layout.yml",
        "betterhud/popups/csflight/tutorial-popup.yml",
        "betterhud/shaders/csflight/item.fsh",
        "betterhud/shaders/csflight/item.vsh",
        "betterhud/shaders/csflight/text.fsh",
        "betterhud/shaders/csflight/text.vsh",
        "betterhud/texts/csflight/default.yml"
    );

    private static final List<String> MYTHIC_MOBS_RESOURCES = List.of(
        "mythicmobs/Packs/FlightSchool/packinfo.yml",
        "mythicmobs/Packs/FlightSchool/Mobs/plane_mobs.yml",
        "mythicmobs/Packs/FlightSchool/Mobs/turret.yml",
        "mythicmobs/Packs/FlightSchool/skills/plane_skills.yml",
        "mythicmobs/Packs/FlightSchool/skills/turret.yml",
        "mythicmobs/Packs/FlightSchool/Models/plane/turret.bbmodel"
    );

    private static final Set<String> PRESERVE_IF_EXISTS = Set.of();

    private final JavaPlugin plugin;
    private final Logger logger;

    public BundledResourceExporter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void exportAll() {
        exportToPlugin(BETTER_HUD_RESOURCES, "betterhud/", "BetterHud");
        exportToPlugin(MYTHIC_MOBS_RESOURCES, "mythicmobs/", "MythicMobs");
    }

    private void exportToPlugin(List<String> resourcePaths, String prefix, String pluginFolderName) {
        if (resourcePaths.isEmpty()) return;

        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null) {
            logger.warning("Plugin folder parent could not be resolved. Skipping " + pluginFolderName + " export.");
            return;
        }

        File pluginFolder = new File(pluginsDir, pluginFolderName);
        if (!pluginFolder.exists() && !pluginFolder.mkdirs()) {
            logger.warning("Could not create " + pluginFolderName + " folder. Skipping export.");
            return;
        }

        int exported = 0;
        for (String resourcePath : resourcePaths) {
            if (exportSingleResource(resourcePath, prefix, pluginFolder)) {
                exported++;
            }
        }

        if (exported > 0) {
            logger.info("Exported " + exported + " bundled " + pluginFolderName + " resource(s).");
        }
    }

    private boolean exportSingleResource(String resourcePath, String prefix, File pluginFolder) {
        if (!resourcePath.startsWith(prefix)) return false;

        File target = new File(pluginFolder, resourcePath.substring(prefix.length()));
        if (target.exists() && PRESERVE_IF_EXISTS.contains(resourcePath)) {
            return false;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warning("Could not create folder for " + target.getAbsolutePath());
            return false;
        }

        try (InputStream in = plugin.getResource(resourcePath)) {
            if (in == null) {
                logger.warning("Bundled resource not found in jar: " + resourcePath);
                return false;
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            logger.severe("Failed to export " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }
}
