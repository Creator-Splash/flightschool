package com.a3v1k.flightSchool.blimp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FlightRunnable extends BukkitRunnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(player.getVehicle() instanceof Horse horse)) continue;

            // Optional: Check for a specific horse name or metadata so
            // regular horses don't suddenly start flying.

            applyFlightPhysics(player, horse);
            checkForCollision(player, horse);
        }
    }

    private void applyFlightPhysics(Player player, Horse horse) {
        Vector dir = player.getEyeLocation().getDirection();

        // --- Tuning Constants ---
        double speed = 0.8;
        double verticalSens = 1.2;

        // 1. Calculate new velocity based on player's gaze
        Vector velocity = dir.clone().multiply(speed);

        // 2. Adjust Y specifically for better lift/dive feel
        velocity.setY(velocity.getY() * verticalSens);

        // 3. Apply to horse
        horse.setGravity(false);
        horse.setVelocity(velocity);

        // 4. Force the horse to face where the player looks
        // This prevents the "sliding sideways" look
        horse.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
    }
}
