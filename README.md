# FlightSchool

A team-based aerial combat minigame for PaperMC 1.21.8+.

## Overview

FlightSchool is a multi-team minigame in which up to **8 teams of 5 players** battle to be the last airborne fleet standing. Each team controls a blimp defended by **2 cannon operators** and attacked by **3 plane pilots**. Pilots fly out from their team's base and try to shoot down enemy cannons; cannons stay mounted on their blimp and try to shoot down enemy planes. When a team's last cannon is destroyed, the team is eliminated. The last team standing wins. If the match timer expires before any team is eliminated, the team with the highest blimp health wins, with team kill count as the tiebreaker.

### Teams

The eight teams have fixed display names and colours:

| Team       | Colour       |
|------------|--------------|
| Orca       | Red          |
| Seahorse   | Yellow       |
| Turtle     | Green        |
| Dolphin    | Aqua         |
| Stingray   | Dark Purple  |
| Jellyfish  | Light Purple |
| Swordfish  | Blue         |
| Octopus    | Gold         |

Each team has 5 player slots: 2 cannon operators and 3 plane pilots.

---

## Commands

All commands are rooted at `/fsh`. Tab-complete shows team names as their display name (e.g. `Orca`, `Seahorse`), but the lookup also resolves internal colour names (`red`, `yellow`, etc.) for compatibility.

### Player commands

These have no permission requirement.

| Command | Description |
|---------|-------------|
| `/fsh playing-as <flight\|cannon>` | Pick your role during the role-selection phase. `flight` assigns Plane Pilot; `cannon` assigns Cannon Operator. Cannon roles are scarce (2 per team) so they fill first when role selection auto-assigns leftovers. |

### Admin commands

All admin commands require the permission `fsh.admin`.

| Command | Description |
|---------|-------------|
| `/fsh start` | Start a match. Only valid in the lobby state. Triggers the cinematic, then role selection, then the round itself. |
| `/fsh stop` | Stop the current match and return everyone to the lobby. |
| `/fsh set-cannon <team>` | **Player only.** Save your current location as a cannon spawn for the given team. Each team needs **2** cannon spawns configured. |
| `/fsh set-plane <team>` | **Player only.** Save your current location as a plane spawn for the given team. Each team needs **3** plane spawns configured. |
| `/fsh team <player> <team>` | Assign a player to a specific team. Useful for manual team setup before starting a match. |

### Debug commands (admin only)

The `/fsh debug …` subtree is for development and testing. All require `fsh.admin`.

| Command | Description |
|---------|-------------|
| `/fsh debug spawnplane <team> <player>` | Spawn a plane for the given team and player at the team's first plane spawn. |
| `/fsh debug cannons` | Spawn a single test turret near you and signal it to mount. |
| `/fsh debug planes` | Spawn a single test plane near you and signal it to mount. |
| `/fsh debug pastemap` | Paste the bundled blimp map schematic at your current location. |
| `/fsh debug tabicon [team]` | Apply the TAB plugin's team prefix and custom name styling to yourself. Defaults to your own team if no argument is given. Requires the TAB plugin. |
| `/fsh debug tabreset` | Reset your TAB plugin styling. Requires the TAB plugin. |

---

## Configuration

The plugin reads from `plugins/FlightSchool/config.yml`. All scalar keys auto-seed with their default value on first read, so you do not need to create the file by hand.

