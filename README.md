# SaveAFK

[![License](https://img.shields.io/badge/license-LGPL--3.0-blue.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)
[![workflow](https://github.com/T3rr0or/safk/actions/workflows/gradle.yml/badge.svg)](https://github.com/T3rr0or/safk/actions/workflows/gradle.yml)

**SaveAFK** lets a player disconnect and leave a stand-in bot behind. Your farms keep ticking, your PC does not. Type `/safk`, shut the machine off, and pick up where you left off when you reconnect.

![InfoGraphic](Infographic_hd.png)

## Prerequisites & Installation
* **Mod Loader:** Fabric
* **Minecraft Version:** 1.19.2 up to 26.2

## Features
* **Go Green:** Turn off your PC while your player-bot continues to AFK for you.
* **Customizable Timeouts:** Set how long a bot stays up. The default is 129600 minutes (90 days).
* **Admin Control:** Server administrators have full command control to spawn, kick, or manage AFK bots.
* **Safety Options:** Reset health on death, disable damage for AFK bots, or hide them from other players and operators.
* **Server Restart:** Bots respawn after a server restart, with a short delay.

## Commands

### Player Commands
* **`/safk [<minutes>] [<reason>]`**: Disconnects you and leaves a bot standing in your place.
  * *Note: the single-player server owner cannot use this*.
* **`/afk [<minutes>] [<reason>]`**: Same thing under the name most players already know. Off by default, and it stays off if another AFK mod has claimed `/afk`.

### Admin Commands
Requires permission level 4 by default.
* **`/safk-admin`**: Displays information about the mod.
* **`/safk-admin save`**: Saves the current configuration.
* **`/safk-admin reload`**: Reloads the configuration, discarding unsaved changes.
* **`/safk-admin purge`**: Resyncs the tracked player/bot maps against the live server. It does not prune stored records — use `forget` for that.
* **`/safk-admin forget <player>`**: Drops a stored player record. Refused while that player is connected or AFK.
* **`/safk-admin spawn <player> [<minutes>] [<reason>]`**: Manually spawns an AFK bot for a player.
* **`/safk-admin kick <player>`**: Removes an active AFK bot.
* **`/safk-admin info [<player>]`**: Detailed debug information for a player. (`advancedAdminOptions` unlocks the full player info)
* **`/safk-admin list [players|bots|all]`**: Lists tracked players or active bots. (`advancedAdminOptions` unlocks the sub commands)
* **`/safk-admin set <setting> <value>`**: Sets a config value. (`advancedAdminOptions` unlocks this sub command, except for `advancedAdminOptions` itself, so you can always switch it back on)

`advancedAdminOptions` takes effect immediately — no restart needed.

## Configuration

Settings live in `config/safk.json`.

| Category     | Option                        | Description                                                                                               | Default  |
|:-------------|:------------------------------|:----------------------------------------------------------------------------------------------------------|:---------|
| **Main**     | `safkEnabled`                 | Toggles the entire AFK feature.                                                                           | `true`   |
| **Main**     | `debugMode`                   | Enables debugging output.                                                                                 | `false`  |
| **Main**     | `reducedListDebugInfo`        | Reduced output for the information commands.                                                              | `true`   |
| **Main**     | `advancedAdminOptions`        | Enables advanced admin options, such as `set`.                                                            | `false`  |
| **Safk**     | `defaultSafkTimeout`          | Default timeout in minutes. Ships at 90 days.                                                             | `129600` |
| **Safk**     | `maxSafkTimeout`              | Longest timeout a player may request, in minutes. `-1` removes the limit.                                 | `129600` |
| **Safk**     | `maxConcurrentBots`           | How many AFK bots may exist at once, server-wide. `-1` removes the limit.                                 | `-1`     |
| **Safk**     | `resetHealthUponDeath`        | Resets the bot's health when it is killed.                                                                | `false`  |
| **Safk**     | `safkDisableDamage`           | Prevents the bot from taking damage.                                                                      | `false`  |
| **Safk**     | `safkHidePlayer`              | Makes the bot invisible to others.                                                                        | `false`  |
| **Safk**     | `safkHideFromOps`             | Hides the bot from operators as well.                                                                     | `false`  |
| **Command**  | `safkCommandPermissions`      | Permission level required for `/safk`.                                                                    | `0`      |
| **Command**  | `safkAdminCommandPermissions` | Permission level for `/safk-admin`.                                                                       | `4`      |
| **Command**  | `afkCommandPermissions`       | Permission level required for `/afk`.                                                                     | `0`      |
| **Command**  | `enableSafkCommand`           | Enables the `/safk` command.                                                                              | `true`   |
| **Command**  | `enableAfkCommand`            | Enables the `/afk` command. (works the same as `/safk`)                                                   | `false`  |
| **Messages** | `broadcastMessages`           | Broadcasts AFK status messages.                                                                           | `false`  |
| **Messages** | `tabListPrefix`               | Marker put before an AFK bot's name in the player list. Empty turns it off.                 | `"§7[AFK] "` |
| **Messages** | `afkLastInTabList`            | Sorts AFK players to the bottom of the player list. Needs 1.21.2 or newer.                  | `true`   |
| **Messages** | `hideSafkJoin`                | Suppresses the default `player has joined` messages while bots are spawned, where possible.               | `false`  |
| **Messages** | `displayDuration`             | Shows the duration in AFK status messages.                                                                | `false`  |
| **Messages** | `displayReturnFeedback`       | Shows why an AFK session ended.                                                                           | `false`  |

## Integrations

SaveAFK publishes its state through [Placeholder API](https://modrinth.com/mod/placeholder-api) rather than styling names itself. A player only gets one scoreboard team, so a mod that claims it for an AFK marker ends up fighting whatever rank system the server runs. Handing the state out instead lets the mod that already owns nametags and the tab list compose `[AFK] ++ Name` from your real ranks.

| Placeholder | Value |
|:------------|:------|
| `%safk:marker%` | The AFK marker plus a trailing space, or empty when the player is not AFK |
| `%safk:is_afk%` | `true` or `false` |
| `%safk:duration%` | How long they have been AFK |
| `%safk:remaining%` | Time left before the session expires |
| `%safk:reason%` | The reason they gave |

`%safk:marker%` carries its own trailing space, because a format string cannot put a separator behind a value that is sometimes empty. Interpolate it unconditionally and non-AFK players stay clean.

Placeholder API is optional. Without it the integration is skipped and everything else still works.

### Tested with

[**TAB**](https://modrinth.com/plugin/tab-was-taken) handles the tab list and nametags together, and reads Placeholder API on Fabric. In `groups.yml`:

```yaml
_DEFAULT_:
  tabprefix: "%safk:marker%%luckperms-prefix%"
  tagprefix: "%safk:marker%%luckperms-prefix%"
```

To sink AFK players to the bottom of the tab list, put this first in `sorting-types` in `config.yml`, since `false` sorts before `true`:

```yaml
  sorting-types:
    - "PLACEHOLDER_A_TO_Z:%safk:is_afk%"
```

[**CustomNameTags**](https://modrinth.com/mod/customnametags) does nametags alone. Its nametag *replaces* the name rather than adding a line, so keep the player in the literal:

```json
{ "nametags": [ { "id": "safk:afk", "update_interval": 20,
                  "literal": "<gray>%safk:marker%</gray>%player:displayname_visual%" } ] }
```

Pin `1.5.0`; `1.5.1` fails to boot on a missing bundled Arcade module. It also needs `fabric-language-kotlin`.

[**Styled Player List**](https://modrinth.com/mod/styledplayerlist) covers the tab list on its own if you would rather not run TAB.

Set `tabListPrefix` to empty when one of these owns the tab list, or the marker appears twice.

### Per-player timeouts

Grant a permission node to give a player their own ceiling. A granted tier replaces `maxSafkTimeout` for that player, so it can lift a donor above the server cap or hold a newcomer below it. The highest granted tier wins, and a player with no node falls back to `maxSafkTimeout`.

| Node | Allowance |
|:-----|:----------|
| `safk.timeout.hour` | 60 minutes |
| `safk.timeout.day` | 1440 minutes |
| `safk.timeout.week` | 10080 minutes |
| `safk.timeout.month` | 43200 minutes |
| `safk.timeout.max` | whatever `maxSafkTimeout` is set to |

The tiers are named rather than numeric because a permission node can only be tested by name, never listed. Nodes are checked without an operator-level fallback, so a tier has to be granted deliberately instead of arriving with op.

### Limits

Two options bound what players can ask for. Set either to `-1` to remove that limit entirely.

`maxSafkTimeout` ships equal to `defaultSafkTimeout`, so out of the box it caps nothing — lower it to tighten. A player who names a longer time gets refused and told the maximum. When the *default* runs past the cap, SaveAFK trims the session instead of refusing, so a misconfigured default can never leave a bare `/safk` broken.

`maxConcurrentBots` ships off, because the right number depends on your slot count. Set it to a positive number and `/safk` is refused once that many bots are up.

Neither limit applies to `/safk-admin spawn` — operators can always place a bot.

**Messages & Formatting:**
Every broadcast message is configurable. The kick message a player sees on a successful `/safk` defaults to `"§6Your player will be AFK§r"`.
`duration` and `timeDate` are CoreLib time formatting options, used by the broadcast messages when `displayDuration` is on.

### Example Config
[CONFIG.md](CONFIG.md)

## Credits

SaveAFK is a fork of [Unplugged-AFK](https://github.com/sakura-ryoko/unplugged-afk) by Sakura-Ryoko, and stays under the same LGPL-3.0 license. It also depends on Sakura-Ryoko's [CoreLib](https://github.com/sakura-ryoko/corelib).
