package com.a3v1k.flightSchool.platform.paper.util;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class PlayerUtil {

    public List<Player> toOnlinePlayers(Collection<UUID> uuids) {
        return uuids.stream()
            .map(Bukkit::getPlayer)
            .filter(p -> p != null && p.isOnline())
            .toList();
    }

}