| Key | Default | Description |
|---|---|---|
| `match.duration-seconds` | `600` | Match length in seconds. When the timer expires, the match ends and a winner is determined. |
| `game.min-players` | `2` | Minimum online players required for a match to actually start. If the online count drops below this during the role-selection countdown, the round is cancelled and everyone is sent back to lobby. |
| `airspace.min-flight-y` | dynamic | Minimum Y coordinate planes are allowed to descend to. The default is computed from your configured plane spawn locations (lowest spawn Y minus 96, clamped to the world's minimum height). |
| `airspace.max-flight-y` | dynamic | Maximum Y coordinate planes are allowed to climb to. The default is computed from your configured plane spawn locations (highest spawn Y plus 160, clamped to the world's maximum height). |
| `cannons.<team>.<uuid>` | — | Cannon spawn locations. Managed automatically by `/fsh set-cannon`. Do not edit by hand. |
| `planes.<team>.<uuid>` | — | Plane spawn locations. Managed automatically by `/fsh set-plane`. Do not edit by hand. |
| `event-mode.proxy-lobby-server` | `creatorsplash` | When the plugin runs in CreatorSplash event mode, this is the proxy lobby server that players are evacuated to after a round ends. Only relevant in event-mode integrations. Does not auto-seed — add the line manually if you need a different value. |

---

## Setup guide

### 1. Install dependencies

Drop the following plugins into your `plugins/` folder before installing FlightSchool:

**Required:**
- CreatorSplashCore
- MythicMobs
- ModelEngine
- WorldEdit
- WorldGuard
- LuckPerms

**Optional:**
- PlaceholderAPI (used by the in-game scoreboard placeholders)
- ItemsAdder (used by the TAB integration)
- TAB (used for in-game name styling)
- AdvancedReplay (used for plane-death killcams)

### 2. Install the plugin

Drop the FlightSchool jar into `plugins/`, then start the server. The plugin will create `plugins/FlightSchool/config.yml` on first launch.

### 3. Set up the game world

The plugin expects a world named exactly `game-world`. Players will be teleported into it during role selection.

Inside `game-world`, create one WorldGuard region per team using these exact names:

| Team       | Region name        |
|------------|--------------------|
| Orca       | `red_spawn`        |
| Seahorse   | `yellow_spawn`     |
| Turtle     | `green_spawn`      |
| Dolphin    | `blue_spawn`       |
| Stingray   | `darkviolet_spawn` |
| Jellyfish  | `violet_spawn`     |
| Swordfish  | `darkblue_spawn`   |
| Octopus    | `orange_spawn`     |

Each region is the area where members of that team will be teleported and stand around during role selection. Make them small, flat platforms.

### 4. Configure cannon and plane spawn locations

You'll need to be in-game and have the `fsh.admin` permission for this step.

For each team you want to be playable, set **2 cannon spawn points** and **3 plane spawn points**:

```
# Stand where you want the team's first cannon mob to spawn:
/fsh set-cannon Orca

# Stand where you want the team's second cannon mob to spawn:
/fsh set-cannon Orca

# Repeat for the team's three plane spawn points:
/fsh set-plane Orca
/fsh set-plane Orca
/fsh set-plane Orca
```

Repeat for every team. The plugin saves the locations under canonical internal names in `config.yml` automatically — you can type either `Orca` or `red`, both resolve to the same team.

### 5. Verify airspace bounds (optional)

After you've set plane spawns, the plugin will auto-fill `airspace.min-flight-y` and `airspace.max-flight-y` based on those spawn heights. If you want different bounds, edit `config.yml` and set explicit values.

### 6. Adjust match duration and player floor (optional)

Open `config.yml` and edit `match.duration-seconds` and `game.min-players` if the defaults don't suit your event.

### 7. Test a round

With at least `game.min-players` players online, run `/fsh start`. The cinematic plays, role selection runs for 20 seconds, then the match begins.

---

## Game flow

### LOBBY
The starting state. Players are in adventure mode at the lobby spawn. Run `/fsh start` to begin.

### CINEMATIC
A short intro plays for all online players. Cannons and planes haven't been spawned yet. The cinematic ends automatically and rolls into role selection.

### ROLE_SELECTION
Players are teleported to their team's WorldGuard spawn region. A 20-second countdown begins. During this window:

- Each player gets a "Pick your team" compass — selecting a role uses `/fsh playing-as <flight|cannon>`.
- The server checks that `game.min-players` is still met every tick. If the online count drops below the minimum, the round is cancelled, a red message is broadcast, and players are sent back to lobby with their team assignments preserved.

When the countdown ends, any player who didn't pick a role is auto-assigned (cannons fill first, then planes). The match starts.

### IN_GAME
- Cannon mobs spawn at the configured cannon spawn locations; cannon operators are seated on them.
- Plane mobs spawn at the configured plane spawn locations; plane pilots are seated on them.
- A floating health bar appears above each team's blimp. It depletes as cannons take damage.
- The match-end timer (configured via `match.duration-seconds`) starts ticking.

#### During the match
- **Plane death**: when a pilot's plane crashes, a killcam plays. If the team's cannons are still alive, the pilot respawns at their plane spawn location and re-mounts. If all the team's cannons have already been destroyed, the respawn is blocked and the pilot is permanently eliminated.
- **Cannon death**: when a cannon mob is destroyed, its operator is moved to spectator immediately and cannot return for the rest of the round. After the second cannon falls, the entire team is eliminated: every team member is moved to spectator, every team plane is despawned, the blimp explodes with particle effects, and an "Eliminated" title is shown to the team.
- **Pilots cannot dismount**: while in adventure mode and seated in their plane, sneaking is suppressed and explicit dismount events are cancelled. The only way out of a plane is to die or have the round end.

#### How a match ends
The match ends as soon as **any one** of the following triggers fires (all converge on the same end-of-match flow):

1. The match timer (`match.duration-seconds`) expires.
2. Only one team has any non-eliminated player remaining (last team standing).
3. Only one team has any alive turret remaining (turret-based fallback for edge cases).

When triggered:
- The winner is determined: a single surviving team wins outright; otherwise the survivor with the highest blimp HP wins, with team score as the tiebreaker.
- A "WINNER" title is shown to all online players for ~5 seconds, with the winning team's display name in their team colour.
- All players are teleported to the lobby spawn (so anyone still seated on a plane or cannon doesn't drop out of the sky).
- Round state resets: managers stop, mobs despawn, scores reset, but team assignments are preserved so a follow-up `/fsh start` keeps everyone on the same team.

### Back to LOBBY
The plugin returns to the lobby state and is ready for another `/fsh start`.

---

## Permissions

| Node | Granted to | Purpose |
|------|------------|---------|
| `fsh.admin` | Admins | All `/fsh` admin and debug commands. |
| (none) | Everyone | `/fsh playing-as` is open to all players. |
