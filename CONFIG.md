### Example Config:

Lives at `config/safk.json`. It is written on first server start, so run the server once before editing it.

```json
{
    "___comment": "SaveAFK-Development Version-0.1.0 Config",
    "config_date": "Sun, 2 Aug 2026 20:09:55 -0400",
    "last_start": 1785715811712,
    "last_stop": 1785715815367,
    "main": {
        "safkEnabled": true,
        "debugMode": false,
        "reducedListDebugInfo": true,
        "advancedAdminOptions": false
    },
    "commands": {
        "safkCommandPermissions": 0,
        "safkAdminCommandPermissions": 4,
        "afkCommandPermissions": 0,
        "enableSafkCommand": true,
        "enableAfkCommand": false
    },
    "safk": {
        "defaultSafkTimeout": 129600,
        "maxSafkTimeout": 129600,
        "maxConcurrentBots": -1,
        "resetHealthUponDeath": false,
        "safkDisableDamage": false,
        "safkHidePlayer": false,
        "safkHideFromOps": false
    },
    "messages": {
        "broadcastMessages": false,
        "afkLastInTabList": true,
        "tabListPrefix": "§7[AFK] ",
        "hideSafkJoin": false,
        "displayDuration": false,
        "displayReturnFeedback": false,
        "defaultSafkReason": "",
        "safkPlayerPrefix": "§e",
        "safkPlayerSuffix": "§r",
        "safkKickMessage": "§6Your player will be AFK§r",
        "safkExpiredReason": "§eTimeout expired§r",
        "safkStarted": " §eis now AFK§r",
        "safkPunctuation": "§e,§r ",
        "safkReplaced": "§6Replaced by player§r",
        "safkTerminated": "§cAFK session terminated§r",
        "safkUnsuccessful": "§eYour AFK session was interrupted§r",
        "safkUnsuccessfulPrefix": " §eafter:§a ",
        "safkUnsuccessfulPunctuation": "\n §7- For:§r ",
        "safkSuccessful": "§eYour Session was successful.§r",
        "safkSuccessfulPrefix": "§eYour §a",
        "safkSuccessfulSuffix": " §eSession was successful.§r",
        "safkSuccessfulPunctuation": "\n §7- For:§r ",
        "whenSafkReturned": " §ehas returned§r",
        "whenSafkExpired": " §eAFK session expired§r",
        "whenSafkInterrupted": " §eAFK session interrupted§r",
        "whenSafkTerminated": " §eAFK session terminated§r",
        "whenSafkDurationPrefix": " §6for: §a",
        "whenSafkDurationSuffix": "§7 minutes)",
        "whenReturnDurationPrefix": " §7(Gone for: §a",
        "whenReturnDurationSuffix": "§7)§r",
        "duration": {
            "option": "PRETTY",
            "customFormat": ""
        },
        "timeDate": {
            "option": "RFC1123",
            "customFormat": ""
        }
    },
    "players": [
        {
            "uuid": "61902a9a-ee57-3dbe-9983-6580939e802a",
            "name": "Player392",
            "state": {
                "status": "INACTIVE",
                "time": 129600,
                "timeout": -1,
                "startTime": -1,
                "reason": ""
            },
            "pos": {
                "location": "minecraft:overworld",
                "x": -112,
                "y": 82,
                "z": -28,
                "yaw": -88.19983,
                "pitch": 2.849978
            },
            "game": {
                "gameMode": "creative",
                "flying": true
            }
        }
    ]
}
```

The `players` array is state, not configuration. SaveAFK rewrites it as players connect and go AFK, so edits there get overwritten.
