package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.ChefGhoulashEntity;
import com.phantompowers.network.StoryPayload;
import com.phantompowers.world.AmityPark;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Story mode. Pick a path (half-ghost, evil half-ghost or ghost hunter) and play its missions, which start in
 * Amity Park. The path is yours alone: anyone can still use any item or power, but only your own path's objectives
 * move your story on. Restart the story (/story restart) to pick a different path.
 */
public final class Story {
	public static final int NONE = 0, HERO = 1, EVIL = 2, HUNTER = 3;
	public static final String[] PATHS = {"", "Half-Ghost", "Evil Half-Ghost", "Ghost Hunter"};
	/** storyMission after the last mission. (99 was "finished mission 1" before mission 2 existed; it moves on to mission 2.) */
	private static final int DONE = 100, LEGACY_DONE = 99, MISSIONS = 2;

	private Story() {}

	public static String missionTitle(int path, int mission) {
		if (path == NONE) return "";
		if (mission >= LEGACY_DONE) return "Mission complete!";
		if (mission == 2) return path == EVIL ? "Mission 2: Lunch Rush (Dark Side)" : "Mission 2: Lunch Rush";
		return switch (path) {
			case HUNTER -> "Mission 1: Ghost Must Die";
			case EVIL -> "Mission 1: My Molecules Got All Rearranged (Dark Side)";
			default -> "Mission 1: My Molecules Got All Rearranged";
		};
	}

	/** The objectives of a mission for each path. */
	private static String[] steps(int path, int mission) {
		if (mission == 2) return switch (path) {
			case HERO -> new String[]{
					"Head to Amity High (your new powers keep glitching!)",
					"Get a Thermos (there's one in the Ghost Lab gear locker)",
					"Check out the new healthy menu in the school cafeteria",
					"Chef Ghoulash hates the new menu! Fight her until she's dazed",
					"Trap Chef Ghoulash in the Thermos (right-click her with it)"};
			case EVIL -> new String[]{
					"Head to Amity High (your dark powers keep glitching!)",
					"Get a Thermos (there's one in the Ghost Lab gear locker)",
					"Somebody's haunting YOUR school cafeteria. Go and see",
					"Show Chef Ghoulash who's in charge: beat her until she's dazed",
					"Trap her in the Thermos. She works for you now"};
			case HUNTER -> new String[]{
					"Ghost activity at Amity High! Suit up and head there",
					"Get a Thermos (there's one in the Ghost Lab gear locker)",
					"Find the ghost in the school cafeteria",
					"Fight Chef Ghoulash until he's dazed",
					"Trap Chef Ghoulash in the Thermos (right-click her with it)"};
			default -> new String[0];
		};
		return switch (path) {
			case HERO -> new String[]{
					"Head to the Ghost Lab house in Amity Park",
					"Go down the stairs to the basement lab",
					"Step inside the Ghost Portal and press the switch",
					"Go ghost! (press V)",
					"Fly! (double-tap jump in ghost form)"};
			case EVIL -> new String[]{
					"Head to the Ghost Lab house in Amity Park",
					"Go down the stairs to the basement lab",
					"Switch on the Prototype Portal (right-click it) and stand in front of it",
					"Wait for the ecto acne to wear off...",
					"Embrace the dark side: Dark Form (Ghost Skills) and go ghost"};
			case HUNTER -> new String[]{
					"Head to the Ghost Lab house in Amity Park",
					"Go down the stairs to the basement lab",
					"Build your own Ghost Hunter Suit: craft it from 7 iron, 2 white wool and 1 ectoplasm (there's ectoplasm in the lab's gear locker)",
					"Wear the suit (chest slot) and suit up! (press U)",
					"Ghosts broke out of the portal! Destroy 3 ghosts"};
			default -> new String[0];
		};
	}

