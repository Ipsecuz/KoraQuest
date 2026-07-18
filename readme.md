<div align="center">

# KoraQuest

**A modern, configurable, multi-cycle and Folia-ready quest system for Minecraft servers.**

![Version](https://img.shields.io/badge/version-1.1-E5B94E)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-62B47A)
![Java](https://img.shields.io/badge/Java-21-ED8B00)
![Platform](https://img.shields.io/badge/platform-Paper%20%7C%20Folia-5C6BC0)

[Documentation](https://ipsecuz.github.io/KoraQuest-Wiki/) · [SpigotMC](https://www.spigotmc.org/resources/137091/) · [Issues](https://github.com/Ipsecuz/KoraQuest/issues)

</div>

---

## Overview

KoraQuest is a professional quest system for **Paper and Folia** servers. It supports independent Daily, Weekly, Monthly, Seasonal and custom quest cycles, secure reward delivery, advanced anti-exploit protection, Java and Bedrock interfaces, multiple storage engines and a public developer API.

Every quest is configured in `quests.yml`, including its cycle, objective, target, required amount, display item, requirements, filters, completion effects and reward commands.

KoraQuest does not require a specific economy, crate or storage plugin. Standard rewards are executed as configurable console commands, while developers can register custom transactional reward providers through the API.

## Screenshots

### Quest Menu

![KoraQuest Quest Menu](https://i.postimg.cc/SxPfJm1M/image.png)

### Commands

![KoraQuest Commands](https://i.postimg.cc/zGpXjBYG/image.png)

---

## Main Features

- Daily, Weekly, Monthly, Seasonal and custom quest cycles
- Real calendar-based reset schedules with timezone support
- Easy, Medium and Hard difficulty pools
- 65 bundled quests, including 30 additional quest templates
- Up to 15 Daily quests and 10 quests for other default cycles
- Automatic GUI pagination with 14 quest icons per page
- Secure Java inventory interface
- Native Bedrock forms through Floodgate and Cumulus
- Advanced player-placed block and mob-spawn anti-exploit checks
- Correct shift-click, number-key and recipe-book crafting progress
- Persistent reward claims with checkpoints and automatic retry
- Free and paid quest rerolls
- Daily streak, Perfect Week, catch-up token and seasonal progression systems
- SQLite, MySQL and MariaDB storage through HikariCP
- Optional SQL network synchronization and Redis Pub/Sub
- PlaceholderAPI expansion
- In-game quest editor
- Public API, lifecycle events and custom provider support
- bStats metrics
- SpigotMC update checker using resource ID `137091`
- Paper and Folia platform detection

---

## Requirements

- Minecraft `1.21.x`
- Java `21`
- Paper or Folia

### Optional Integrations

- PlaceholderAPI
- Vault
- PlayerPoints
- CoinsEngine
- Floodgate
- Citizens
- WorldGuard
- MythicMobs
- SuperiorSkyblock2
- BentoBox
- PlotSquared
- Jobs Reborn
- mcMMO
- ItemsAdder
- Oraxen
- Nexo
- MMOItems
- ExecutableItems
- ExcellentCrates
- VotingPlugin
- NuVotifier

KoraQuest starts normally when optional integrations are not installed.

---

## Quest Cycles

Each cycle has independent progress, quest pools, permissions, limits, rerolls, history and reset scheduling.

| Cycle | Default selected quests | Default active limit |
|---|---:|---:|
| Daily | 15 | 15 |
| Weekly | 10 | 10 |
| Monthly | 10 | 10 |
| Seasonal | 10 | 10 |

Supported reset modes:

- `CRON`
- `DAILY`
- `WEEKLY`
- `MONTHLY`
- `INTERVAL`
- `MANUAL`

---

## Supported Objective Types

| Group | Objective types |
|---|---|
| Combat | `KILL`, `DAMAGE_DEALT`, `DAMAGE_TAKEN`, `HEAL`, `DIE` |
| Blocks | `BREAK`, `PLACE`, `INTERACT_BLOCK` |
| Items | `CRAFT`, `SMELT`, `ENCHANT`, `CONSUME`, `ITEM_PICKUP`, `ITEM_DROP`, `ITEM_SUBMIT`, `REPAIR`, `ANVIL`, `SMITHING`, `STONECUT`, `LOOT_CHEST` |
| Entities | `BREED`, `TAME`, `SHEAR`, `INTERACT_ENTITY`, `TRADE_VILLAGER` |
| Movement | `WALK`, `SPRINT`, `SWIM`, `FLY`, `GLIDE`, `JUMP`, `VISIT_LOCATION`, `ENTER_WORLD`, `ENTER_REGION` |
| Player activity | `PLAYTIME`, `CHAT`, `COMMAND`, `LOGIN`, `VOTE`, `EXP_GAIN`, `LEVEL_GAIN` |
| Economy | `MONEY_EARN`, `MONEY_SPEND` |
| Other | `FISH`, `POTION_BREW`, `BUCKET_FILL`, `CUSTOM` |

Objectives support filters such as world, Y range, weapon, spawn reason, critical hits, regions, island ownership, plot ownership, custom item IDs and custom entity IDs.

---

## Example Quest

```yaml
quests:
  daily_zombie_cleaner:
    cycle: daily
    difficulty: easy
    type: KILL
    target: ZOMBIE
    required: 15
    material: ROTTEN_FLESH

    title: "&#75FF75ᴢᴏᴍʙɪᴇ ᴄʟᴇᴀɴᴜᴘ"
    lore:
      - "&#8A8A8AKill &f15 Zombies&#8A8A8A."
      - "&#75FF75Reward: money and MobCoins"

    filters:
      worlds: [survival]
      spawn-reasons: [NATURAL, SPAWNER]

    completion:
      sound: ENTITY_PLAYER_LEVELUP
      particle: TOTEM_OF_UNDYING

    rewards:
      - "eco give %player% {random:100000-200000}"
      - "mc give %player% 50"
```

---

## Reward System

KoraQuest stores a durable reward claim before executing rewards.

- Unique `claim_id` for every quest completion
- Database uniqueness protection against duplicate delivery
- Persisted command checkpoints
- Automatic retry for failed or interrupted claims
- Retry when the player reconnects
- Administrator retry command
- Quest status becomes `COMPLETED` only after the complete claim is delivered

Standard rewards are console commands:

```yaml
rewards:
  - "eco give %player% {random:2000000-5000000}"
  - "crate key give %player% rare 1"
```

Custom API reward providers use this syntax:

```yaml
rewards:
  - "provider:myplugin:reward-value"
```

---

## Reward Placeholders

| Placeholder | Value |
|---|---|
| `%player%` | Player name |
| `%uuid%` | Player UUID |
| `%quest_id%` | Quest ID |
| `%quest_name%` | Quest display title |
| `%cycle%` | Quest cycle |
| `%difficulty%` | Difficulty ID |
| `%difficulty_name%` | Translated difficulty name |
| `%progress%` | Current progress |
| `%required%` | Required progress |
| `%percent%` | Progress percentage |
| `%reset_time%` | Current cycle reset time |
| `%quest_reset_time%` | The quest's own cycle reset time |
| `{random:min-max}` | Random integer |

PlaceholderAPI values can also be used in quest titles, lore, requirements, rewards, GUI text and broadcasts.

---

## Anti-Exploit Protection

KoraQuest can prevent common progress-farming methods:

- Tracks player-placed blocks in storage
- Prevents PLACE/BREAK loops using the same block
- Creative mode and gamemode validation
- Configurable Silk Touch handling
- Automation and fake-player checks
- Mob spawn-reason whitelist
- Custom-named mob filtering
- MythicMobs filtering
- Hooked entities do not count as fish
- World blacklist and whitelist
- WorldGuard region checks
- SuperiorSkyblock2 and BentoBox island checks
- PlotSquared ownership checks
- Permission-based bypasses

---

## Storage and Network Support

```yaml
storage:
  type: SQLITE # SQLITE, MYSQL, MARIADB
```

Features include:

- HikariCP connection pooling
- Automatic schema creation
- Legacy SQLite migration
- Lazy player-data loading
- Cache expiration and delayed unloading
- Sequential saves per UUID
- Shared SQL cycle state
- Cross-server progress
- Distributed cycle-reset locks
- Optional Redis Pub/Sub invalidation

SQLite uses a single connection to reduce database-lock contention.

---

## PlaceholderAPI

Common placeholders:

```text
%koraquest_active_count%
%koraquest_completed_daily%
%koraquest_daily_total%
%koraquest_daily_remaining%
%koraquest_reset_time%
%koraquest_streak%
%koraquest_best_streak%
%koraquest_perfect_weeks%
%koraquest_catchup_tokens%
%koraquest_season_level%
%koraquest_season_xp%
%koraquest_season_next_level_xp%
%koraquest_<cycle>_total%
%koraquest_<cycle>_completed%
%koraquest_<cycle>_remaining%
%koraquest_<cycle>_active%
%koraquest_<cycle>_reset_time%
%koraquest_<cycle>_cycle_id%
%koraquest_quest_<id>_progress%
%koraquest_quest_<id>_required%
%koraquest_quest_<id>_percent%
%koraquest_quest_<id>_status%
%koraquest_quest_<id>_cycle%
```

---

## Commands

### Player Commands

| Command | Description |
|---|---|
| `/quest` | Open the quest interface |
| `/quest help` | Display available commands |
| `/quest <cycle>` | Open a specific cycle |
| `/quest active [cycle]` | Show active quests |
| `/quest search [cycle] <text>` | Search quests |
| `/quest accept <id>` | Accept a quest |
| `/quest cancel <id>` | Cancel an active quest |
| `/quest claim <id>` | Claim a completed quest |
| `/quest reroll <id>` | Replace a quest |
| `/quest submit <id> [amount]` | Submit quest items |
| `/quest version` | Display plugin information |

### Administrator Commands

| Command | Description |
|---|---|
| `/quest admin` | Display administrator commands |
| `/quest editor [id]` | Open the quest editor |
| `/quest admin reload` | Reload configurations and messages |
| `/quest admin reset <cycle>` | Reset a quest cycle |
| `/quest admin editor [id]` | Open the administrator editor |
| `/quest admin create ...` | Create a quest template |
| `/quest admin delete <id>` | Delete a quest |
| `/quest admin progress <player> <id> <amount>` | Add quest progress |
| `/quest admin retryrewards [player]` | Retry pending rewards |
| `/quest admin validate [id]` | Validate quest configuration |
| `/quest admin types` | List supported objective types |
| `/quest admin update` | Check for a newer version |

Players only see commands they have permission to use. Administrators can view the complete command list.

---

## Permissions

```text
koraquest.command.open
koraquest.command.accept
koraquest.command.cancel
koraquest.command.claim
koraquest.command.reroll
koraquest.command.submit

koraquest.admin.reload
koraquest.admin.reset
koraquest.admin.editor
koraquest.admin.progress
koraquest.admin.rewardretry
koraquest.admin.update

koraquest.limit.<amount>
koraquest.limit.daily.<amount>
koraquest.limit.weekly.<amount>
koraquest.limit.monthly.<amount>
koraquest.limit.seasonal.<amount>
koraquest.reroll.<amount>
koraquest.booster.<percent>

koraquest.bypass.world
koraquest.bypass.antiexploit
koraquest.update-notify
```

Legacy parent permissions `koraquest.use` and `koraquest.admin` remain available.

---

# Developer API

KoraQuest provides a separate lightweight API artifact:

```text
target/KoraQuest-1.1-api.jar
```

The API artifact contains the public API, events and model packages. Do not install the API JAR in the server's `plugins` folder. Install `KoraQuest-1.1.jar` on the server and use the API JAR only as a compile-time dependency.

## Add the Dependency

### Gradle

```kotlin
dependencies {
    compileOnly(files("libs/KoraQuest-1.1-api.jar"))
}
```

### Maven Local Installation

```bash
mvn install:install-file \
  -Dfile=KoraQuest-1.1-api.jar \
  -DgroupId=dev.ipseucz \
  -DartifactId=KoraQuest \
  -Dversion=1.1 \
  -Dclassifier=api \
  -Dpackaging=jar
```

```xml
<dependency>
    <groupId>dev.ipseucz</groupId>
    <artifactId>KoraQuest</artifactId>
    <version>1.1</version>
    <classifier>api</classifier>
    <scope>provided</scope>
</dependency>
```

Add KoraQuest to the integrating plugin's `plugin.yml`:

```yaml
softdepend:
  - KoraQuest
```

Use `depend` instead when KoraQuest is required for your plugin to start.

## Check API Availability

```java
import dev.ipseucz.koraquest.api.KoraQuestAPI;

if (!KoraQuestAPI.isAvailable()) {
    return;
}
```

## Query Quest Data

```java
Optional<QuestDefinition> quest = KoraQuestAPI.getQuest("daily_zombie_cleaner");

Collection<QuestDefinition> active =
        KoraQuestAPI.getActiveQuests(player.getUniqueId());

Collection<QuestDefinition> dailyActive =
        KoraQuestAPI.getActiveQuests(player.getUniqueId(), "daily");

int progress = KoraQuestAPI.getProgress(
        player.getUniqueId(),
        "daily_zombie_cleaner"
);

String status = KoraQuestAPI.getStatus(
        player.getUniqueId(),
        "daily_zombie_cleaner"
);
```

Possible status values include:

```text
AVAILABLE
ACTIVE
READY
COMPLETED
UNAVAILABLE
```

## Modify Quest State

```java
KoraQuestAPI.acceptQuest(player, "daily_zombie_cleaner");
KoraQuestAPI.cancelQuest(player, "daily_zombie_cleaner");
KoraQuestAPI.completeQuest(player, "daily_zombie_cleaner");
KoraQuestAPI.rerollQuest(player, "daily_zombie_cleaner");
KoraQuestAPI.progressQuest(player, "daily_zombie_cleaner", 1);
```

## Progress Objectives

Progress all matching objectives of a built-in type:

```java
KoraQuestAPI.progress(
    player,
    ObjectiveType.VOTE,
    "SERVER_VOTE",
    1
);
```

Progress a custom objective:

```java
KoraQuestAPI.progressCustom(player, "DUNGEON_CLEAR", 1);
```

## Register an Objective Provider

```java
KoraQuestAPI.registerObjectiveProvider(
    "myplugin",
    (player, target, amount) ->
        KoraQuestAPI.progressCustom(player, target, amount)
);
```

Dispatch progress through the registered provider:

```java
KoraQuestAPI.dispatchObjectiveProvider(
    "myplugin",
    player,
    "DUNGEON_CLEAR",
    1
);
```

Unregister providers when the integrating plugin disables:

```java
KoraQuestAPI.unregisterObjectiveProvider("myplugin");
```

## Register a Reward Provider

```java
KoraQuestAPI.registerRewardProvider(
    "myplugin",
    (player, quest, value) -> {
        // Deliver a custom reward here.
        // Return true only when delivery succeeds.
        return true;
    }
);
```

Use the provider in `quests.yml`:

```yaml
rewards:
  - "provider:myplugin:reward-value"
```

Unregister it on plugin disable:

```java
KoraQuestAPI.unregisterRewardProvider("myplugin");
```

## API Events

KoraQuest exposes the following Bukkit events:

- `QuestAcceptEvent` — cancellable
- `QuestCancelEvent` — cancellable
- `QuestProgressEvent`
- `QuestReadyEvent`
- `QuestCompleteEvent`
- `QuestRewardEvent`
- `QuestCycleResetEvent`
- `QuestRerollEvent` — cancellable

Example listener:

```java
import dev.ipseucz.koraquest.event.QuestCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class QuestCompleteListener implements Listener {

    @EventHandler
    public void onQuestComplete(QuestCompleteEvent event) {
        String playerName = event.getPlayer().getName();
        String questId = event.getQuest().id();

        getLogger().info(playerName + " completed " + questId);
    }
}
```

## Folia Thread Safety

Query methods read already-loaded data. Methods that modify a player's quest state or progress must run on that player's owning entity thread when the server uses Folia.

This includes:

- `acceptQuest`
- `cancelQuest`
- `completeQuest`
- `rerollQuest`
- `progressQuest`
- `progress`
- `progressCustom`

---

## File Structure

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

---

## Build

```bash
mvn clean package
```

Generated artifacts:

```text
target/KoraQuest-1.1.jar
target/KoraQuest-1.1-api.jar
```

- `KoraQuest-1.1.jar` — install this on the server
- `KoraQuest-1.1-api.jar` — compile-time dependency for developers

---

## Update Checker

```yaml
update-checker:
  enabled: true
  resource-id: 137091
  interval-hours: 6
  notify-console: true
  notify-permission: koraquest.update-notify
  download-url: ''
```

The update checker only reports newer versions. It does not automatically download or replace the plugin.

---

## Metrics

KoraQuest uses bStats to collect anonymous usage statistics.

```yaml
metric: true
```

No player database information, quest definitions or private server data is collected.

---

## Documentation and Support

- Documentation: https://ipsecuz.github.io/KoraQuest-Wiki/
- Issues: https://github.com/Ipsecuz/KoraQuest/issues
- SpigotMC: https://www.spigotmc.org/resources/137091/

When reporting a problem, include:

- Server software and version
- KoraQuest version
- Installed integrations
- Relevant console errors
- Steps to reproduce the issue

---

## Author

Developed by **Ipsecuz_**.
