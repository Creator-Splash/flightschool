package com.a3v1k.flightSchool.application.blimp;

import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FlightRunnable extends BukkitRunnable {

    private static final double COLLISION_LOOKAHEAD = 3.0D;
    private static final double COLLISION_STEP = 0.5D;
    private static final double BOUNCE_SPEED = 1.35D;

    private final FlightSchool plugin;
    private final GameManager gameManager;
    private final NamespacedKey ownerKey;

    private final Set<UUID> cancelPlayerPlaneMovements = new HashSet<>();

    public FlightRunnable(FlightSchool plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.ownerKey = new NamespacedKey(plugin, "owner_uuid");
    }

    @Override
    public void run() {
        if (gameManager.getGameState() != GameState.IN_GAME) {
            cancel();
            return;
        }

        Set<UUID> activePlaneIds = new HashSet<>();

        for (List<ActiveMob> activeMobs : gameManager.getTeamPlanes().values()) {
            if (activeMobs == null) {
                continue;
            }

            for (ActiveMob activeMob : activeMobs) {
                if (activeMob != null && activeMob.getEntity() != null) {
                    activePlaneIds.add(activeMob.getEntity().getUniqueId());
                }
                checkForCollision(activeMob);
            }
        }

    }

    private void checkForCollision(ActiveMob activeMob) {
        if (activeMob == null || activeMob.isDead() || activeMob.getEntity() == null) {
            return;
        }

        Entity plane = activeMob.getEntity().getBukkitEntity();
        if (plane == null || !plane.isValid()) {
            return;
        }

        Player player = getOwner(plane);
        if (player == null) {
            return;
        }

        Location collisionLocation = findCollisionLocation(plane, player.getLocation().getDirection());
        if (collisionLocation == null) {
            return;
        }

        GamePlayer gamePlayer = gameManager.getGamePlayer(player);
        Team playerTeam = gamePlayer == null ? null : gamePlayer.getTeam();
        String nearestBlimpTeam = findNearestBlimpTeam(collisionLocation);

        if (playerTeam != null && playerTeam.getName().equalsIgnoreCase(nearestBlimpTeam)) {
            bounceBack(player, plane, player.getLocation().add(0, player.getEyeHeight() / 2, 0).subtract(collisionLocation).toVector().normalize()/*direction away from collision point*/);
            return;
        }

        crashPlane(plane);
    }


    private Location findCollisionLocation(Entity plane, Vector direction) {
        for (double distance = COLLISION_STEP; distance <= COLLISION_LOOKAHEAD; distance += COLLISION_STEP) {
            Vector offset = direction.clone().multiply(distance);
            BoundingBox projectedBox = plane.getBoundingBox()
                    .clone()
                    .expand(1.0D, 0.0D, 1.0D)
                    .shift(offset);
            Location collisionLocation = findSolidBlockIn(projectedBox, plane.getWorld());

            if (collisionLocation != null) {
                return collisionLocation;
            }
        }

        return null;
    }

    private Location findSolidBlockIn(BoundingBox box, World world) {
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.floor(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isSolid()) {
                        continue;
                    }

                    return block.getLocation();
                }
            }
        }

        return null;
    }

    private String findNearestBlimpTeam(Location location) {
        String nearestTeam = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (BlimpData blimp : gameManager.getRuntime().getBlimps().values()) {
            for (int segment = 0; segment < blimp.segmentCount(); segment++) {
                for (Location blimpBlock : blimp.getSegment(segment)) {
                    if (!blimpBlock.getWorld().equals(location.getWorld())) {
                        continue;
                    }

                    double distanceSquared = blimpBlock.distanceSquared(location);
                    if (distanceSquared >= nearestDistanceSquared) {
                        continue;
                    }

                    nearestDistanceSquared = distanceSquared;
                    nearestTeam = blimp.getTeamName();
                }
            }
        }

        return nearestTeam;
    }

    private Player getOwner(Entity plane) {
        String ownerUuid = plane.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (ownerUuid == null) {
            return null;
        }

        try {
            return Bukkit.getPlayer(UUID.fromString(ownerUuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }


    private void bounceBack(Player player, Entity plane, Vector direction) {
        Vector velocity = direction.clone().normalize().multiply(BOUNCE_SPEED);

        plane.setVelocity(velocity);
        plane.setFallDistance(0.0F);
        player.setFallDistance(0.0F);

        cancelPlayerPlaneMovements.add(player.getUniqueId());
        new BukkitRunnable() {
            public void run() {
                cancelPlayerPlaneMovements.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 4L);

    }

    private void crashPlane(Entity plane) {
        if (plane instanceof LivingEntity livingEntity && !livingEntity.isDead()) {
            livingEntity.setHealth(0.0D);
        }
    }
}
