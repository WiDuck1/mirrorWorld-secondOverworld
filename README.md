# Mirror World

Adds a reusable **Mirror Stone** and a separate **Mirror World** with vanilla
Overworld terrain, biomes, ores, and structures. The dimension shares the save's
world-generation seed, so terrain may match the Overworld, but its chunks and
changes are stored separately. It does not automatically reset.

Craft the Mirror Stone in a crafting table:

```text
empty       iron ingot    empty
iron ingot  compass       iron ingot
empty       ender pearl   empty
```

Right-click once and stand still for four seconds to travel. The charge uses
the vanilla Nether portal screen distortion and trigger sound, with portal
particles around the player. Moving (including jumping or knockback), switching
away from the stone, dying, or changing dimensions cancels the charge. Looking
around is allowed. The effect fades out when the charge ends or is cancelled.
Minecraft's accessibility settings control the portal screen effect as usual.

Use it in the Overworld to travel to the Resource World. Your first
visit searches for a safe surface near the same X/Z coordinates. Later visits
return to the position where you last used the stone to leave that dimension.
The stone remembers separate positions and viewing directions for the
Overworld and Resource World. If a remembered point is obstructed, it searches
for a safe nearby surface. If no safe spot is found, it leaves you where you are.
Destination chunks are loaded before checking their terrain height.

Positions are saved on each player, survive restarts and death, and work with
replacement stones. Existing Overworld return positions from older compasses
are imported on use. With no remembered Overworld position, the stone searches
near the Overworld spawn. The item is not consumed and has a two-second cooldown.
It only works in these two dimensions, and you must dismount before using it.

The Mirror Stone appears in the Tools & Utilities creative tab and uses
a custom purple compass texture. For testing:

```mcfunction
/give @s mirrorworld:mirror_stone
```

The mod ID is `mirrorworld` and the item ID is `mirrorworld:mirror_stone`.
Registry identifiers must be lowercase; the displayed mod name is `mirrorWorld`.
This replaces the old `eriks-test-mod1` namespace. Existing items, dimension data,
and player travel attachments under that namespace are not automatically migrated.
Use a new test world for this version, or migrate an existing save before using it.

Install NeoForge 26.2.0.88 or newer for Minecraft 26.2 and this mod on both the client and server, then restart the
world/server so the new dimension loads. Build with `./gradlew build` (Java 25);
the mod jar is `build/libs/mirrorworld-neoforge-26.2-1.0.0.jar`.

## Setup

This branch targets Minecraft 26.2 with NeoForge and Java 25. Import the Gradle project into IntelliJ IDEA. Run `./gradlew runClient` or `./gradlew runServer` for development.

Travel positions are stored using NeoForge attachments. Existing Fabric player attachments are not automatically migrated; use a new test world when testing the port.

## License

This template is available under the MIT license. Feel free to learn from it and incorporate it in your own projects.
