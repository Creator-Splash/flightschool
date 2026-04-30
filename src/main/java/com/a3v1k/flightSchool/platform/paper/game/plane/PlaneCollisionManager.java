package com.a3v1k.flightSchool.platform.paper.game.plane;

import com.a3v1k.flightSchool.application.game.GameManager;
import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import com.a3v1k.flightSchool.domain.match.GameState;
import com.a3v1k.flightSchool.domain.player.GamePlayer;
import com.a3v1k.flightSchool.domain.team.Team;
import com.a3v1k.flightSchool.platform.paper.game.PaperGameManager;
import com.a3v1k.flightSchool.platform.paper.util.PdcKeys;
import io.lumine.mythic.core.mobs.ActiveMob;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public final class PlaneCollisionManager {

    private static final double COLLISION_LOOKAHEAD = 3.0D;
    private static final double COLLISION_STEP = 0.5D;
    private static final double BOUNCE_SPEED = 1.35D;

    private final FlightSchool plugin;
    private final GameManager gameManager;

    private Scheduler.Task task;

    public void start(Scheduler scheduler) {
        task = scheduler.runRepeating(this::update, 0L, 1L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void update() {
        if (gameManager.getGameState() != GameState.IN_GAME) {
            stop();
            return;
        }

        if (!(plugin.getGameManager() instanceof PaperGameManager paperGameManager)) return;

        for (List<ActiveMob> activeMobs : paperGameManager.getTeamPlanes().values()) {
            if (activeMobs == null) continue;
            for (ActiveMob activeMob : activeMobs) {
                checkForCollision(activeMob);
            }
        }
    }

    private void checkForCollision(ActiveMob activeMob) {
        if (activeMob == null || activeMob.isDead() || activeMob.getEntity() == null) return;

        Entity plane = activeMob.getEntity().getBukkitEntity();
        if (plane == null || !plane.isValid()) return;

        Player player = getOwnerPlayer(plane);
        if (player == null) return;

        Location collisionLocation = findCollisionLocation(plane, player.getLocation().getDirection());
        if (collisionLocation == null) return;

        GamePlayer gamePlayer = gameManager.getGamePlayer(player.getUniqueId());
        Team playerTeam = gamePlayer == null ? null : gamePlayer.getTeam();
        String nearestBlimpTeam = findNearestBlimpTeam(collisionLocation);

        if (playerTeam != null && playerTeam.getName().equalsIgnoreCase(nearestBlimpTeam)) {
            Vector bounceDirection = player.getLocation()
                .add(0, player.getEyeHeight() / 2, 0)
                .subtract(collisionLocation)
                .toVector()
                .normalize();
            bounceBack(player, plane, bounceDirection);
            return;
        }

        crashPlane(plane);
    }

    private Location findCollisionLocation(Entity plane, Vector direction) {
        for (double distance = COLLISION_STEP; distance <= COLLISION_LOOKAHEAD; distance += COLLISION_STEP) {
            BoundingBox projectedBox = plane.getBoundingBox()
                .clone()
                .expand(1.0D, 0.0D, 1.0D)
                .shift(direction.clone().multiply(distance));

            Location collision = findSolidBlockIn(projectedBox, plane.getWorld());
            if (collision != null) return collision;
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
                    if (block.getType().isSolid()) return block.getLocation();
                }
            }
        }
        return null;
    }

    private String findNearestBlimpTeam(Location location) {
        String nearestTeam = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        if (!(plugin.getGameManager() instanceof PaperGameManager paperGameManager)) return null;

        for (BlimpData blimp : paperGameManager.getRuntime().getBlimps().values()) {
            for (int segment = 0; segment < blimp.segmentCount(); segment++) {
                for (Location blimpBlock : blimp.getSegment(segment)) {
                    if (!blimpBlock.getWorld().equals(location.getWorld())) continue;

                    double distanceSquared = blimpBlock.distanceSquared(location);
                    if (distanceSquared >= nearestDistanceSquared) continue;

                    nearestDistanceSquared = distanceSquared;
                    nearestTeam = blimp.getTeamName();
                }
            }
        }

        return nearestTeam;
    }

    private void bounceBack(Player player, Entity plane, Vector direction) {
        Vector velocity = direction.clone().normalize().multiply(BOUNCE_SPEED);
        plane.setVelocity(velocity);
        plane.setFallDistance(0.0F);
        player.setFallDistance(0.0F);
    }

    private void crashPlane(Entity plane) {
        if (plane instanceof LivingEntity le && !le.isDead()) {
            le.setHealth(0.0D);
        }
    }

    private Player getOwnerPlayer(Entity plane) {
        String uuidString = plane.getPersistentDataContainer()
            .get(PdcKeys.OWNER_UUID, PersistentDataType.STRING);
        if (uuidString == null) return null;

        try {
            return Bukkit.getPlayer(UUID.fromString(uuidString));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
