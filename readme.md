# KoraQuest

**A modern, configurable and Folia-ready quest system for Minecraft servers.**

KoraQuest allows server owners to create daily objectives, track player progress and execute fully customizable reward commands without depending on a specific economy, crate or storage plugin.

Each quest is configured directly inside `quests.yml`, including its title, lore, objective, required amount, display item and reward commands.

## Features

* Configurable Easy, Medium and Hard quests
* Daily quest rotation with weighted difficulty distribution
* Per-quest console command rewards
* SQLite player data storage
* Multi-language message system
* Secure inventory interface
* Paper, Purpur, Spigot and Folia support
* Built-in and custom quest objectives
* Random number placeholders for reward commands
* bStats metrics
* SpigotMC update checker
* Startup banner with platform, version and loaded quest count

## Supported quest types

| Type      | Description                              |
| --------- | ---------------------------------------- |
| `KILL`    | Kill entities                            |
| `BREAK`   | Break blocks                             |
| `PLACE`   | Place blocks                             |
| `CRAFT`   | Craft items                              |
| `FISH`    | Catch fish                               |
| `ENCHANT` | Enchant items                            |
| `BREED`   | Breed animals                            |
| `TAME`    | Tame entities                            |
| `CONSUME` | Consume items                            |
| `SMELT`   | Smelt materials                          |
| `SHEAR`   | Shear entities                           |
| `CUSTOM`  | Progressed by command or external plugin |

## Example quest

```yaml
quests:
  easy_zombie_cleaner:
    difficulty: easy
    type: KILL
    target: ZOMBIE
    required: 15
    material: ROTTEN_FLESH

    title: "&#75FF75ᴢᴏᴍʙɪᴇ ᴄʟᴇᴀɴᴜᴘ"
    lore:
      - "&#8A8A8AKill &f15 Zombies &#8A8A8Afrom your mob farm."
      - "&#75FF75Reward: money and MobCoins"

    rewards:
      - "eco give %player% {random:100000-200000}"
      - "mc give %player% 50"
```

KoraQuest does not interpret money, crate or storage rewards internally. Every line in `rewards` is executed as a console command.

This makes the plugin compatible with almost any external system.

## Custom quest example

```yaml
quests:
  medium_server_vote:
    difficulty: medium
    type: CUSTOM
    target: SERVER_VOTE
    required: 3
    material: EMERALD

    title: "&#FFD36Aꜱᴇʀᴠᴇʀ ꜱᴜᴘᴘᴏʀᴛᴇʀ"
    lore:
      - "&#8A8A8AVote for the server &f3 times&#8A8A8A."
      - "&#FFD36AReward: configured custom commands"

    rewards:
      - "eco give %player% 750000"
      - "opc give %player% uncommon 1"
```

Update progress through console or another plugin:

```text
quest admin progress <player> medium_server_vote 1
```

## Reward placeholders

| Placeholder         | Value                      |
| ------------------- | -------------------------- |
| `%player%`          | Player name                |
| `%uuid%`            | Player UUID                |
| `%quest_id%`        | Quest ID                   |
| `%quest_name%`      | Quest display title        |
| `%difficulty%`      | Difficulty ID              |
| `%difficulty_name%` | Translated difficulty name |
| `%progress%`        | Current progress           |
| `%required%`        | Required progress          |
| `{random:min-max}`  | Random integer             |

Example:

```yaml
rewards:
  - "eco give %player% {random:2000000-5000000}"
```

## File structure

```text
plugins/KoraQuest/
├── data/
│   └── data.db
├── message/
│   ├── messages.yml
│   └── messages_en.yml
├── config.yml
└── quests.yml
```

## Inventory security

KoraQuest blocks common GUI extraction methods, including:

* Left-click and right-click item movement
* Shift-click
* Hotbar number-key swapping
* Offhand swapping
* Double-click collection
* Drag events
* Drop-key actions
* Creative inventory cloning

Inventory refreshes are scheduled outside the active click event to reduce client/server desynchronization and duplication risks.

## Platform support

* Minecraft `1.21.x`
* Java `21`
* Spigot
* Paper
* Purpur
* Folia

## Build

```bash
mvn clean package
```

The compiled plugin will be generated at:

```text
target/KoraQuest-1.0.jar
```

## Metrics

KoraQuest uses bStats to collect anonymous usage statistics.

Metrics can be controlled in `config.yml`:

```yaml
metric: true
```

No player database information, quest contents or private server data is collected.

## License

Add the license selected for your public release here.

## Author

Developed by **Ipsecuz_**
