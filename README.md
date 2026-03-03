# FlightSchool

## Commands
# FlightSchool Plugin — Command Reference

## `/fsh start`
Starts the Flight School game. Only works when the game is in the `LOBBY` state. Transitions to the role selection phase.

---

## `/fsh stop`
Stops an ongoing game. Only works when the game is in the `IN_GAME` state.

---

## `/fsh set-cannon <team>`
**Player only.** Saves the player's current location as the cannon position for the specified team.

---

## `/fsh set-plane <team>`
**Player only.** Saves the player's current location as the plane spawn position for the specified team.

---

## `/fsh team <playerName> <teamName>`
**Player only.** Assigns a player to a specified team. Requires both the target player and the team to exist as active game participants.

---

## `/fsh playing-as <flight|cannon>`
**Player only.** Lets a player select their role during the `ROLE_SELECTION` phase.

| Argument | Assigned Role |
|----------|---------------|
| `flight` | `PLANE_PILOT` |
| `cannon` | `CANNON_OPERATOR` |

- Roles **cannot be changed** once selected.
- Each role has a **cap per team, variable depending on the role**.
