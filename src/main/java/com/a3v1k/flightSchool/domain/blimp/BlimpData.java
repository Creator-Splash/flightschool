package com.a3v1k.flightSchool.domain.blimp;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BlimpData {

    private final String teamName;

    private final List<List<Location>> segments;
    private final Location center;

    public BlimpData(String teamName, List<Location> solidBlocks) {
        this.teamName = teamName;

        // Compute principal axis
        Location min = solidBlocks.getFirst().clone();
        Location max = solidBlocks.getFirst().clone();

        for (Location l : solidBlocks) {
            min.setX(Math.min(min.getX(), l.getX()));
            min.setY(Math.min(min.getY(), l.getY()));
            min.setZ(Math.min(min.getZ(), l.getZ()));
            max.setX(Math.max(max.getX(), l.getX()));
            max.setY(Math.max(max.getY(), l.getY()));
            max.setZ(Math.max(max.getZ(), l.getZ()));
        }

        this.center = new Location(
                min.getWorld(),
                (min.getX() + max.getX()) / 2.0,
                (min.getY() + max.getY()) / 2.0,
                (min.getZ() + max.getZ()) / 2.0
        );

        Vector axis = max.toVector().subtract(min.toVector()).normalize();

        // Split into 6 longitudinal segments
        segments = new ArrayList<>();
        for (int i = 0; i < 6; i++) segments.add(new ArrayList<>());

        double length = max.toVector().subtract(min.toVector()).length();

        for (Location l : solidBlocks) {
            double t = l.toVector()
                    .subtract(min.toVector())
                    .dot(axis) / length;

            int idx = Math.min(5, Math.max(0, (int) (t * 6)));
            segments.get(idx).add(l);
        }
    }

    public List<Location> getSegment(int i) {
        return segments.get(i);
    }

    public int segmentCount() {
        return segments.size();
    }

}


