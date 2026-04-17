package com.a3v1k.flightSchool.game;

import com.a3v1k.flightSchool.FlightSchool;
import com.a3v1k.flightSchool.blimp.BlimpData;
import com.a3v1k.flightSchool.player.GamePlayer;
import com.a3v1k.flightSchool.player.Role;
import com.a3v1k.flightSchool.team.Team;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class GameManager {

    private final FlightSchool plugin;
    public GameRuntime runtime;
    private final Map<Role, Integer> roleLimits = new HashMap<>();
    private Clipboard blimpClipboard;
    private AirspaceManager airspaceManager;
    private static final Material[] DEBRIS = {
            Material.WHITE_WOOL,
            Material.STONE,
            Material.IRON_BLOCK,
            Material.OAK_PLANKS,
            Material.CHAIN
    };



    public GameManager() {
        this.plugin = FlightSchool.getInstance();
        this.runtime = new GameRuntime();
        roleLimits.put(Role.CANNON_OPERATOR, 2);
        roleLimits.put(Role.PLANE_PILOT, 3);

        try {
            loadBlimpSchematic();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void computeBlimpData(World world, Location center) {
        double radius = 750.0;

        for (int i = 0; i < 8; i++) {
            Object[][] teams = {
                    {"red", BlockTypes.RED_CONCRETE},
                    {"yellow", BlockTypes.YELLOW_CONCRETE},
                    {"green", BlockTypes.LIME_CONCRETE}, // "Green" usually implies Lime in MC for brightness
                    {"blue", BlockTypes.LIGHT_BLUE_CONCRETE}, // "Blue/Teal"
                    {"dark_violet", BlockTypes.MAGENTA_CONCRETE}, // "Fuchsia"
                    {"violet", BlockTypes.PURPLE_CONCRETE},
                    {"dark_blue", BlockTypes.BLUE_CONCRETE},
                    {"orange", BlockTypes.ORANGE_CONCRETE}
            };
            Team team = getTeam(teams[i][0].toString());

            double angleDeg = i * 45;
            double angleRad = Math.toRadians(angleDeg);

            int x = (int) (center.getX() + radius * Math.cos(angleRad));
            int z = (int) (center.getZ() + radius * Math.sin(angleRad));
            int y = center.getBlockY();

            BlockVector3 pastePos = BlockVector3.at(x, y, z);

            AffineTransform transform = new AffineTransform().rotateY(-angleDeg);

            List<Location> solidBlocks = extractSolidBlocks(
                    blimpClipboard,
                    transform,
                    pastePos,
                    world
            );

            registerBlimp(team.getName(), solidBlocks);
        }
    }

    public void explodeBlimp(BlimpData blimp, Team team) {
        World world = blimp.getSegment(0).getFirst().getWorld();
        Color color = team.getColor();
        Random random = new Random();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick > 25*20) {
                    cancel();
                    return;
                }

                int phase;
                if (tick < 40) phase = 2;          // center
                else if (tick < 80) phase = tick % 2 == 0 ? 1 : 3;
                else phase = tick % 2 == 0 ? 0 : 5;

                List<Location> pool = blimp.getSegment(phase);
                if (pool.isEmpty()) return;

                int explosions = phase == 2 ? 24 : 14;

                for (int i = 0; i < explosions; i++) {
                    Location loc = pool.get(random.nextInt(pool.size()));

                    world.spawnParticle(Particle.EXPLOSION, loc, 1);
                    world.spawnParticle(
                            Particle.LARGE_SMOKE,
                            loc,
                            10,
                            1.5, 1.5, 1.5
                    );

                    world.spawnParticle(
                            Particle.DUST,
                            loc,
                            30,
                            1.8, 1.8, 1.8,
                            new Particle.DustOptions(color, 2.2f)
                    );

                    spawnDebris(loc, phase);

                }

                if (tick % 12 == 0) {
                    world.playSound(
                            pool.get(0),
                            Sound.ENTITY_GENERIC_EXPLODE,
                            6f,
                            0.5f
                    );
                }

                tick += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void spawnDebris(Location loc, int segment) {
        Material mat = DEBRIS[segment % DEBRIS.length];

        FallingBlock block = loc.getWorld().spawn(
                loc.clone().add(0, 0.5, 0),
                FallingBlock.class
        );

        block.setBlockData(mat.createBlockData());

        org.bukkit.util.Vector v = new org.bukkit.util.Vector(
                random(-0.5, 0.5),
                random(0.3, 0.9),
                random(-0.5, 0.5)
        );

        block.setVelocity(v);
        block.setDropItem(false);
        block.setHurtEntities(false);

        Bukkit.getScheduler().runTaskLater(plugin, block::remove, 80L);
    }

    private double random(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private List<Location> extractSolidBlocks(Clipboard clipboard, Transform transform, BlockVector3 pastePos, World world) {
        List<Location> result = new ArrayList<>();

        for (BlockVector3 pt : clipboard.getRegion()) {
            if (clipboard.getBlock(pt).getBlockType() == BlockTypes.AIR) continue;

            Vector3 transformed = transform.apply(pt.toVector3());

            Location loc = new Location(
                    world,
                    pastePos.x() + transformed.x(),
                    pastePos.y() + transformed.y(),
                    pastePos.z() + transformed.z()
            );

            result.add(loc);
        }

        return result;
    }

    public void loadBlimpSchematic() throws IOException {
        File schemFile = new File(plugin.getDataFolder(), "blimp.schem");
        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
            blimpClipboard = reader.read();
        }
    }

    public void startGame(List<Player> playerList) {
        setGameState(GameState.IN_GAME);
        Map<String, List<Location>> cannonLocations = this.plugin.getConfigManager().getCannonLocations(); // Team: Location mappings
        Map<String, List<Location>> planeLocations = this.plugin.getConfigManager().getPlaneLocations();
        this.runtime.setHealthManagers(new HashMap<>());
        this.runtime.setTeamPlaneMaps(new HashMap<>());
        World world = cannonLocations.values().stream().toList().getFirst().getFirst().getWorld();

        computeBlimpData(world, new Location(world, 0, 65, 0));

        for(Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "The game has started. Good luck!");
            player.getInventory().clear();
        }

        //set the worldborder
        world.getWorldBorder().setCenter(world.getSpawnLocation());
        world.getWorldBorder().setSize(2048);
        int minFlightY = this.plugin.getConfigManager().getMinFlightY(world, planeLocations);
        int maxFlightY = this.plugin.getConfigManager().getMaxFlightY(world, planeLocations);
        if (minFlightY >= maxFlightY) {
            minFlightY = maxFlightY - 32;
        }

        if (airspaceManager != null) {
            airspaceManager.shutdown();
        }

        airspaceManager = new AirspaceManager(this.plugin, this, minFlightY, maxFlightY);
        airspaceManager.runTaskTimer(this.plugin, 0L, 1L);

        Component message = Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                        .append(Component.newline())
                        .append(Component.text("You are a cannon!", NamedTextColor.GREEN))
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("Protect your blimp against other planes! Use [SPACE] to shoot!", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));


        for(Map.Entry<String, List<Location>> cannonMap : cannonLocations.entrySet()) {
            Team team = this.plugin.getGameManager().getTeam(cannonMap.getKey());
            List<Player> players = team.getCannonMembers();

            players.forEach(p -> p.getInventory().clear());

            // Spawn and then send players to sit.
            List<ActiveMob> activeMobs = new ArrayList<>();
            int index = 0;
            for(Location location : cannonMap.getValue()) {
                MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("flightschool_turret_" + cannonMap.getKey().toLowerCase()).orElse(null);
                if(mob != null) {
                    // spawns mob
                    ActiveMob knight = mob.spawn(BukkitAdapter.adapt(location), 1);

                    if (players.size() <= index) {
                        knight.despawn();
                        continue;
                    }
                    activeMobs.add(knight);

                    Player player = players.get(index);
//                    knight.setOwner(player.getUniqueId());
                    NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

                    knight.getEntity().getBukkitEntity()
                            .getPersistentDataContainer()
                            .set(key, PersistentDataType.STRING, player.getUniqueId().toString());
                    player.teleport(location);
                    player.sendMessage(message);
                    player.setGameMode(GameMode.ADVENTURE);

                    // Set a light source
                    location.add(2, -3, 2).getBlock().setType(Material.LIGHT);

                    index += 1;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (knight.isDead() || !player.isOnline()) return;

                            // Now that the 'seat' bone exists, this signal will work
                            knight.signalMob(BukkitAdapter.adapt(player), "mountCannon");
                        }
                    }.runTaskLater(this.plugin, 10L); // Delay 10 ticks (0.5 seconds)
                }
            }
            BlimpHealthManager blimpHealthManager = new BlimpHealthManager(activeMobs);
            blimpHealthManager.runTaskTimer(this.plugin, 0, 1);
            runtime.getHealthManagers().put(team.getName(), blimpHealthManager);
        }

        Component message1 = Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN)
                        .append(Component.newline())
                        .append(Component.text("You are a plane!", NamedTextColor.GREEN))
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("Shoot down other planes and protect your blimp! Use [SPACE] to shoot!", NamedTextColor.GOLD))
                        .append(Component.newline())
                        .append(Component.text("════════════════════════════════", NamedTextColor.DARK_GREEN));

        // Do planes now.
        for (Map.Entry<String, List<Location>> planeMap : planeLocations.entrySet()) {
            Team team = this.plugin.getGameManager().getTeam(planeMap.getKey());
            List<Player> players = team.getPlaneMembers();

            players.forEach(p -> p.getInventory().clear());

            // Spawn and then send players to sit.
            this.runtime.getTeamPlaneMaps().put(team, new ArrayList<>());
            int index = 0;
            for (Location location : planeMap.getValue()) {
                MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("plane_" + team.getName()).orElse(null);

                if (mob != null) {
                    // spawns mob
                    ActiveMob knight = mob.spawn(BukkitAdapter.adapt(location), 1);


                    if (players.size() <= index) {
                        knight.despawn();
                        continue;
                    }

                    Player player = players.get(index);
                    registerActivePlane(team, player, knight);
//                    knight.setOwner(player.getUniqueId());
                    NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");

                    knight.getEntity().getBukkitEntity()
                            .getPersistentDataContainer()
                            .set(key, PersistentDataType.STRING, player.getUniqueId().toString());
                    player.teleport(location);
                    player.sendMessage(message1);
                    player.setGameMode(GameMode.ADVENTURE);
                    this.plugin.getKillcamManager().startRecording(player);
                    index += 1;

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (knight.isDead() || !player.isOnline()) return;

                            // Now that the 'seat' bone exists, this signal will work
                            knight.signalMob(BukkitAdapter.adapt(player), "mountPlane");
                            knight.getEntity().getBukkitEntity().addPassenger(player);
                        }
                    }.runTaskLater(this.plugin, 10L); // Delay 10 ticks (0.5 seconds)
                }
            }
        }
    }

    public Map<String, BlimpHealthManager> getHealthManager() {
        return this.runtime.getHealthManagers();
    }

    public Map<Team, List<ActiveMob>> getTeamPlanes() {
        return this.runtime.getTeamPlaneMaps();
    }

    private int getAliveBlimps() {
        int aliveBlimps = 0;
        for(Team team : this.getTeams().values()) {
            if(!team.getBlimpDestroyed()) {
                aliveBlimps++;
            }
        }
        return aliveBlimps;
    }

    public List<Player> getPlanePlayers(Team team) {
        return this.runtime.getPlayerPlaneMaps().get(team);
    }

    public void setGameState(GameState gameState) {
        this.runtime.setGameState(gameState);
        this.plugin.getLogger().info("Gamestate set to: " + gameState.toString());
    }

    public void addPlayer(Player player) {
        this.runtime.addPlayer(player);
    }

    public void removePlayer(Player player) {
        this.runtime.removePlayer(player);
    }

    public GamePlayer getGamePlayer(Player player) {
        return this.runtime.getGamePlayer(player);
    }

    public void addTeam(Team team) {
        this.runtime.addTeam(team);
    }

    public Team getTeam(String name) {
        return this.runtime.getTeam(name);
    }

    public int getRoleLimit(Role role) {
        return roleLimits.getOrDefault(role, Integer.valueOf(0));
    }

    public boolean canAssignRole(Team team, Role role) {
        long count = runtime.getPlayers().values().stream()
                .filter(p -> p.getTeam() == team && p.getRole() == role)
                .count();
        return count < getRoleLimit(role);
    }

    public void assignPlayerToTeam(Player player, Team team) {
        if (player == null) {
            return;
        }

        for (Team existingTeam : runtime.getTeams().values()) {
            existingTeam.removeMember(player);
        }

        GamePlayer gamePlayer = getGamePlayer(player);
        if (gamePlayer == null) {
            return;
        }

        gamePlayer.setTeam(team);

        if (team != null) {
            team.addMember(player);
        }
    }

    public void assignRole(Player player, Role role) {
        this.runtime.assignRole(player, role);
    }

    public void registerBlimp(String teamName, List<Location> solidBlocks) {
        this.runtime.registerBlimp(teamName, solidBlocks);
    }

    public BlimpData getBlimp(String teamName) {
        return this.runtime.getBlimp(teamName);
    }

    public boolean hasBlimp(String teamName) {
        return this.runtime.hasBlimp(teamName);
    }

    public long getRoleCount(Team team, Role role) {
        return this.runtime.getRoleCount(team, role);
    }

    private void spawnPlane(String teamName, Location location, Player player) {
        MythicMob mob = MythicBukkit.inst().getMobManager().getMythicMob("flightschool_plane_" + teamName.toLowerCase()).orElse(null);
        if (mob != null) {
            // spawns mob
            ActiveMob knight = mob.spawn(BukkitAdapter.adapt(location), 1);
            Team team = getTeam(teamName);
            if (team != null) {
                registerActivePlane(team, player, knight);
            }

            knight.getEntity().getBukkitEntity().setCustomName(player.getUniqueId().toString());
            NamespacedKey key = new NamespacedKey(plugin, "owner_uuid");
            knight.getEntity().getBukkitEntity()
                    .getPersistentDataContainer()
                    .set(key, PersistentDataType.STRING, player.getUniqueId().toString());
            player.teleport(location);
            player.setGameMode(GameMode.ADVENTURE);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (knight.isDead() || !player.isOnline()) return;

                    // Now that the 'seat' bone exists, this signal will work
                    knight.signalMob(BukkitAdapter.adapt(player), "mountPlane");
                }
            }.runTaskLater(this.plugin, 10L); // Delay 10 ticks (0.5 seconds)
        }
    }

    private void registerActivePlane(Team team, Player player, ActiveMob planeMob) {
        List<ActiveMob> activePlanes = runtime.getTeamPlaneMaps().computeIfAbsent(team, key -> new ArrayList<>());
        int planeIndex = team.getPlaneMembers().indexOf(player);

        if (planeIndex < 0) {
            activePlanes.add(planeMob);
            return;
        }

        while (activePlanes.size() <= planeIndex) {
            activePlanes.add(null);
        }

        activePlanes.set(planeIndex, planeMob);
    }

    public void spawnDelayedPlane(String teamName, Location location, Player player, int delay) {
        Team team = getTeam(teamName);
        GamePlayer gamePlayer = getGamePlayer(player);
        if (team != null && team.getBlimpDestroyed() && !gamePlayer.isLastStand()) {
            player.showTitle(Title.title(
                    Component.text("Your blimp is destroyed!"),
                    Component.text("You cannot respawn."),
                    Title.Times.times(
                            Duration.ofMillis(500),
                            Duration.ofMillis(3500),
                            Duration.ofMillis(1000)
                    )
            ));
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if(team != null && team.getBlimpDestroyed() && gamePlayer.isLastStand()) {
                    gamePlayer.setLastStand(false);
                    gamePlayer.setEliminated(true);
                }

                spawnPlane(teamName, location, player);
            }
        }.runTaskLater(this.plugin, delay* 20L);
    }

    public GameRuntime getRuntime() {
        return this.runtime;
    }

    public GameState getGameState() {
        return this.runtime.getGameState();
    }

    public Map<UUID, GamePlayer> getPlayers() {
        return this.runtime.getPlayers();
    }

    public Map<String, Team> getTeams() {
        return this.runtime.getTeams();
    }

    public ScoreManager getScoreManager() {
        return this.runtime.getScoreManager();
    }

    public long getGameStartedAt() {
        return this.runtime.getGameStartedAt();
    }

    public void setGameStartedAt(long gameStartedAt) {
        this.runtime.setGameStartedAt(gameStartedAt);
    }

    public void resetRoundState() {
        if (airspaceManager != null) {
            airspaceManager.shutdown();
            airspaceManager = null;
        }

        Bukkit.getScheduler().cancelTasks(plugin);
        plugin.getKillcamManager().reset();

        for (BlimpHealthManager healthManager : runtime.getHealthManagers().values()) {
            if (healthManager != null) {
                healthManager.disableAndDespawn();
            }
        }

        for (List<ActiveMob> activeMobs : runtime.getTeamPlaneMaps().values()) {
            despawnActiveMobs(activeMobs);
        }

        List<Team> teams = new ArrayList<>(runtime.getTeams().values());
        for (Team team : teams) {
            team.resetRoundState();
        }

        GameRuntime freshRuntime = new GameRuntime();
        for (Team team : teams) {
            freshRuntime.addTeam(team);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            freshRuntime.addPlayer(player);
        }

        freshRuntime.setGameState(GameState.LOBBY);
        freshRuntime.setGameStartedAt(-1L);
        runtime = freshRuntime;
    }

    private void despawnActiveMobs(List<ActiveMob> activeMobs) {
        if (activeMobs == null) {
            return;
        }

        for (ActiveMob activeMob : activeMobs) {
            if (activeMob == null || activeMob.getEntity() == null) {
                continue;
            }

            activeMob.despawn();
        }
    }

    public boolean pasteMap(Location location, int teamCount) {
        // 1. Definition of Teams
        Object[][] allTeams = {
                {"Red", BlockTypes.RED_CONCRETE},
                {"Yellow", BlockTypes.YELLOW_CONCRETE},
                {"Green", BlockTypes.LIME_CONCRETE},
                {"Blue", BlockTypes.LIGHT_BLUE_CONCRETE},
                {"Dark Violet", BlockTypes.MAGENTA_CONCRETE},
                {"Violet", BlockTypes.PURPLE_CONCRETE},
                {"Dark Blue", BlockTypes.BLUE_CONCRETE},
                {"Orange", BlockTypes.ORANGE_CONCRETE}
        };

        // Safety check for team count
        if (teamCount > allTeams.length) teamCount = allTeams.length;
        if (teamCount < 1) return false;

        // 2. Load Schematic
        File schemFile = new File(plugin.getDataFolder(), "blimp.schem");
        if (!schemFile.exists()) {
            return true;
        }

        Clipboard originalClipboard;
        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schemFile))) {
            originalClipboard = reader.read();
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        // ---------------------------------------------------
        // CONFIGURATION & MATH
        // ---------------------------------------------------

        // Scale Factor based on your command: x = x / 0.65
        // This means the object gets LARGER.
        double scaleVal = 0.65;

        // Calculate Polygon Math
        // We want to keep the gap between blimps consistent regardless of count.
        // Based on your previous code: 8 blimps had a radius of 200.
        // Side Length (Chord) s = 2 * R * sin(pi/n)
        // s = 2 * 200 * sin(pi/8) ≈ 153.07 blocks (Distance between blimp centers)
        double targetSideLength = 153.0;

        // New Radius formula: R = s / (2 * sin(pi/n))
        double radius = targetSideLength / (2.0 * Math.sin(Math.PI / teamCount));

        // Handle edge case for 1 or 2 blimps where radius math gets weird
        if (teamCount == 1) radius = 0;
        if (teamCount == 2) radius = 100; // Linear separation

        BlockVector3 center = com.sk89q.worldedit.bukkit.BukkitAdapter.asBlockVector(location);


        try (EditSession editSession = WorldEdit.getInstance().newEditSession(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()))) {
            for (int i = 0; i < teamCount; i++) {

                String teamName = (String) allTeams[i][0];
                BlockType teamBlock = (BlockType) allTeams[i][1];

                // Calculate Angle
                double angleDeg = i * (360.0 / teamCount);
                double angleRad = Math.toRadians(angleDeg);

                int x = (int) (center.x() + radius * Math.cos(angleRad));
                int z = (int) (center.z() + radius * Math.sin(angleRad));
                int y = center.y();

                BlockVector3 pastePos = BlockVector3.at(x, y, z);

                // ---------------------------------------------------
                // 4. Color Replacement (On Original Scale)
                // ---------------------------------------------------
                Clipboard teamClipboard = new BlockArrayClipboard(originalClipboard.getRegion());
                teamClipboard.setOrigin(originalClipboard.getOrigin());

                // Copy blocks
                for (BlockVector3 pt : originalClipboard.getRegion()) {
                    try {
                        teamClipboard.setBlock(pt, originalClipboard.getBlock(pt));
                    } catch (WorldEditException e) { e.printStackTrace(); }
                }

                // Replace STONE with specific Team Block
                for (BlockVector3 pt : teamClipboard.getRegion()) {
                    if (teamClipboard.getBlock(pt).getBlockType() == BlockTypes.DIORITE) {
                        try {
                            teamClipboard.setBlock(pt, teamBlock.getDefaultState());
                        } catch (WorldEditException e) { e.printStackTrace(); }
                    }
                }

                // ---------------------------------------------------
                // 5. Apply Transforms (Scale & Rotate)
                // ---------------------------------------------------
                ClipboardHolder holder = new ClipboardHolder(teamClipboard);

                // 1. Scale (Deform)
                AffineTransform scaleTransform = new AffineTransform().scale(scaleVal, scaleVal, scaleVal);

                // 2. Rotate (Face Center)
                // Note: We rotate negative angle to face inward/follow circle
                AffineTransform rotateTransform = new AffineTransform().rotateY(-angleDeg);

                // Combine: Scale FIRST, then Rotate
                holder.setTransform(holder.getTransform().combine(scaleTransform).combine(rotateTransform));

                try {
                    Operation operation = holder
                            .createPaste(editSession)
                            .to(pastePos)
                            .ignoreAirBlocks(true)
                            .build();

                    Operations.complete(operation);


                } catch (WorldEditException e) {
                    e.printStackTrace();
                }
            }

        }

        return true;
    }

}
