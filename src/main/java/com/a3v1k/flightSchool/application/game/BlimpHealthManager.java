package com.a3v1k.flightSchool.application.game;

public interface BlimpHealthManager {

    /** Tick hook — refreshes derived state and the floating health display. */
    void update();

    /** Returns the team's blimp health as a percentage in {@code [0, 100]}. */
    double getHealth();

    /** Returns the rounded-up integer health percentage. Convenience for display layers. */
    int getHealthPercentage();

    /**
     * Apply damage to the blimp's tracked health pool. Triggers the broken/last-stand
     * transition once health hits zero.
     */
    void damage(double damage);

    /**
     * Returns true once every turret originally registered for this blimp has been
     * destroyed (i.e. {@code team.destroyedBlimps >= initialTurretCount}).
     */
    boolean hasAllTurretsDestroyed();

    /** Disable the manager and despawn its turret mobs and floating display, if any. */
    void disableAndDespawn();

}
