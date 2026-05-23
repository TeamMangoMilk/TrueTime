# TrueTime

TrueTime is a NeoForge Minecraft mod that keeps server time moving forward, even when commands or mods try to set the time backwards.

Instead of letting `/time set day`, `/time set night`, or other time-setting hooks rewind the world clock, TrueTime converts those changes into the next matching point in the future. This preserves the overall in-game day counter while still allowing players and server tools to choose a time of day.

## The Problem

Minecraft's time commands can reset the current day count because they set the absolute world time directly. On servers that care about long-running day progression, this can make the world feel inconsistent and can interfere with systems that depend on the total number of elapsed days.

For example, setting the time to day on day 40 should not send the world back to day 0. It should move the clock forward to the next valid day time.

## How it Works

TrueTime intercepts server-side time changes and normalises them so the resulting world time is never earlier than the current world time.

1. **Forward-only changes**: Time updates that would move the clock backwards are shifted into the next future day cycle.
2. **Day counter preservation**: The total elapsed day count continues increasing instead of being reset by absolute time commands.
3. **Vanilla-friendly behaviour**: Commands and systems can still request familiar times such as day, noon, night, or midnight; TrueTime simply resolves them to the next valid future occurrence.
4. **Overworld-only tracking**: TrueTime tracks and corrects the Overworld day counter, leaving custom dimensions and non-standard time systems alone.
5. **Manual day control**: Server operators can inspect, reset, resync, or set the tracked day counter when needed.
6. **Server-side correction**: The fix is applied on the server where authoritative world time is controlled.

## Commands

TrueTime provides operator commands for manually controlling the preserved day counter:

```mcfunction
/truetime info
/truetime resetday
/truetime setday <day>
/truetime adddays <days>
/truetime settime <day> <timeOfDay>
/truetime sync
/truetime reload
```

- `/truetime info` shows the preserved day counter, raw Overworld game time, current time of day, and recent correction status.
- `/truetime resetday` resets the preserved day counter back to day 0.
- `/truetime setday <day>` sets the preserved day counter to a specific non-negative day value.
- `/truetime adddays <days>` moves the preserved day counter forward by a specific number of days.
- `/truetime settime <day> <timeOfDay>` sets both the preserved day counter and the time of day in one command.
- `/truetime sync` recalculates the preserved day counter from the current Overworld time, useful after world migration or configuration changes.
- `/truetime reload` refreshes TrueTime integrations, rewrites the placeholder export, and retries TAB placeholder registration.

Commands that would move the Overworld clock backwards require `allowBackwardTimeForOps` to be enabled.

## Configuration

TrueTime includes server configuration options for controlling how strict and visible time correction should be:

- `allowBackwardTimeForOps`: Allows operators to intentionally move time backwards through dedicated TrueTime commands. Regular vanilla-style time changes remain protected by default.
- `logCorrections`: Logs whenever TrueTime converts a backward time change into the next valid future time. This is useful for diagnosing command blocks, datapacks, or other mods that modify time.
- `exportPlaceholderFile`: Writes the preserved day counter to a plain text file for PlaceholderAPI/TAB bridges.
- `placeholderFilePath`: Controls where the placeholder export is written. The default is `truetime/day.txt` in the server root.
- `announceDayChanges`: Announces preserved Overworld day changes to players. Disabled by default.
- `announcementMode`: Controls where announcements appear. Valid values: `chat`, `actionbar`, `title`, `chat_actionbar`, `chat_title`, and `all`.
- `dayChangeMessage`: Custom chat message used for day-change announcements. Supports `{day}`, `{time}`, and `{raw_time}`.
- `dayChangeTitle`: Custom title text used when `announcementMode` includes `title`.
- `dayChangeSubtitle`: Custom subtitle text used when `announcementMode` includes `title`. Leave empty to hide it.
- `dayChangeActionBar`: Custom action bar text used when `announcementMode` includes `actionbar`.
- `titleFadeInTicks`, `titleStayTicks`, `titleFadeOutTicks`: Title timing controls in ticks.

## Persistence

TrueTime stores its preserved day counter in world saved data, not a global config file. This keeps the tracked day value tied to the world itself so it survives server restarts, backups, transfers, and modpack updates.

When TrueTime is added to an existing server, it initialises from the current Overworld time on the first server tick. This means existing worlds keep their current day counter instead of starting again from day 0.

Only the Overworld day counter is tracked. Nether, End, and custom dimension time values are not corrected unless they are explicitly tied to Overworld time by another system.

## Placeholder Export

When the NEZNAMY/TAB mod is installed, TrueTime registers a TAB server placeholder directly:

```text
%truetime_day%
```

This placeholder can be used in TAB config files on NeoForge servers without Bukkit, Paper, or PlaceholderAPI.

TrueTime can also export the preserved Overworld day counter to a plain text file for external tools or custom bridge setups.

By default, the file is written to:

```text
truetime/day.txt
```

The file contains only the current preserved day number, for example:

```text
42
```

The file is written when TrueTime first initialises, when the preserved day changes, and when an operator command updates the counter. This gives plugin-side placeholder tools a simple value to read without requiring direct NeoForge-to-Bukkit API integration.

## Requirements

- Minecraft: `1.21.1`
- NeoForge: `21.1.230` or newer
- Java: `21`
- Side: Server-side. Clients do not need the mod to connect.

## Installation

Place the compiled TrueTime JAR in the server's `mods` folder and start the server normally.

## Build from Source

```cmd
.\gradlew.bat build
```

The compiled mod JAR will be generated at `build/libs/truetime-1.0.0.jar`.

## License

This project is licensed under the MIT License.
