package com.a3v1k.flightSchool.application.game;

import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.platform.paper.listener.GameListener;
import com.a3v1k.flightSchool.platform.paper.util.PdcKeys;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class AirspaceManager extends BukkitRunnable {

    private static final long WARNING_COOLDOWN_MILLIS = 1_500L;
    private static final double BOUNDARY_WARNING_DISTANCE = 24.0D;
    private static final float BOUNDARY_DISPLAY_SCALE = 800.0F;
    private static final float BOUNDARY_VIEW_RANGE = 64.0F;
    private static final int BOUNDARY_TEXT_SIZE = 10;
    private static final int MIN_BOUNDARY_ALPHA = 24;
    private static final int MAX_BOUNDARY_ALPHA = 190;

    private final FlightSchool plugin;
    private final GameManager gameManager;
    private final int minFlightY;
    private final int maxFlightY;
    private final Map<UUID, Long> lastWarningAt = new HashMap<>();
    private final Map<UUID, BoundaryDisplays> boundaryDisplays = new HashMap<>();

    @Override
    public void run() {
        if (gameManager.getGameState() != GameState.IN_GAME) {
            shutdown();
            return;
        }

        Set<UUID> activePilots = new HashSet<>();
        Set<UUID> activePlanes = new HashSet<>();

        for (List<ActiveMob> activeMobs : gameManager.getTeamPlanes().values()) {
            if (activeMobs == null) {
                continue;
            }

            for (ActiveMob activeMob : activeMobs) {
                updatePlaneBoundary(activeMob, activePilots, activePlanes);
            }
        }

        cleanupStaleState(activePilots, activePlanes);
    }

    public void shutdown() {
        removeAllDisplays();

        try {
            cancel();
        } catch (IllegalStateException ignored) {
        }
    }

    private void updatePlaneBoundary(ActiveMob activeMob, Set<UUID> activePilots, Set<UUID> activePlanes) {
        if (activeMob == null || activeMob.isDead() || activeMob.getEntity() == null) {
            return;
        }

        Entity plane = activeMob.getEntity().getBukkitEntity();
        if (plane == null || !plane.isValid()) {
            return;
        }

        activePlanes.add(plane.getUniqueId());

        Player player = getOwner(plane);
        if (player == null) {
            return;
        }

        activePilots.add(player.getUniqueId());

        if (plane.getLocation().getY() > maxFlightY) {
            crashPlane(activeMob, plane, player, "ceiling");
            return;
        }

        if (plane.getLocation().getY() < minFlightY) {
            crashPlane(activeMob, plane, player, "floor");
            return;
        }

        updateBoundaryDisplays(player, plane);
    }

    private void crashPlane(ActiveMob activeMob, Entity plane, Player player, String boundaryName) {
        player.sendActionBar(Component.text("You crashed into the airspace " + boundaryName, NamedTextColor.RED));

        if (plane instanceof LivingEntity livingEntity) {
            if (!livingEntity.isDead()) {
                livingEntity.setHealth(0.0D);
            }
            return;
        }


        activeMob.despawn();
    }

    private void updateBoundaryDisplays(Player player, Entity plane) {
        BoundaryDisplays displays = getOrCreateDisplays(player, plane.getWorld());
        Location planeLocation = plane.getLocation().clone();

        double distanceToTop = maxFlightY - planeLocation.getY();
        double distanceToBottom = planeLocation.getY() - minFlightY;

        updateBoundaryDisplay(player, displays.topDisplay, planeLocation, maxFlightY, distanceToTop);
        updateBoundaryDisplay(player, displays.bottomDisplay, planeLocation, minFlightY, distanceToBottom);
    }

    private void updateBoundaryDisplay(Player player, TextDisplay display, Location planeLocation, int boundaryY, double distanceToBoundary) {
        if (distanceToBoundary > BOUNDARY_WARNING_DISTANCE) {
            hideDisplay(player, display);
            return;
        }

        double zOffset = BOUNDARY_DISPLAY_SCALE / 9;

        boolean ceiling = planeLocation.getY() > (double) (maxFlightY + minFlightY) / 2;
        Location currentDisplayLocation = display.getLocation();
        Location displayLocation = new Location(
                planeLocation.getWorld(),
                planeLocation.getX(),
                planeLocation.getY() + (ceiling ? 5 : -2 /*needed because the text display can bug visually when you get close to it*/),
                planeLocation.getZ() + (ceiling ? -zOffset : zOffset),
                currentDisplayLocation.getYaw(),
                currentDisplayLocation.getPitch()
        );
        display.teleport(displayLocation);
        display.setBackgroundColor(buildBoundaryBackground(distanceToBoundary));

        if (!player.canSee(display)) {
            player.showEntity(plugin, display);
        }
    }

    private BoundaryDisplays getOrCreateDisplays(Player player, World world) {
        BoundaryDisplays existingDisplays = boundaryDisplays.get(player.getUniqueId());

        if (existingDisplays == null || !existingDisplays.isValidFor(world)) {
            if (existingDisplays != null) {
                existingDisplays.remove();
            }

            BoundaryDisplays newDisplays = new BoundaryDisplays(
                    createBoundaryDisplay(world, true),
                    createBoundaryDisplay(world, false)
            );
            boundaryDisplays.put(player.getUniqueId(), newDisplays);
            return newDisplays;
        }

        return existingDisplays;
    }

    private TextDisplay createBoundaryDisplay(World world, boolean facingDown) {
        TextDisplay display = world.spawn(new Location(world, 0, facingDown ? maxFlightY : minFlightY, 0), TextDisplay.class);
        display.setVisibleByDefault(false);
        display.setPersistent(false);
        display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
        display.setShadowed(false);
        display.setSeeThrough(true);
        display.setDefaultBackground(false);
        display.setLineWidth(2048);
        display.setTextOpacity((byte) 0);
        display.setInterpolationDelay(0);
        display.setTeleportDuration(1);
        display.setViewRange(BOUNDARY_VIEW_RANGE);
        display.setTransformation(createBoundaryTransformation(facingDown));
        display.text(Component.text("  "));
        display.setBackgroundColor(buildBoundaryBackground(BOUNDARY_WARNING_DISTANCE));
        return display;
    }

    private Transformation createBoundaryTransformation(boolean facingDown) {
        float rotation = (float) Math.toRadians(facingDown ? 90.0D : -90.0D);
        return new Transformation(
                new Vector3f(),
                new AxisAngle4f(rotation, 1.0F, 0.0F, 0.0F),
                new Vector3f(BOUNDARY_DISPLAY_SCALE, BOUNDARY_DISPLAY_SCALE, BOUNDARY_DISPLAY_SCALE),
                new AxisAngle4f()
        );
    }

    private Color buildBoundaryBackground(double distanceToBoundary) {
        double clampedDistance = Math.max(0.0D, Math.min(distanceToBoundary, BOUNDARY_WARNING_DISTANCE));
        double intensity = 1.0D - (clampedDistance / BOUNDARY_WARNING_DISTANCE);
        int alpha = (int) Math.round(MIN_BOUNDARY_ALPHA + ((MAX_BOUNDARY_ALPHA - MIN_BOUNDARY_ALPHA) * intensity));
        return Color.fromARGB(alpha, 255, 0, 0);
    }

    private Player getOwner(Entity plane) {
        String ownerUuid = plane.getPersistentDataContainer()
            .get(PdcKeys.OWNER_UUID, PersistentDataType.STRING);
        if (ownerUuid == null) {
            return null;
        }

        try {
            return Bukkit.getPlayer(UUID.fromString(ownerUuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void warnPilot(Player player, String boundaryName) {
        long now = System.currentTimeMillis();
        long lastWarning = lastWarningAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastWarning < WARNING_COOLDOWN_MILLIS) {
            return;
        }

        player.sendActionBar(Component.text("You hit the airspace " + boundaryName, NamedTextColor.RED));
        lastWarningAt.put(player.getUniqueId(), now);
    }

    private void hideDisplay(Player player, TextDisplay display) {
        if (display.isValid() && player.canSee(display)) {
            player.hideEntity(plugin, display);
        }
    }

    private void cleanupStaleState(Set<UUID> activePilots, Set<UUID> activePlanes) {
        boundaryDisplays.entrySet().removeIf(entry -> {
            if (activePilots.contains(entry.getKey())) {
                return false;
            }

            entry.getValue().remove();
            return true;
        });

        lastWarningAt.keySet().removeIf(uuid -> !activePilots.contains(uuid));
    }

    private void removeAllDisplays() {
        for (BoundaryDisplays displays : boundaryDisplays.values()) {
            displays.remove();
        }

        boundaryDisplays.clear();
        lastWarningAt.clear();
    }


    private static final class BoundaryDisplays {
        private final TextDisplay topDisplay;
        private final TextDisplay bottomDisplay;

        private BoundaryDisplays(TextDisplay topDisplay, TextDisplay bottomDisplay) {
            this.topDisplay = topDisplay;
            this.bottomDisplay = bottomDisplay;
        }

        private boolean isValidFor(World world) {
            return topDisplay.isValid()
                    && bottomDisplay.isValid()
                    && topDisplay.getWorld().equals(world)
                    && bottomDisplay.getWorld().equals(world);
        }

        private void remove() {
            topDisplay.remove();
            bottomDisplay.remove();
        }
    }
}
