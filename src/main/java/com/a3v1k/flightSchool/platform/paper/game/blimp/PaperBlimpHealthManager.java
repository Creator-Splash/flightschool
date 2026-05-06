package com.a3v1k.flightSchool.platform.paper.game.blimp;

import com.a3v1k.flightSchool.application.game.BlimpHealthManager;
import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public final class PaperBlimpHealthManager implements BlimpHealthManager {

    /** Used when a turret mob's max health can't be read (despawned, non-LivingEntity). */
    private static final double FALLBACK_TURRET_HEALTH = 150;

    private final List<ActiveMob> activeMobs;
    private final Team team;
    private final FlightSchool plugin;
    private final int initialTurretCount;
    private final double maxHealth;

    @Getter
    private double trackedHealth;
    @Getter
    private boolean broken = false;

    private TextDisplay display;

    public PaperBlimpHealthManager(List<ActiveMob> activeMobs, Team team, FlightSchool plugin) {
        this.activeMobs = activeMobs;
        this.team = team;
        this.plugin = plugin;
        this.initialTurretCount = Math.max(1, activeMobs.size());
        this.maxHealth = determineMaxHealth(activeMobs);
        this.trackedHealth = this.maxHealth;
    }

    @Override
    public void update() {
        if (broken) return;

        if (trackedHealth <= 0) {
            broken = true;
            announceBlimpDestroyed();
        }

        updateDisplay();
    }

    @Override
    public double getHealth() {
        // Existing contract: return percentage in [0, 100]. Computed from the tracked
        // health pool so damage() decrements are visible to the placeholder layer.
        if (maxHealth <= 0) return 0;
        double pct = (trackedHealth / maxHealth) * 100.0;
        if (Double.isNaN(pct)) return 0;
        return Math.max(0, Math.min(100, pct));
    }

    @Override
    public int getHealthPercentage() {
        if (maxHealth <= 0) return 0;
        return (int) Math.ceil((trackedHealth / maxHealth) * 100.0);
    }

    @Override
    public void damage(double damage) {
        if (broken || team.getMembers().isEmpty()) return;
        trackedHealth -= damage;
        update();
    }

    @Override
    public boolean hasAllTurretsDestroyed() {
        return team.getDestroyedBlimps() >= initialTurretCount;
    }

    @Override
    public void disableAndDespawn() {
        if (display != null) {
            display.remove();
            display = null;
        }

        for (ActiveMob activeMob : activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) continue;
            activeMob.despawn();
        }
    }

    /* == TextDisplay management == */

    private void updateDisplay() {
        if (team.getMembers().isEmpty()) return;

        if (display == null) {
            Location displayLocation = getDisplayLocation();
            if (displayLocation == null || displayLocation.getWorld() == null) return;

            displayLocation.getWorld().spawn(displayLocation, TextDisplay.class, textDisplay -> {
                this.display = textDisplay;

                Transformation transformation = textDisplay.getTransformation();
                transformation.getScale().set(40);

                textDisplay.setTransformation(transformation);
                textDisplay.setBillboard(Display.Billboard.CENTER);
                textDisplay.setShadowed(false);
                textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                textDisplay.setViewRange(1000);
            });
        }

        if (display != null) {
            display.text(getHealthDisplayComponent());
        }
    }

    private Location getDisplayLocation() {
        BlimpData blimp = plugin.getGameManager().getBlimp(team.getName());
        if (blimp != null && blimp.getCenter() != null) {
            return blimp.getCenter().clone().add(0, 50, 0);
        }
        plugin.getLogger().warning("[PaperBlimpHealthManager] No BlimpData center for team "
            + team.getName() + " — TextDisplay anchor unavailable.");
        return null;
    }

    private Component getHealthDisplayComponent() {
        if (broken) {
            return Component.text("Destroyed", NamedTextColor.RED);
        }

        int healthPercentage = getHealthPercentage();

        StringBuilder green = new StringBuilder();
        StringBuilder red = new StringBuilder();

        // Mirrors the diff: green bars while bars <= percentage/2, red for the rest up
        // to a 50-bar total. Edge case: at 100% the green loop overshoots to 51 bars
        // (preserved verbatim so the visual matches the source).
        int bars = 0;
        while (bars <= healthPercentage / 2) {
            green.append('|');
            bars++;
        }
        while (bars < 50) {
            red.append('|');
            bars++;
        }

        return Component.text()
            .append(Component.text(green.toString(), NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
            .append(Component.text(red.toString(), NamedTextColor.RED).decorate(TextDecoration.BOLD))
            .build();
    }

    /* == Helpers == */

    private void announceBlimpDestroyed() {
        Title title = Title.title(
            Component.text("Blimp Destroyed", NamedTextColor.RED),
            Component.text("Your team has been eliminated!", NamedTextColor.GOLD),
            Title.Times.times(
                Duration.ofMillis(250),
                Duration.ofSeconds(1),
                Duration.ofMillis(250)
            )
        );

        for (UUID uuid : team.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.showTitle(title);
            }
        }
    }

    private double determineMaxHealth(List<ActiveMob> mobs) {
        double total = 0;

        for (ActiveMob activeMob : mobs) {
            if (activeMob == null || activeMob.getEntity() == null) {
                total += FALLBACK_TURRET_HEALTH;
                continue;
            }

            if (activeMob.getEntity().getBukkitEntity() instanceof LivingEntity le) {
                AttributeInstance maxHealthAttr = le.getAttribute(Attribute.MAX_HEALTH);
                total += maxHealthAttr != null ? maxHealthAttr.getValue() : FALLBACK_TURRET_HEALTH;
            } else {
                total += FALLBACK_TURRET_HEALTH;
            }
        }

        return total <= 0 ? FALLBACK_TURRET_HEALTH : total;
    }
}
