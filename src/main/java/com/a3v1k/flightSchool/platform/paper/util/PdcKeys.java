package com.a3v1k.flightSchool.platform.paper.util;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;

@UtilityClass
public class PdcKeys {

    public final NamespacedKey OWNER_UUID = new NamespacedKey(
        FlightSchool.getInstance(), "owner_uuid");

}
