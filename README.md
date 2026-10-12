# Phantom Powers

A ghost-powers mod for **Minecraft 1.21.1** with **Fabric**.

## Install
1. Install **Fabric Loader** for 1.21.1 (fabricmc.net/use).
2. Put **Fabric API** (for 1.21.1) and `phantom-powers-1.0.0.jar` in your `.minecraft/mods` folder.
3. Start Minecraft with the Fabric profile.

## Getting your powers
1. Build a **Ghost Portal** like the lab portal from the show: a small ring of **Ghost Portal Frame** blocks
   (craft 8 from 4 iron ingots, 4 redstone and 1 copper ingot) around an opening. 2 wide and 3 tall is perfect
   (just 10 frame blocks; the corners are optional), and the smallest that works is 1 wide and 2 tall.
   **Hazard Plating** (craft 8 from 4 yellow dye, 4 black dye and 1 iron ingot) counts as frame too, for the striped trim:

   ```
   H H H H
   F . . F
   F . . F
   F . . F
   H H H H      F = Ghost Portal Frame, H = Hazard Plating, . = empty (corners optional)
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
| H | Possession: take over a mob or player and control where it goes. Your host gets your glowing ghost eyes, and you get **its powers**: a passive bonus plus a power on **R** (creeper explodes, skeleton shoots, blaze/ghast fireballs, enderman teleports, warden sonic boom, iron golem slam, horse gallop and more). H again to leave | 5 |
| B | Ghostly Wail: **hold** to keep screaming. Shockwaves break blocks and damage and blast mobs away. Drains you back to human form. **Hold it for 5 seconds** and it turns into a cutscene: the camera swings round, you rise up and wail in every direction, then you're drained back to human | 8 |
| N | Wail Tornado: a giant tornado of wail energy that roams forward, sucking up mobs and ripping up the ground | 12 |
| J | Invisibility: completely invisible, armor and held items too (but the Ghost Hunter Suit can still spot you) | 2 |
| Z | Frost Breath: **hold** to breathe ice. Enemies that stay in it freeze solid in a block of ice; **punch a frozen enemy on low health to shatter it**. Freezes water too | 6 |
| K | Open the **Ghost Skills** screen (also a button on your inventory) | - |

You earn **1 stat point every 30 seconds** in ghost form. Spend them in the Ghost Skills screen, in any order.
Powers use **ghost energy** (the green bar). Ghost flight is faster than creative flight, and when you fly fast you level out **like Superman**: body stretched along your flight path, one fist punched forward. While flying (not when you jump) your legs become a **ghost tail** (white with black stripes like your suit) that streams behind you, and you leave a glowing wisp trail. Going ghost plays a **hero pose**: arms thrown wide as the rings pass, then a fist punched to the sky.

## Ghost Zone doors
Every floating door in the Ghost Zone leads to **its own room**: libraries, frozen halls, ember chambers,
gardens, treasure vaults, ghost prisons, aquariums and more. Each room is different in size, layout and loot,
and some have mobs. Use the door behind you to come back out.

## Ghost form look
In ghost form a solid white suit with black stripes (collar, belt, side stripes, cuffs and boot bands) appears over your body. Your face, skin color and hairstyle stay your own, but your **hair turns white** and your **eyes glow red**.

## Hover Glider
Craft: iron ingot / ectoplasm / iron ingot over redstone / firework rocket / redstone.
Place it, right-click to hop on. You ride it standing up in a surfer stance with your arms out. It flies where you look: W to fly, S to brake, A/D to strafe, **right-click to fire twin lasers**, sneak to hop off. Sneak + right-click it to pick it up.

## Ghost Hunter Suit
While the suit is deployed, ghosts that have turned **invisible** within 64 blocks show up as a glowing **infrared outline** (even behind walls), and a small **tracker ring** around your crosshair shows little arrows pointing at nearby ghosts (green) and hostile mobs (red), with the distance to the closest.
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
Right-click a ghost to suck it inside (it begs "No... no... NOOOO!" as it gets pulled in). It holds as many ghosts as you like, but each catch wears it down. If it breaks, they all escape!
Sneak + right-click to let them all out. It even yanks other players out of ghost form.

## Ghost Prison and the Ghost Jailer
Floating prison fortresses are scattered across the Ghost Zone (about one every 640 blocks). Cells hold captured ghosts.
The **Ghost Jailer** guards it: it chains you back when you run, calls guard ghosts, and hits hard. It also marks the ground and comes crashing down in a **Prison Slam** shockwave (jump over the ring to dodge it), locks you in an **iron-bar cage** for a few seconds, and **blinks behind you** in a puff of smoke. Below half health it goes into **Lockdown**: faster, and its chains hit everyone nearby. Beat it to free the prisoners and loot the hall, where a Thermos is always waiting.
Weaken it below 30% health and you can trap it in a Thermos.

## The Crate King
A box-obsessed ghost boss with a cardboard box for a head lives in a floating warehouse in each region of the Ghost Zone (across from the Ghost Prison).
Stage one he hurls crates at you, makes **crates rain** from the sky and does a **Big Box Slam** shockwave. At half health he pulls out **Pandora's Box** and turns evil: dark purple beams, a whirling crate storm, and ghosts pouring out of the box. He also **boxes you in** with crate walls, and **Pandora's Vortex** drags everyone toward him before it explodes.
Beat him for **Pandora's Box** (right-click: a dark shockwave that blasts every hostile mob around you, 20s recharge). Weaken him below 20% to trap him in a Thermos.

## Voltage and the Thunder Guitar
A punk-rock ghost girl (spiky pink hair, studded leather jacket, plaid skirt, torn fishnets and platform boots) plays a concert stage in every region of the Ghost Zone. Her power chords blast huge sound waves, her solos call down lightning, and her fans fight for her. Her **Amp Blast** sends three sound shockwaves rolling across the floor, and she **Stage Dives** onto you with a lightning crash. Below 40% she goes into an **Encore**: her hair blazes up into a tall electric-pink flame and she electrifies the floor in rings of lightning.
Beat her for the **Thunder Guitar**. You hold it across your body like a real guitar, and when you use it you strum, slide along the neck and headbang. It's a double-cutaway electric guitar with a lightning-bolt sunburst, gold knobs and a whammy bar: a strong melee weapon. Right-click for a **Power Chord** (a giant glowing cone of sound waves, 16 blocks long, played as a real distorted guitar riff), sneak + right-click for a **Lightning Riff** (strikes up to 3 hostile mobs, with a screaming guitar solo).

## Ghost Blaster
A detailed silver sci-fi blaster with glowing green energy cells, cooling coils and a scope. **Hold sneak to aim down the scope**: 4x zoom, a green targeting reticle with a range finder, double the range and 35% more damage.
Craft: iron ingot, iron ingot, glass pane / redstone block, ectoplasm / iron ingot (bottom-left). Right-click to fire. Double damage to ghosts, and stronger and faster with the Ghost Hunter Suit deployed. Repair with ectoplasm.

## Eating ghosts
Ghosts and bosses drop their **essence**. Eat it to take their power until you die:
Hostile Ghost (strength + night vision), Friendly Ghost (constant healing), Ghost Jailer (toughness, your hits chain enemies), Crate King (a crate-armor shield that comes back), Voltage (speed, every 4th hit calls lightning), Wishkeeper (luck, and a free random blessing every minute).

## Room ghosts
Every door room has its own ghost: Librarian Specter, Frost Wraith, Cinder Spirit, Bloom Sprite, Miser Ghost, Shackled Soul, Gearwork Ghost, Box Hoarder, Cloud Wisp, Drowned Phantom and Spirit Monk. Some are friendly and give gifts, others attack with their own tricks.

## The Wishkeeper
A genie ghost with flowing black hair swept over one eye, a jeweled blue headband, a sky-blue top with lilac trim, a lilac choker and jeweled belt, blue bangles and a long curling sky-blue smoke tail floats in a domed golden palace in every region (look for the golden beacon). She grants **twisted wishes**: "You wish you could fly?" throws you into the sky, "You wish to be strong?" weighs you down, "You wish for riches?" makes gold explode around you, and "You wish to be safe?" locks you in a golden cage. She also fires homing wish orbs, vanishes in smoke and spins up sand cyclones. Below half health she calls a **Wish Storm** with smoky copies of herself, and wishes herself back to health once.
Beat her for the **Wishing Lamp** (right-click for a wish: usually a blessing, sometimes a trick) and her essence. Weaken her below 20% to trap her in a Thermos.

## Bosses come back, and break out
Every region of the Ghost Zone has a Ghost Prison (two designs: the Big House and the Spire), a Crate King warehouse, a Voltage stage and the Wishkeeper's palace.
Prisons have a real inside: the Big House has a two-story admin block with a **cafeteria**, the **warden's office** (with a safe), an armory, an infirmary and a visiting room; the Spire has an admin wing with a cafeteria and an office. Prisons shine a **green beacon beam** you can see from far away. A beaten boss returns to its lair one Minecraft day later.
Sometimes at night a boss **breaks out into the Overworld** near you. It fades away at sunrise if nobody's around.

## Ghost level
Every 2 stat points you earn raises your ghost level. In ghost form you get extra hearts, armor, a **Strength buff** that goes up a level every 7 ghost levels (Strength I up to V) plus extra punch damage, and (from level 5) Resistance, all growing with your level.

If a Ghost Portal won't turn on, the chat tells you what's wrong with the frame.

## Test commands (cheats on)
`/phantom grant`, `/phantom prison`, `/phantom crateking`, `/phantom voltage`, `/phantom wishkeeper`, `/phantom breakout`, `/phantom sense`, `/phantom testportal`, `/phantom points <n>`, `/phantom unlockall`, `/phantom reset`, `/phantom ghostzone`

## Multiplayer servers
Set `allow-flight=true` in `server.properties`, or players floating while intangible may get kicked.
