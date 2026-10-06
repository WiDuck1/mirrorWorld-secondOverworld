# Mirror World

Adds a reusable **Mirror Stone** and a separate **Mirror World** with vanilla
Overworld terrain, biomes, ores, and structures. The dimension shares the save's
world-generation seed, so terrain may match the Overworld, but its chunks and
changes are stored separately. It does not automatically reset.

This version targets Minecraft **26.2** with Fabric and requires Java **25**.

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
Fabric requires lowercase identifiers; the displayed mod name is `mirrorWorld`.
This replaces the old `eriks-test-mod1` namespace. Existing items, dimension data,
and player travel attachments under that namespace are not automatically migrated.
Use a new test world for this version, or migrate an existing save before using it.

Install the mod and Fabric API on both the client and server, then restart the
world/server so the new dimension loads. Build with `./gradlew build` (Java 25);
the mod jar is `build/libs/mirrorworld-1.0.0.jar`.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the MIT license. Feel free to learn from it and incorporate it in your own projects.
