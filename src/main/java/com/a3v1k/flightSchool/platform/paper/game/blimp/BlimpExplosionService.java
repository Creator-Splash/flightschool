package com.a3v1k.flightSchool.platform.paper.game.blimp;

import com.a3v1k.flightSchool.application.scheduler.Scheduler;
import com.a3v1k.flightSchool.domain.blimp.BlimpData;
import com.a3v1k.flightSchool.domain.team.Team;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.entity.FallingBlock;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

@RequiredArgsConstructor
public final class BlimpExplosionService {

    private static final Material[] DEBRIS = {
        Material.WHITE_WOOL,
        Material.STONE,
        Material.IRON_BLOCK,
        Material.OAK_PLANKS,
        Material.CHAIN
    };

    private static final int EXPLOSION_DURATION_TICKS = 25 * 20;

    private final Scheduler scheduler;
    private final Logger logger;
    private final Random random = new Random();

    public void explode(BlimpData blimp, Team team) {
        if (blimp == null || team == null) {
            logger.warning("explode called with null blimp or team");
            return;
        }

        List<Location> firstSegment = blimp.getSegment(0);
        if (firstSegment.isEmpty()) {
            logger.warning("Blimp for team " + team.getName() + " has empty first segment");
            return;
        }

        World world = firstSegment.getFirst().getWorld();
        Color color = team.getColor();

        scheduler.runRepeating(t -> {
            int elapsed = t.elapsedTicks();
            int tick = elapsed * 2;

            if (tick > EXPLOSION_DURATION_TICKS) {
                t.cancel();
                return;
            }

            int phase;
            if (tick < 40) phase = 2;
            else if (tick < 80) phase = elapsed % 2 == 0 ? 1 : 3;
            else phase = elapsed % 2 == 0 ? 0 : 5;

            List<Location> pool = blimp.getSegment(phase);
            if (pool.isEmpty()) return;

            int explosions = phase == 2 ? 24 : 14;

            for (int i = 0; i < explosions; i++) {
                Location loc = pool.get(random.nextInt(pool.size()));
                world.spawnParticle(Particle.EXPLOSION, loc, 1);
                world.spawnParticle(Particle.LARGE_SMOKE, loc, 10, 1.5, 1.5, 1.5);
                world.spawnParticle(Particle.DUST, loc, 30, 1.8, 1.8, 1.8,
                        new Particle.DustOptions(color, 2.2f));
                spawnDebris(world, loc, phase);
            }

            if (tick % 12 == 0) {
                world.playSound(pool.getFirst(), Sound.ENTITY_GENERIC_EXPLODE, 6f, 0.5f);
            }

        }, 0L, 2L);
    }

    private void spawnDebris(World world, Location loc, int segment) {
        Material mat = DEBRIS[segment % DEBRIS.length];

        FallingBlock block = world.spawn(
            loc.clone().add(0, 0.5, 0),
            FallingBlock.class
        );

        block.setBlockData(mat.createBlockData());
        block.setVelocity(new org.bukkit.util.Vector(
            randomRange(-0.5, 0.5),
            randomRange(0.3, 0.9),
            randomRange(-0.5, 0.5)
        ));
        block.setDropItem(false);
        block.setHurtEntities(false);

        scheduler.runLater(block::remove, 80L);
    }

    private double randomRange(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

}
