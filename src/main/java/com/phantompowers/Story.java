package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
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
import net.minecraft.util.math.Vec3d;

/**
 * Story mode. Pick a path (half-ghost, evil half-ghost or ghost hunter) and play its missions, which start in
 * Amity Park. The path is yours alone: anyone can still use any item or power, but only your own path's objectives
 * move your story on. Restart the story (/story restart) to pick a different path.
 */
public final class Story {
	public static final int NONE = 0, HERO = 1, EVIL = 2, HUNTER = 3;
	public static final String[] PATHS = {"", "Half-Ghost", "Evil Half-Ghost", "Ghost Hunter"};
	private static final int DONE = 99;

	private Story() {}

	public static String missionTitle(int path, int mission) {
		if (path == NONE) return "";
		if (mission >= DONE) return "Mission complete!";
		return switch (path) {
			case HUNTER -> "Mission 1: Ghost Must Die";
			case EVIL -> "Mission 1: My Molecules Got All Rearranged (Dark Side)";
			default -> "Mission 1: My Molecules Got All Rearranged";
		};
	}

	/** The objectives of mission 1 for each path. */
	private static String[] steps(int path) {
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
		String[] s = steps(d.storyPath);
		if (d.storyMission >= DONE) return "More missions coming soon! (/story restart to play another path)";
		if (d.storyStep >= s.length) return "";
		String o = s[d.storyStep];
		if (d.storyPath == HUNTER && d.storyStep == 4) o += " (" + Math.min(3, d.storyCount) + "/3)";
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
	private static String direction(ServerPlayerEntity p) {
		BlockPos c = AmityPark.state(p.getServer()).center;
		if (p.getWorld() != p.getServer().getOverworld()) return " (it's in the Overworld)";
		Vec3d to = AmityPark.labHouseDoor(c).subtract(p.getPos());
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
			if (!AmityPark.request(server)) {   // the town is still going up (or this world never started it)
				if (server.getTicks() % 20 == 0) send(p, d, false);
				continue;
			}
			if (check(p, d)) {
				d.storyStep++;
				d.storyCount = 0;
				st.markDirty();
				if (d.storyStep >= steps(d.storyPath).length) complete(p, d);
				else stepStarted(p, d);
				send(p, d, false);
			} else if (server.getTicks() % 20 == 0) {
				send(p, d, false);
			}
		}
	}

	/** Is the current objective done? */
	private static boolean check(ServerPlayerEntity p, GhostData d) {
		BlockPos c = AmityPark.state(p.getServer()).center;
		boolean overworld = p.getWorld() == p.getServer().getOverworld();
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
		if (d.storyPath == HUNTER && d.storyStep == 4) {
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
		d.storyMission = DONE;
		ServerWorld w = p.getServerWorld();
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Mission Complete!").formatted(Formatting.GOLD, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(missionTitle(d.storyPath, 1).replace(" (Dark Side)", "")).formatted(Formatting.GRAY)));
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1f, 1f);
		give(p, new ItemStack(ModItems.ECTOPLASM, 16));
		switch (d.storyPath) {
			case HUNTER -> {
				give(p, new ItemStack(ModItems.SUIT_UPGRADE));
				give(p, new ItemStack(ModItems.ECTO_SWORD));
			}
			case EVIL -> give(p, new ItemStack(ModItems.ESSENCES[GhostPowers.ESS_SHADE]));
			default -> give(p, new ItemStack(ModItems.ESSENCES[GhostPowers.ESS_FRIENDLY], 2));
		}
		p.sendMessage(Text.literal("Mission complete! You got some rewards. More missions are coming in a future update.").formatted(Formatting.GOLD), false);
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
		if (d.storyPath == HUNTER && d.storyMission < DONE && d.storyStep == 4) {
			d.storyCount++;
			st.markDirty();
			p.sendMessage(Text.literal("Ghosts destroyed: " + Math.min(3, d.storyCount) + "/3").formatted(Formatting.AQUA), true);
			send(p, d, false);
		}
	}

	public static void send(ServerPlayerEntity p, GhostData d, boolean openPicker) {
		if (!ServerPlayNetworking.canSend(p, StoryPayload.ID)) return;
		String[] s = steps(d.storyPath);
		String obj = objective(d);
		if (d.storyPath != NONE && d.storyMission < DONE && d.storyStep == 0)
			obj += AmityPark.ready(p.getServer()) ? direction(p) : " (Amity Park is being built: " + AmityPark.progress(p.getServer()) + "%)";
		ServerPlayNetworking.send(p, new StoryPayload(openPicker, d.storyPath, missionTitle(d.storyPath, d.storyMission), obj,
				Math.min(d.storyStep, s.length), s.length));
	}
}
