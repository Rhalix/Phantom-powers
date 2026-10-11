# Phantom Powers

A ghost-powers mod for **Minecraft 1.21.1** with **Fabric**.

## Install
1. Install **Fabric Loader** for 1.21.1 (fabricmc.net/use).
2. Put **Fabric API** (for 1.21.1) and `phantom-powers-1.0.0.jar` in your `.minecraft/mods` folder.
3. Start Minecraft with the Fabric profile.

## Getting your powers
1. Build a **Ghost Portal**: a closed frame (like a Nether portal, any size) made of **black concrete**,
   with **at least 5 iron blocks** somewhere in the frame. Example, standing upright:

   ```
   I I B I
   B . . B
   B . . B
   B . . B
   I B B I      I = iron block, B = black concrete, . = empty
   ```
2. Put a **button** inside the opening (on the floor or on an inside wall).
3. Stand **inside** the portal and press the button. The portal turns on and zaps you.
4. You respawn in front of the portal **with ghost powers**.

The portal now leads to the **Ghost Zone**: a green void full of floating islands and drifting doors.
Walk back through the portal there to come home.

## Controls (change them in Options > Controls > Phantom Powers)
| Key | Power | Cost |
|-----|-------|------|
| V | Go ghost / go human (you can fly in ghost form) | - |
| R | Ecto Blast: a glowing green energy beam | free |
| G | Intangibility: walk through walls. Floors stay solid; **hold sneak to sink**, fly up to rise. **Sneak + hold G** on a mob, player or wall keeps it intangible until you let go | 3 |
| H | Possession: take over a mob or player and control where it goes. H again to leave | 5 |
| B | Ghostly Wail: a huge scream that breaks blocks and blasts mobs away. Drains you back to human form | 8 |
| N | Wail Tornado: a giant tornado of wail energy that roams forward, sucking up mobs and ripping up the ground | 12 |
| J | Invisibility: completely invisible to other players, armor and all | 2 |
| K | Open the **Ghost Skills** screen (also a button on your inventory) | - |

You earn **1 stat point every 30 seconds** in ghost form. Spend them in the Ghost Skills screen, in any order.
Powers use **ghost energy** (the green bar).

## Ghost Zone doors
Every floating door in the Ghost Zone leads to **its own room**: libraries, frozen halls, ember chambers,
gardens, treasure vaults, ghost prisons, aquariums and more. Each room is different in size, layout and loot,
and some have mobs. Use the door behind you to come back out.

## Ghost sense
When a ghost or hostile mob comes near, a wisp of icy blue breath curls out of your mouth and your screen frosts over.

## Ghosts
The Ghost Zone is haunted. **Hostile ghosts** hunt you through walls. **Friendly ghosts** float along beside you and give you a gift if you talk to them (right-click).
Many door rooms are haunted too, some by friendly ghosts and some by hostile ones. Ghosts drop **Ectoplasm**.

## The Thermos
Craft: iron nugget on top, iron ingot / ectoplasm / iron ingot, iron ingot / redstone / iron ingot.
Right-click a ghost to suck it inside. It holds as many ghosts as you like, but each catch wears it down. If it breaks, they all escape!
Sneak + right-click to let them all out. It even yanks other players out of ghost form.

## Ghost Prison and the Ghost Jailer
Floating prison fortresses are scattered across the Ghost Zone (about one every 640 blocks). Cells hold captured ghosts.
The **Ghost Jailer** guards it: it chains you back when you run, calls guard ghosts, and hits hard. Beat it to free the prisoners and loot the hall, where a Thermos is always waiting.
Weaken it below 30% health and you can trap it in a Thermos.

## Ghost level
Every 2 stat points you earn raises your ghost level. In ghost form you get extra hearts, strength, armor and (from level 5) Resistance, growing with your level.

If a Ghost Portal won't turn on, the chat tells you what's wrong with the frame.

## Test commands (cheats on)
`/phantom grant`, `/phantom prison`, `/phantom sense`, `/phantom testportal`, `/phantom points <n>`, `/phantom unlockall`, `/phantom reset`, `/phantom ghostzone`

## Multiplayer servers
Set `allow-flight=true` in `server.properties`, or players floating while intangible may get kicked.
