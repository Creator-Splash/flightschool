package com.a3v1k.flightSchool.blimp;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class BlimpData {

    @Getter
    private final String teamName;

    private final List<List<Location>> segments;

    public BlimpData(String teamName, List<Location> solidBlocks) {
        this.teamName = teamName;

        // Compute principal axis
        Location min = solidBlocks.get(0).clone();
        Location max = solidBlocks.get(0).clone();

        for (Location l : solidBlocks) {
            min.setX(Math.min(min.getX(), l.getX()));
            min.setY(Math.min(min.getY(), l.getY()));
            min.setZ(Math.min(min.getZ(), l.getZ()));
            max.setX(Math.max(max.getX(), l.getX()));
            max.setY(Math.max(max.getY(), l.getY()));
            max.setZ(Math.max(max.getZ(), l.getZ()));
        }

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

    public String getTeamName() {
        return teamName;
    }
}