	public static String objective(GhostData d) {
		String[] s = steps(d.storyPath, d.storyMission);
		if (d.storyMission >= LEGACY_DONE) return "More missions coming soon! (/story restart to play another path)";
		if (d.storyStep >= s.length) return "";
		String o = s[d.storyStep];
		if (d.storyMission == 1 && d.storyPath == HUNTER && d.storyStep == 4) o += " (" + Math.min(3, d.storyCount) + "/3)";
		return o;
	}

	// -------------------------------------------------------------- start

	/** Show the path picker (if no story is running) or the current objective. */
	public static void open(ServerPlayerEntity p) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		if (d.storyPath == NONE) send(p, d, true);
		else {
			send(p, d, false);
			p.sendMessage(Text.literal("Story (" + PATHS[d.storyPath] + "): " + missionTitle(d.storyPath, d.storyMission)).formatted(Formatting.GREEN), false);
			p.sendMessage(Text.literal("Objective: " + objective(d)).formatted(Formatting.GRAY), false);
		}
	}

	public static void restart(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		d.storyPath = NONE;
		d.storyMission = 0;
		d.storyStep = 0;
		d.storyCount = 0;
		st.markDirty();
		p.sendMessage(Text.literal("Story reset. Pick a new path!").formatted(Formatting.YELLOW), false);
		send(p, d, true);
	}

	/** The player picked a path on the story screen. */
	public static void choose(ServerPlayerEntity p, int path) {
		if (path < HERO || path > HUNTER) return;
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.storyPath != NONE) {
			p.sendMessage(Text.literal("You're already on the " + PATHS[d.storyPath] + " path. Use /story restart to change it.").formatted(Formatting.YELLOW), false);
			return;
		}
		d.storyPath = path;
		d.storyMission = 1;
		d.storyStep = 0;
		d.storyCount = 0;
		st.markDirty();
		boolean ready = AmityPark.request(p.getServer());
		BlockPos c = AmityPark.state(p.getServer()).center;
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(missionTitle(path, 1).replace(" (Dark Side)", "")).formatted(path == EVIL ? Formatting.LIGHT_PURPLE : Formatting.GREEN, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Head for Amity Park").formatted(Formatting.GRAY)));
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.2f);
		if (ready) p.sendMessage(Text.literal("Amity Park is at " + c.getX() + ", " + c.getZ() + " in the Overworld. The tracker shows the way, or fast travel there from the Phantom Menu (Story Mode).")
				.formatted(Formatting.AQUA), false);
		else p.sendMessage(Text.literal("Amity Park is being built in the background (you can keep playing). You'll get a message when it's ready.")
				.formatted(Formatting.AQUA), false);
		p.sendMessage(Text.literal("Story mode: " + PATHS[path] + " path").formatted(Formatting.GREEN, Formatting.BOLD), false);
		p.sendMessage(Text.literal(intro(path)).formatted(Formatting.GRAY), false);
		send(p, d, false);
	}

	private static String intro(int path) {
		return switch (path) {
			case HUNTER -> "Amity Park is crawling with ghosts, and somebody has to stop them. Build yourself a hunter suit at the Ghost Lab. Ghosts must die!";
			case EVIL -> "There's a prototype portal in the Ghost Lab basement. It's unstable. It's dangerous. It's exactly what you need...";
			default -> "Your parents built a Ghost Portal in the basement of the Ghost Lab, but it doesn't work. Maybe you should take a look inside...";
		};
	}

	/** Fast travel to Amity Park. */
	public static void fastTravel(ServerPlayerEntity p) {
		if (!AmityPark.request(p.getServer())) {
			p.sendMessage(Text.literal("Amity Park is still being built (" + AmityPark.progress(p.getServer()) + "%). Try again in a moment!").formatted(Formatting.YELLOW), true);
			return;
		}
		BlockPos c = AmityPark.state(p.getServer()).center;
		Vec3d at = AmityPark.entrance(c);
		ServerWorld ow = p.getServer().getOverworld();
		p.getServerWorld().spawnParticles(GhostPowers.GREEN, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
		p.teleport(ow, at.x, at.y, at.z, -90f, 0f);
		ow.spawnParticles(GhostPowers.GREEN, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.05);
		ow.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1.2f);
		p.sendMessage(Text.literal("Welcome to Amity Park!").formatted(Formatting.GREEN), true);
	}

	/** The town finished building: tell everyone who's on a story path. */
	public static void townReady(MinecraftServer server, BlockPos c) {
		GhostState st = GhostState.of(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			GhostData d = st.data(p.getUuid());
			if (d.storyPath == NONE) continue;
			p.sendMessage(Text.literal("Amity Park is ready! It's at " + c.getX() + ", " + c.getZ() + " in the Overworld. Follow the tracker, or fast travel from the Phantom Menu (Story Mode).")
					.formatted(Formatting.GREEN), false);
			p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1f, 1.2f);
			send(p, d, false);
		}
	}

	/** "(312 blocks north-east)" toward the Ghost Lab, for the first objective. */
	private static String direction(ServerPlayerEntity p, GhostData d) {
		BlockPos c = AmityPark.state(p.getServer()).center;
		if (p.getWorld() != p.getServer().getOverworld()) return " (it's in the Overworld)";
		Vec3d goal = d.storyMission != 2 ? AmityPark.labHouseDoor(c) : d.storyStep == 0 ? AmityPark.schoolDoor(c) : AmityPark.cafeteria(c);
		Vec3d to = goal.subtract(p.getPos());
		int dist = (int) Math.sqrt(to.x * to.x + to.z * to.z);
		String[] dirs = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
		float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
		String dir = dirs[Math.floorMod(Math.round(yaw / 45f), 8)];
		return " (" + dist + " blocks " + dir + ")";
	}

	// --------------------------------------------------------------- tick

	public static void tick(MinecraftServer server) {
		if (server.getTicks() % 10 != 0) return;
		GhostState st = GhostState.of(server);
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			GhostData d = st.data(p.getUuid());
			if (d.storyPath == NONE || d.storyMission >= DONE) continue;
			if (d.storyMission == LEGACY_DONE) {   // finished mission 1 before mission 2 existed
				startMission(p, d, 2);
				st.markDirty();
				continue;
			}
			if (!AmityPark.request(server)) {   // the town is still going up (or this world never started it)
				if (server.getTicks() % 20 == 0) send(p, d, false);
				continue;
			}
			if (check(p, d)) {
				d.storyStep++;
				d.storyCount = 0;
				st.markDirty();
				if (d.storyStep >= steps(d.storyPath, d.storyMission).length) complete(p, d);
				else stepStarted(p, d);
				send(p, d, false);
			} else if (server.getTicks() % 20 == 0) {
				send(p, d, false);
			}
			if (d.storyMission == 2) mission2Tick(p, d);
		}
	}

	/** Is the current objective done? */
	private static boolean check(ServerPlayerEntity p, GhostData d) {
		BlockPos c = AmityPark.state(p.getServer()).center;
		boolean overworld = p.getWorld() == p.getServer().getOverworld();
		if (d.storyMission == 2) return switch (d.storyStep) {
			case 0 -> overworld && p.getPos().squaredDistanceTo(AmityPark.schoolDoor(c)) < 12 * 12;
			case 1 -> p.getInventory().contains(new ItemStack(ModItems.THERMOS));
			case 2 -> overworld && p.getPos().squaredDistanceTo(AmityPark.cafeteria(c)) < 9 * 9;
			case 3 -> d.storyCount > 0 || (overworld && !p.getServerWorld().getEntitiesByClass(ChefGhoulashEntity.class,
					new Box(BlockPos.ofFloored(AmityPark.cafeteria(c))).expand(64), e -> e.storyMode() && e.dazed()).isEmpty());
			default -> d.storyCount > 0;
		};
		return switch (d.storyStep) {
			case 0 -> overworld && p.getPos().squaredDistanceTo(AmityPark.labHouseDoor(c)) < 12 * 12;
			case 1 -> overworld && p.getPos().squaredDistanceTo(Vec3d.ofBottomCenter(AmityPark.lab(c))) < 9 * 9 && p.getY() < c.getY() - 3;
			default -> switch (d.storyPath) {
				case HERO -> d.storyStep == 2 ? d.powers : d.storyStep == 3 ? d.transformed : p.getAbilities().flying && d.transformed;
				case EVIL -> d.storyStep == 2 ? d.acneTicks > 0 || d.dark : d.storyStep == 3 ? d.dark && d.acneTicks <= 0 : d.dark && d.darkMode && d.transformed;
				case HUNTER -> d.storyStep == 2 ? p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.HUNTER_SUIT) || p.getInventory().contains(new ItemStack(ModItems.HUNTER_SUIT))
						: d.storyStep == 3 ? d.suitOn : d.storyCount >= 3;
				default -> false;
			};
		};
	}

	private static void stepStarted(ServerPlayerEntity p, GhostData d) {
		ServerWorld w = p.getServerWorld();
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.4f);
		p.sendMessage(Text.literal("New objective: " + objective(d)).formatted(Formatting.YELLOW), true);
		if (d.storyMission == 2 && d.storyStep == 3) spawnChef(p, true);
		if (d.storyMission == 1 && d.storyPath == HUNTER && d.storyStep == 4) {
			// the portal coughs up some ghosts for you to deal with
			for (int i = 0; i < 3; i++) {
				var g = ModEntities.HOSTILE_GHOST.create(w);
				if (g == null) continue;
				double a = i * Math.PI * 2 / 3;
				g.refreshPositionAndAngles(p.getX() + Math.cos(a) * 4, p.getY() + 1, p.getZ() + Math.sin(a) * 4, 0, 0);
				g.addCommandTag("phantom_story");
				w.spawnEntity(g);
				w.spawnParticles(GhostPowers.GREEN, g.getX(), g.getY() + 1, g.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
			}
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1f, 1.2f);
		}
	}

	private static void complete(ServerPlayerEntity p, GhostData d) {
		int mission = d.storyMission;
		ServerWorld w = p.getServerWorld();
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Mission Complete!").formatted(Formatting.GOLD, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(missionTitle(d.storyPath, mission).replace(" (Dark Side)", "")).formatted(Formatting.GRAY)));
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
		give(p, new ItemStack(ModItems.ECTOPLASM, 16));
		if (mission == 1) {
			switch (d.storyPath) {
				case HUNTER -> {
					give(p, new ItemStack(ModItems.SUIT_UPGRADE));
					give(p, new ItemStack(ModItems.ECTO_SWORD));
				}
				case EVIL -> give(p, new ItemStack(ModItems.ESSENCES[GhostPowers.ESS_SHADE]));
				default -> give(p, new ItemStack(ModItems.ESSENCES[GhostPowers.ESS_FRIENDLY], 2));
			}
		} else {
			give(p, new ItemStack(net.minecraft.item.Items.COOKED_BEEF, 8));   // leftovers
			switch (d.storyPath) {
				case HUNTER -> {
					give(p, new ItemStack(ModItems.GHOST_CORE, 2));
					give(p, new ItemStack(ModItems.SPECTRAL_SHARD, 2));
				}
				case EVIL -> {
					give(p, new ItemStack(ModItems.PHANTOM_HEART));
					give(p, new ItemStack(ModItems.SPECTRAL_SHARD, 2));
				}
				default -> {
					give(p, new ItemStack(ModItems.GHOST_CORE));
					give(p, new ItemStack(ModItems.ESSENCES[GhostPowers.ESS_FRIENDLY], 2));
				}
			}
		}
		if (mission < MISSIONS) {
			p.sendMessage(Text.literal("Mission complete! You got some rewards. The next mission starts in a moment...").formatted(Formatting.GOLD), false);
			d.storyMission = mission + 1;
			d.storyStep = 0;
			d.storyCount = 0;
			java.util.UUID id = p.getUuid();
			MinecraftServer server = p.getServer();
			com.phantompowers.entity.BossMoves.later(120, () -> {
				ServerPlayerEntity sp = server.getPlayerManager().getPlayer(id);
				if (sp != null) announce(sp, GhostState.of(server).data(id));
			});
		} else {
			d.storyMission = DONE;
			p.sendMessage(Text.literal("Mission complete! You got some rewards. More missions are coming in a future update.").formatted(Formatting.GOLD), false);
		}
	}

	private static void startMission(ServerPlayerEntity p, GhostData d, int mission) {
		d.storyMission = mission;
		d.storyStep = 0;
		d.storyCount = 0;
		announce(p, d);
	}

	/** Show a mission's title and intro. */
	private static void announce(ServerPlayerEntity p, GhostData d) {
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(missionTitle(d.storyPath, d.storyMission).replace(" (Dark Side)", ""))
				.formatted(d.storyPath == EVIL ? Formatting.LIGHT_PURPLE : Formatting.GREEN, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(objective(d)).formatted(Formatting.GRAY)));
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.2f);
		if (d.storyMission == 2) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "Ghost sensors are going crazy over at Amity High. Something in the cafeteria is VERY unhappy about the new healthy lunch menu...";
			case EVIL -> "Your new powers keep acting up on their own. Meanwhile, some ghost is throwing a tantrum in the Amity High cafeteria. Nobody haunts YOUR town but you.";
			default -> "Being half-ghost is harder than it looks: your powers keep going off by themselves! And today Amity High switched to a healthy lunch menu. Somebody is NOT happy about it...";
		}).formatted(Formatting.GRAY), false);
		send(p, d, false);
	}

	/** Mission 2 extras: powers glitching on the way to school, and keeping Chef Ghoulash around for the fight. */
	private static void mission2Tick(ServerPlayerEntity p, GhostData d) {
		ServerWorld w = p.getServerWorld();
		if (d.storyStep <= 1 && d.storyPath != HUNTER && w.random.nextInt(24) == 0) {
			// brand-new powers misfire: suddenly invisible, or floating up off the ground
			if (w.random.nextBoolean()) {
				p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.INVISIBILITY, 60, 0, false, false));
				p.sendMessage(Text.literal("Whoa! Your powers glitched and you turned invisible!").formatted(Formatting.AQUA), true);
			} else {
				p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.LEVITATION, 20, 0, false, false));
				p.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING, 80, 0, false, false));
				p.sendMessage(Text.literal("Whoa! Your powers glitched and you started floating!").formatted(Formatting.AQUA), true);
			}
			w.spawnParticles(GhostPowers.GREEN, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.8, 0.4, 0.04);
			w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
		}
		if (d.storyStep == 3 && d.storyCount == 0 && w.getServer().getTicks() % 40 == 0) spawnChef(p, false);
	}

	/** Chef Ghoulash bursts out of the cafeteria kitchen (unless she's already around). */
	private static void spawnChef(ServerPlayerEntity p, boolean announce) {
		ServerWorld w = p.getServerWorld();
		if (w != p.getServer().getOverworld()) return;
		BlockPos c = AmityPark.state(p.getServer()).center;
		Vec3d at = AmityPark.cafeteria(c);
		if (p.getPos().squaredDistanceTo(at) > 24 * 24) return;
		if (!w.getEntitiesByClass(ChefGhoulashEntity.class, new Box(BlockPos.ofFloored(at)).expand(64), ChefGhoulashEntity::storyMode).isEmpty()) return;
		ChefGhoulashEntity chef = ModEntities.CHEF_GHOULASH.create(w);
		if (chef == null) return;
		Vec3d kitchen = AmityPark.cafeteriaKitchen(c);
		chef.refreshPositionAndAngles(kitchen.x, kitchen.y + 0.5, kitchen.z, 90f, 0f);
		chef.addCommandTag(ChefGhoulashEntity.STORY_TAG);
		chef.setHome(BlockPos.ofFloored(at).up(), 13f);
		chef.setTarget(p);
		w.spawnEntity(chef);
		w.spawnParticles(GhostPowers.GREEN, chef.getX(), chef.getY() + 1, chef.getZ(), 80, 0.8, 1.2, 0.8, 0.1);
		w.playSound(null, chef.getX(), chef.getY(), chef.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 2f, 0.7f);
		if (announce) {
			p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
			p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Chef Ghoulash").formatted(Formatting.GREEN, Formatting.BOLD)));
			p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("\"WHO CHANGED MY MENU?!\"").formatted(Formatting.GRAY)));
		}
	}

	/** Something got sucked into a Thermos: if it was the story's Chef Ghoulash, everyone fighting her moves on. */
	public static void onTrapped(ServerWorld w, ServerPlayerEntity by, Entity ghost) {
		if (!(ghost instanceof ChefGhoulashEntity chef) || !chef.storyMode()) return;
		GhostState st = GhostState.of(w.getServer());
		for (ServerPlayerEntity p : w.getPlayers(sp -> sp == by || sp.squaredDistanceTo(ghost) < 48 * 48)) {
			GhostData d = st.data(p.getUuid());
			if (d.storyMission != 2 || d.storyStep < 3) continue;
			d.storyStep = 4;
			d.storyCount = 1;
			st.markDirty();
		}
		// her kitchen spirits fade away with her
		for (var g : w.getEntitiesByClass(com.phantompowers.entity.HostileGhostEntity.class, ghost.getBoundingBox().expand(40),
				e -> e.getCommandTags().contains("phantom_kitchen"))) {
			w.spawnParticles(net.minecraft.particle.ParticleTypes.SOUL, g.getX(), g.getY() + 1, g.getZ(), 12, 0.3, 0.5, 0.3, 0.03);
			g.discard();
		}
	}

	private static void give(ServerPlayerEntity p, ItemStack s) {
		if (!p.giveItemStack(s)) p.dropItem(s, false);
	}

	/** Ghost kills count for the hunter's "destroy 3 ghosts" objective. */
	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof AbstractGhostEntity)) return;
		Entity killer = source.getAttacker();
		if (!(killer instanceof ServerPlayerEntity p)) return;
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.storyPath == HUNTER && d.storyMission == 1 && d.storyStep == 4) {
			d.storyCount++;
			st.markDirty();
			p.sendMessage(Text.literal("Ghosts destroyed: " + Math.min(3, d.storyCount) + "/3").formatted(Formatting.AQUA), true);
			send(p, d, false);
		}
	}

	public static void send(ServerPlayerEntity p, GhostData d, boolean openPicker) {
		if (!ServerPlayNetworking.canSend(p, StoryPayload.ID)) return;
		String[] s = steps(d.storyPath, d.storyMission);
		String obj = objective(d);
		boolean travel = d.storyMission == 1 ? d.storyStep == 0 : d.storyMission == 2 && (d.storyStep == 0 || d.storyStep == 2);
		if (d.storyPath != NONE && d.storyMission < LEGACY_DONE && travel)
			obj += AmityPark.ready(p.getServer()) ? direction(p, d) : " (Amity Park is being built: " + AmityPark.progress(p.getServer()) + "%)";
		ServerPlayNetworking.send(p, new StoryPayload(openPicker, d.storyPath, missionTitle(d.storyPath, d.storyMission), obj,
				Math.min(d.storyStep, s.length), s.length));
	}
}
