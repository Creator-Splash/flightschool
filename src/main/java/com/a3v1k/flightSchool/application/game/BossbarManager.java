package com.a3v1k.flightSchool.application.game;

import lombok.Getter;
import lombok.Setter;

public final class BossbarManager {

    private int currentTicks;

    @Setter @Getter
    private boolean paused;

    public BossbarManager(int totalTicks) {
        this.currentTicks = totalTicks;
        this.paused = false;
    }

    public void update() {
        if (isFinished() || paused) return;
        currentTicks--;
    }

    public boolean isFinished() {
        return currentTicks <= 0;
    }

    private String secondsToString(int time) {
        return String.format("%02d:%02d", time / 60, time % 60);
    }

    public String getTime() {
        return secondsToString(currentTicks / 20);
    }

}

