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

If a Ghost Portal won't turn on, the chat tells you what's wrong with the frame.

## Test commands (cheats on)
`/phantom grant`, `/phantom testportal`, `/phantom points <n>`, `/phantom unlockall`, `/phantom reset`, `/phantom ghostzone`

## Multiplayer servers
Set `allow-flight=true` in `server.properties`, or players floating while intangible may get kicked.
