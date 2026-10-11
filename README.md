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
| B | Ghostly Wail: **hold** to keep screaming (up to 10s). Shockwaves break blocks and damage and blast mobs away. Drains you back to human form | 8 |
| N | Wail Tornado: a giant tornado of wail energy that roams forward, sucking up mobs and ripping up the ground | 12 |
| J | Invisibility: completely invisible to other players, armor and all | 2 |
| K | Open the **Ghost Skills** screen (also a button on your inventory) | - |

You earn **1 stat point every 30 seconds** in ghost form. Spend them in the Ghost Skills screen, in any order.
Powers use **ghost energy** (the green bar).

## Ghost Zone doors
Every floating door in the Ghost Zone leads to **its own room**: libraries, frozen halls, ember chambers,
gardens, treasure vaults, ghost prisons, aquariums and more. Each room is different in size, layout and loot,
and some have mobs. Use the door behind you to come back out.

## Ghost form look
In ghost form a solid white suit with gloves and boots appears over your body. Your face, skin and hair stay your own.

## Hover Glider
Craft: iron ingot / ectoplasm / iron ingot over redstone / firework rocket / redstone.
Place it, right-click to hop on. It flies where you look: W to fly, S to brake, A/D to strafe, **right-click to fire twin lasers**, sneak to hop off. Sneak + right-click it to pick it up.

## Ghost Hunter Suit
Craft: iron ingots in a chestplate shape with white wool and ectoplasm in the middle row.
It's as strong as a full set of netherite armor when deployed. Wear it in your **chest armor slot**: it shows as a compact armored backpack with a glowing core. Press **U** to deploy it: the pack vanishes and armor bands wrap up around your body and you're in a black armored suit with white gloves and boots and a glowing visor helmet (extra armor, strength, speed and night vision). Press U again to stow it. **Hold U** for a suit-up cutscene.

A guide in the bottom-right corner of the screen shows the keys for everything you've unlocked.

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

## The Crate King
A box-obsessed ghost boss with a cardboard box for a head lives in a floating warehouse in each region of the Ghost Zone (across from the Ghost Prison).
Stage one he hurls crates at you. At half health he pulls out **Pandora's Box** and turns evil: dark purple beams, a whirling crate storm, and ghosts pouring out of the box.
Beat him for **Pandora's Box** (right-click: a dark shockwave that blasts every hostile mob around you, 20s recharge). Weaken him below 20% to trap him in a Thermos.

## Voltage and the Thunder Guitar
A punk-rock ghost with crackling electric hair plays a concert stage in every region of the Ghost Zone. Her power chords blast sound, her solos call down lightning, and her fans fight for her. Below 40% she goes into an **Encore**.
Beat her for the **Thunder Guitar**: a strong melee weapon. Right-click for a **Power Chord** (sound blast), sneak + right-click for a **Lightning Riff** (strikes up to 3 hostile mobs).

## Ghost Blaster
Craft: iron ingot, iron ingot, glass pane / redstone block, ectoplasm / iron ingot (bottom-left). Right-click to fire. Double damage to ghosts, and stronger and faster with the Ghost Hunter Suit deployed. Repair with ectoplasm.

## Eating ghosts
Ghosts and bosses drop their **essence**. Eat it to take their power until you die:
Hostile Ghost (strength + night vision), Friendly Ghost (constant healing), Ghost Jailer (toughness, your hits chain enemies), Crate King (a crate-armor shield that comes back), Voltage (speed, every 4th hit calls lightning).

## Room ghosts
Every door room has its own ghost: Librarian Specter, Frost Wraith, Cinder Spirit, Bloom Sprite, Miser Ghost, Shackled Soul, Gearwork Ghost, Box Hoarder, Cloud Wisp, Drowned Phantom and Spirit Monk. Some are friendly and give gifts, others attack with their own tricks.

## Bosses come back, and break out
Every region of the Ghost Zone has a Ghost Prison (two designs: the Big House and the Spire), a Crate King warehouse and a Voltage stage. Prisons shine a **green beacon beam** you can see from far away. A beaten boss returns to its lair one Minecraft day later.
Sometimes at night a boss **breaks out into the Overworld** near you. It fades away at sunrise if nobody's around.

## Ghost level
Every 2 stat points you earn raises your ghost level. In ghost form you get extra hearts, strength, armor and (from level 5) Resistance, growing with your level.

If a Ghost Portal won't turn on, the chat tells you what's wrong with the frame.

## Test commands (cheats on)
`/phantom grant`, `/phantom prison`, `/phantom crateking`, `/phantom voltage`, `/phantom breakout`, `/phantom sense`, `/phantom testportal`, `/phantom points <n>`, `/phantom unlockall`, `/phantom reset`, `/phantom ghostzone`

## Multiplayer servers
Set `allow-flight=true` in `server.properties`, or players floating while intangible may get kicked.
