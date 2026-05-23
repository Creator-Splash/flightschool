package com.a3v1k.flightSchool.platform.paper.integration;

import com.a3v1k.flightSchool.platform.paper.FlightSchool;
import creatorsplash.creatorsplashcore.api.EndCondition;
import creatorsplash.creatorsplashcore.api.EvacuationPolicy;
import creatorsplash.creatorsplashcore.api.GameAdapter;
import creatorsplash.creatorsplashcore.api.GameRequirements;
import creatorsplash.creatorsplashcore.api.OnEndContinuation;
import creatorsplash.creatorsplashcore.api.TeamMode;
import creatorsplash.creatorsplashcore.api.runtime.GameRuntime;
import creatorsplash.creatorsplashcore.api.runtime.RuntimeServices;
import creatorsplash.creatorsplashcore.event.GameContext;

import java.time.Duration;

public final class FlightSchoolGameAdapter implements GameAdapter {

    private final FlightSchool plugin;

    private volatile FlightSchoolRuntime activeRuntime;

    public FlightSchoolGameAdapter(FlightSchool plugin) {
        this.plugin = plugin;
    }

    @Override
    public String gameId() {
        return "flightschool";
    }

    @Override
    public String displayName() {
        return "Flight School";
    }

    @Override
    public GameRequirements requirements() {
        int durationSeconds = Math.min(50 * 60,
                Math.max(60, plugin.getConfig().getInt("game.duration-seconds", 600)));
        int warmupSeconds = Math.max(0, plugin.getConfig().getInt("event-mode.warmup-seconds", 5));
        int readinessSeconds = Math.max(0, plugin.getConfig().getInt("event-mode.readiness-timeout-seconds", 0));
        int postEndSeconds = Math.max(5, Math.min(30,
                plugin.getConfig().getInt("event-mode.post-end-seconds", 5)));
        return GameRequirements.builder()
                .teamMode(TeamMode.TEAM_VS_TEAM)
                .teamRange(2, 8)
                .playersPerTeamRange(1, 5)
                .declareEndCondition(EndCondition.lastTeamStanding())
                .declareEndCondition(EndCondition.timeout(Duration.ofSeconds(durationSeconds)))
                .evacuationPolicy(EvacuationPolicy.AUTO_TO_LOBBY)
                .onEndContinuation(OnEndContinuation.PUBLISH_GAME_COMPLETE)
                .readinessTimeout(Duration.ofSeconds(readinessSeconds))
                .preGameWarmup(Duration.ofSeconds(warmupSeconds))
                .postEndDisplay(Duration.ofSeconds(postEndSeconds))
                .build();
    }

    @Override
    public GameRuntime createRuntime(GameContext ctx, RuntimeServices services) {
        FlightSchoolRuntime runtime = new FlightSchoolRuntime(plugin, this, ctx, services);
        this.activeRuntime = runtime;
        return runtime;
    }

    void clearActiveRuntime(FlightSchoolRuntime runtime) {
        if (this.activeRuntime == runtime) this.activeRuntime = null;
    }
}
