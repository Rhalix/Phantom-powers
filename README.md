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
| Key | Power | Unlock cost |
|-----|-------|-------------|
| V | Go ghost / go human (you can fly in ghost form) | - |
| R | Ecto Blast: green energy beam | free |
| G | Intangibility: float through walls, can't be hurt. **Sneak + G** turns the mob, player or wall you look at intangible | 3 points |
| H | Possession: jump into a mob or player and control where it goes. Press H again to leave | 5 points |
| B | Ghostly Wail: a huge scream that breaks blocks and blasts mobs away. Drains you back to human form | 8 points |
| N | Wail Tornado: a giant spinning tornado of wail energy that roams forward for 10 seconds, sucking up mobs and ripping up the ground, then explodes outward. Drains you back to human form | 12 points |

You earn **1 stat point every 30 seconds** you stay in ghost form. Press a locked power's key to unlock it
once you have enough points. Powers must be unlocked in order. Powers use **ghost energy** (the green bar).

## Test commands (cheats on)
`/phantom grant`, `/phantom points <n>`, `/phantom unlockall`, `/phantom reset`, `/phantom ghostzone`

## Multiplayer servers
Set `allow-flight=true` in `server.properties`, or players floating while intangible may get kicked.
