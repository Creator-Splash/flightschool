package com.a3v1k.flightSchool.platform.paper.game.blimp;

import com.a3v1k.flightSchool.application.game.BlimpHealthManager;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

import java.util.List;

public final class PaperBlimpHealthManager implements BlimpHealthManager {

    private final List<ActiveMob> activeMobs;

    public PaperBlimpHealthManager(List<ActiveMob> activeMobs) {
        this.activeMobs = activeMobs;
    }

    @Override
    public void update() {
        // reserved for future health sync logic
    }

    @Override
    public double getHealth() {
        double netHealth = 0.0;
        double maxHealth = 0.0;

        for (ActiveMob activeMob : activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) continue;

            AttributeInstance healthAttr =
                activeMob.getEntity().getBukkitEntity() instanceof LivingEntity le
                    ? le.getAttribute(Attribute.MAX_HEALTH)
                    : null;

            if (healthAttr == null) continue;

            netHealth += activeMob.getEntity().getHealth();
            maxHealth += healthAttr.getValue();
        }

        double health = netHealth / maxHealth * 100;
        return Double.isNaN(health) ? 0 : health;
    }

    @Override
    public void disableAndDespawn() {
        for (ActiveMob activeMob : activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) continue;
            activeMob.despawn();
        }
    }
}
