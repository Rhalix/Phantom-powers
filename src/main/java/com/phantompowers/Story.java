package com.phantompowers;

import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.ChefGhoulashEntity;
import com.phantompowers.entity.RageWyvernEntity;
import com.phantompowers.network.StoryPayload;
import com.phantompowers.world.AmityPark;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import com.phantompowers.entity.TownspersonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
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
 * Story mode. Pick a path (half-ghost, evil half-ghost or ghost hunter) and play its missions in Amity Park.
 * Each mission is a list of objectives; each objective has a goal (go somewhere, use a power, beat a boss...)
 * that's checked twice a second. The path is yours alone: only your own path's objectives move your story on.
 * Restart the story (/story restart) to pick a different path.
 */
public final class Story {
	public static final int NONE = 0, HERO = 1, EVIL = 2, HUNTER = 3;
	public static final String[] PATHS = {"", "Half-Ghost", "Evil Half-Ghost", "Ghost Hunter"};
	/**
	 * storyMission is 1..MISSIONS while playing; 98 + n means "finished mission n and it was the last one at the time".
	 * When new missions are added, those players move straight on to the next one.
	 */
	private static final int MISSIONS = 4, LEGACY_DONE = 99, DONE = 98 + MISSIONS;
	/** Objective lists got longer in version 2: anyone part-way through a mission from before starts its objectives again. */
	private static final int VERSION = 2;

	// objective goals
	private static final String LAB_DOOR = "lab_door", LAB_FLOOR = "lab_floor", CONSOLES = "consoles", SCAN = "scan", POWERS = "powers",
			TRANSFORMED = "transformed", INTANGIBLE = "intangible", INVISIBLE = "invisible", FLY = "fly", KILL3 = "kill3", HUMAN = "human",
			PROTO = "proto", ACNE_GONE = "acne_gone", DARK = "dark", SUIT_ITEM = "suit_item", SUIT_ON = "suit_on", THERMOS = "thermos",
			TRAP_ANY = "trap_any", SCHOOL = "school", FRIEND = "friend", CAFETERIA = "cafeteria", CHEF1 = "chef1", CHEF_DAZED = "chef_dazed",
			CHEF_TRAPPED = "chef_trapped", AMULET = "amulet", TALK_PAIGE = "talk_paige", SNEAK_PAIGE = "sneak_paige", OVERSHADOW = "overshadow",
			TUNIC = "tunic", FOUNTAIN = "fountain", DANCE = "dance", WYVERN_DAZED = "wyvern_dazed", AMULET_PULLED = "amulet_pulled",
			CHECK_PAIGE = "check_paige", BOX = "box", ESCAPE = "escape", NAP = "nap", LIBRARY = "library", FOX = "fox",
			COLLECTOR_DAZED = "collector_dazed", COLLECTOR_TRAPPED = "collector_trapped", TEACHER = "teacher";
	private static final int SCAN_CHECKS = 20, DANCE_CHECKS = 40, NAP_CHECKS = 20, STUDY_CHECKS = 20;   // checks happen twice a second

	private Story() {}

	private record Step(String goal, String text) {}

	private static Step s(String goal, String text) {
		return new Step(goal, text);
	}

	public static String missionTitle(int path, int mission) {
		if (path == NONE) return "";
		if (mission >= LEGACY_DONE) return "Mission complete!";
		if (mission == 2) return path == EVIL ? "Mission 2: Lunch Rush (Dark Side)" : "Mission 2: Lunch Rush";
		if (mission == 3) return path == EVIL ? "Mission 3: Dance Disaster (Dark Side)" : "Mission 3: Dance Disaster";
		if (mission == 4) return path == EVIL ? "Mission 4: Rare Catch (Dark Side)" : "Mission 4: Rare Catch";
		return switch (path) {
			case HUNTER -> "Mission 1: Ghost Must Die";
			case EVIL -> "Mission 1: My Molecules Got All Rearranged (Dark Side)";
			default -> "Mission 1: My Molecules Got All Rearranged";
		};
	}

	/** The objectives of a mission for each path. */
	private static Step[] steps(int path, int mission) {
		if (mission == 1) return switch (path) {
			case HERO -> new Step[]{
					s(LAB_DOOR, "Head to the Ghost Lab house in Amity Park"),
					s(LAB_FLOOR, "Your parents' Ghost Portal is in the basement. Go down the stairs to the lab"),
					s(CONSOLES, "Read your parents' notes on the lab consoles (stand at the consoles)"),
					s(POWERS, "The portal doesn't work... Step inside the Ghost Portal and press the switch"),
					s(TRANSFORMED, "Something's different about you. Go ghost! (press V)"),
					s(INTANGIBLE, "Your molecules got rearranged! Try going intangible and pass through a wall"),
					s(INVISIBLE, "Now try turning invisible"),
					s(FLY, "Fly! (double-tap jump in ghost form)"),
					s(KILL3, "Ghosts came through the portal! Blast them back"),
					s(HUMAN, "Change back to human before anyone sees you (press V)")};
			case EVIL -> new Step[]{
					s(LAB_DOOR, "Head to the Ghost Lab house in Amity Park"),
					s(LAB_FLOOR, "Sneak down the stairs to the basement lab"),
					s(CONSOLES, "Read the notes about the prototype portal on the lab consoles"),
					s(PROTO, "Switch on the Prototype Portal (right-click it) and stand in front of it"),
					s(ACNE_GONE, "Wait for the ecto acne to wear off..."),
					s(TRANSFORMED, "Feel that power? Go ghost! (press V)"),
					s(INTANGIBLE, "Go intangible and slip through a wall"),
					s(FLY, "Fly! (double-tap jump in ghost form)"),
					s(KILL3, "Ghosts came through the prototype portal. Show them who's boss"),
					s(DARK, "Embrace the dark side: Dark Form (Ghost Skills) and go ghost")};
			case HUNTER -> new Step[]{
					s(LAB_DOOR, "Head to the Ghost Lab house in Amity Park"),
					s(LAB_FLOOR, "Go down the stairs to the basement lab"),
					s(CONSOLES, "Read the hunter suit blueprints on the lab consoles"),
					s(SUIT_ITEM, "Build your own Ghost Hunter Suit: craft it from 7 iron, 2 white wool and 1 ectoplasm (there's ectoplasm in the lab's gear locker)"),
					s(SUIT_ON, "Wear the suit (chest slot) and suit up! (press U)"),
					s(KILL3, "Ghosts broke out of the portal! Destroy them"),
					s(THERMOS, "Grab a Thermos from the lab's gear locker"),
					s(TRAP_ANY, "One more ghost slipped out! Trap it in the Thermos (right-click it)"),
					s(LAB_DOOR, "Report back: head outside the Ghost Lab house")};
			default -> new Step[0];
		};
		if (mission == 2) return switch (path) {
			case HERO -> new Step[]{
					s(SCHOOL, "Head to Amity High (your new powers keep glitching!)"),
					s(FRIEND, "Your friend Sam is starting a petition for a healthy lunch menu. Find her outside school and sign it"),
					s(CAFETERIA, "The petition worked! Check out the new healthy menu in the school cafeteria"),
					s(CHEF1, "Lunch Lady hates the new menu! Fight her"),
					s(THERMOS, "She got away! Get a Thermos from the Ghost Lab gear locker"),
					s(CAFETERIA, "She'll be back for lunch. Get back to the cafeteria"),
					s(CHEF_DAZED, "Round two! Fight Lunch Lady until she's dazed"),
					s(CHEF_TRAPPED, "Trap Lunch Lady in the Thermos (right-click her with it)"),
					s(FRIEND, "Tell Sam the cafeteria is safe again")};
			case EVIL -> new Step[]{
					s(SCHOOL, "Head to Amity High (your dark powers keep glitching!)"),
					s(FRIEND, "Sam is collecting signatures for a healthy-menu petition outside school. Go and talk to her"),
					s(CAFETERIA, "The menu changed and somebody's furious. Go and see in the school cafeteria"),
					s(CHEF1, "Some ghost is haunting YOUR school. Fight her"),
					s(THERMOS, "She ran off! Get a Thermos from the Ghost Lab gear locker"),
					s(CAFETERIA, "She'll be back for lunch. Wait for her in the cafeteria"),
					s(CHEF_DAZED, "Show Lunch Lady who's in charge: beat her until she's dazed"),
					s(CHEF_TRAPPED, "Trap her in the Thermos. She works for you now"),
					s(FRIEND, "Let Sam know there's a new boss in town")};
			case HUNTER -> new Step[]{
					s(SCHOOL, "Ghost activity at Amity High! Suit up and head there"),
					s(FRIEND, "Ask Sam outside school about the ghost sightings"),
					s(CAFETERIA, "She says the cafeteria's haunted since the menu changed. Investigate"),
					s(CHEF1, "Ghost! Fight Lunch Lady"),
					s(THERMOS, "She escaped! Get a Thermos from the Ghost Lab gear locker"),
					s(CAFETERIA, "Stake out the cafeteria: she'll be back for lunch"),
					s(CHEF_DAZED, "Fight Lunch Lady until she's dazed"),
					s(CHEF_TRAPPED, "Trap Lunch Lady in the Thermos (right-click her with it)"),
					s(FRIEND, "Let Sam know the ghost is caught")};
			default -> new Step[0];
		};
		if (mission == 4) return switch (path) {
			case HERO -> new Step[]{
					s(BOX, "It's late, and the Box Ghost is wrecking the Quick Stop! Stop him (beat him, or trap him in the Thermos)"),
					s(ESCAPE, "Something's been watching you... Slucker! He's too strong to fight. Get back to the Ghost Lab house!"),
					s(NAP, "You were up all night. Take a nap on the couch in the Ghost Lab house (sit on it)"),
					s(FRIEND, "You flunked your biology test! Find Sam outside Amity High"),
					s(LIBRARY, "Sam says do an extra-credit report on a rare animal. Read up in the library (stay inside a while)"),
					s(FOX, "Observe the rare snow fox in the park for your report (stay close to it)"),
					s(COLLECTOR_DAZED, "Slucker stole Sam's tablet to upgrade his gear, and now he wants the fox AND you! Fight him until he's dazed"),
					s(COLLECTOR_TRAPPED, "Trap the Slucker in the Thermos (right-click him with it)"),
					s(TEACHER, "Hand your report in to Mr. Lancer outside Amity High")};
			case EVIL -> new Step[]{
					s(BOX, "The Box Ghost is making a mess of YOUR town at the Quick Stop. Teach him a lesson (beat him or trap him)"),
					s(ESCAPE, "A ghost hunter is stalking you: the Slucker! Not now. Get back to the Ghost Lab house!"),
					s(NAP, "Even villains need sleep. Nap on the couch in the Ghost Lab house (sit on it)"),
					s(FRIEND, "You flunked biology. Annoying. Find Sam outside Amity High"),
					s(LIBRARY, "Extra credit, ugh. Read up on rare animals in the library (stay inside a while)"),
					s(FOX, "Find the rare snow fox in the park and study it (stay close to it)"),
					s(COLLECTOR_DAZED, "Slucker stole Sam's tablet and upgraded his gear. Nobody collects YOU. Beat him until he's dazed"),
					s(COLLECTOR_TRAPPED, "Trap the Slucker in the Thermos. Your trophy now"),
					s(TEACHER, "Hand your report in to Mr. Lancer outside Amity High")};
			case HUNTER -> new Step[]{
					s(BOX, "Night patrol: the Box Ghost is wrecking the Quick Stop! Stop him (beat him, or trap him in the Thermos)"),
					s(ESCAPE, "Ghost readings off the charts... the Slucker wants your suit for his collection! Fall back to the Ghost Lab house!"),
					s(NAP, "You've been on patrol all night. Rest on the couch in the Ghost Lab house (sit on it)"),
					s(FRIEND, "You flunked your biology test! Find Sam outside Amity High"),
					s(LIBRARY, "Sam suggests an extra-credit report on a rare animal. Read up in the library (stay inside a while)"),
					s(FOX, "Observe the rare snow fox in the park for your report (stay close to it)"),
					s(COLLECTOR_DAZED, "Slucker stole Sam's tablet to upgrade his gear and he's hunting the fox! Fight him until he's dazed"),
					s(COLLECTOR_TRAPPED, "Trap the Slucker in the Thermos (right-click him with it)"),
					s(TEACHER, "Hand your report in to Mr. Lancer outside Amity High")};
			default -> new Step[0];
		};
		if (mission == 3) return switch (path) {
			case HERO -> new Step[]{
					s(AMULET, "Something is glowing by the park pond. Go and find it"),
					s(TALK_PAIGE, "It'd make a great gift... Find Princess Dorathea in the park and ask her to the dance"),
					s(OVERSHADOW, "Your powers got you in trouble at school! Overshadow the teacher outside Amity High to talk your way out of it"),
					s(TUNIC, "Get dressed up for the dance: wear a leather tunic (any colour)"),
					s(FOUNTAIN, "Go to the dance at the park fountain (bring the amulet as a gift)"),
					s(DANCE, "Dance! Stay on the dance floor by the fountain"),
					s(WYVERN_DAZED, "The amulet turned Princess Dorathea into a Princess Dorathea! Fight it until it's dazed"),
					s(AMULET_PULLED, "Pull the amulet off the dazed wyvern (right-click it)"),
					s(CHECK_PAIGE, "Check that Princess Dorathea is okay")};
			case EVIL -> new Step[]{
					s(AMULET, "Something full of rage is glowing by the park pond. Go and take it"),
					s(SNEAK_PAIGE, "Princess Dorathea is in the park. Turn invisible and sneak up on her"),
					s(OVERSHADOW, "Overshadow the teacher outside Amity High and get yourself out of detention"),
					s(TUNIC, "Crash the dance in style: wear a leather tunic (any colour)"),
					s(FOUNTAIN, "Crash the dance at the park fountain (bring the amulet)"),
					s(DANCE, "Make a scene: stay on the dance floor by the fountain"),
					s(WYVERN_DAZED, "The amulet's rage got loose and made a Princess Dorathea! Beat it until it's dazed"),
					s(AMULET_PULLED, "Rip the amulet off the wyvern (right-click it). Its power is yours now"),
					s(CHECK_PAIGE, "Go and see what's left of Princess Dorathea")};
			case HUNTER -> new Step[]{
					s(AMULET, "Strange ghost readings by the park pond. Find what's causing them"),
					s(LAB_FLOOR, "Take the amulet to the Ghost Lab basement"),
					s(SCAN, "Scan the amulet at the lab consoles (stay there while it scans)"),
					s(TUNIC, "You're chaperoning the dance tonight. Wear a leather tunic (any colour)"),
					s(FOUNTAIN, "Chaperone the dance at the park fountain (bring the amulet)"),
					s(DANCE, "Keep watch over the dance floor by the fountain"),
					s(WYVERN_DAZED, "A Princess Dorathea is attacking the dance! Fight it until it's dazed"),
					s(AMULET_PULLED, "Pull the amulet off the dazed wyvern (right-click it)"),
					s(CHECK_PAIGE, "Check that Princess Dorathea is okay")};
			default -> new Step[0];
		};
		return new Step[0];
	}

	private static Step current(GhostData d) {
		Step[] all = steps(d.storyPath, d.storyMission);
		return d.storyStep >= 0 && d.storyStep < all.length ? all[d.storyStep] : null;
	}

	private static String goal(GhostData d) {
		Step st = current(d);
		return st == null ? "" : st.goal();
	}

	public static String objective(GhostData d) {
		if (d.storyMission >= LEGACY_DONE) return "More missions coming soon! (/story restart to play another path)";
		Step st = current(d);
		if (st == null) return "";
		String o = st.text();
		switch (st.goal()) {
			case KILL3 -> o += " (" + Math.min(3, d.storyCount) + "/3)";
			case SCAN -> o += " (" + Math.min(100, d.storyCount * 100 / SCAN_CHECKS) + "%)";
			case LIBRARY, FOX -> o += " (" + Math.min(100, d.storyCount * 100 / STUDY_CHECKS) + "%)";
			case NAP -> o += " (" + Math.max(0, (NAP_CHECKS - d.storyCount) / 2) + "s)";
			case DANCE -> o += " (" + Math.max(0, (DANCE_CHECKS - d.storyCount) / 2) + "s left)";
			default -> {}
		}
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
		d.storyVersion = VERSION;
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
		d.storyVersion = VERSION;
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

	/** Where the current objective wants you to go (for the tracker), or null. */
	private static Vec3d destination(GhostData d, BlockPos c) {
		return switch (goal(d)) {
			case LAB_DOOR, LAB_FLOOR, CONSOLES, SCAN, THERMOS -> AmityPark.labHouseDoor(c);
			case SCHOOL, FRIEND, OVERSHADOW -> AmityPark.schoolDoor(c);
			case CAFETERIA -> AmityPark.cafeteria(c);
			case AMULET -> AmityPark.parkPond(c);
			case TALK_PAIGE, SNEAK_PAIGE -> AmityPark.parkGate(c);
			case FOUNTAIN, DANCE, CHECK_PAIGE -> AmityPark.parkFountain(c);
			case BOX -> AmityPark.store(c);
			case ESCAPE, NAP -> AmityPark.labHouseDoor(c);
			case LIBRARY -> AmityPark.library(c);
			case FOX, COLLECTOR_DAZED -> AmityPark.parkMeadow(c);
			case TEACHER -> AmityPark.schoolDoor(c);
			default -> null;
		};
	}

	/** "(312 blocks north-east)" toward the current objective. */
	private static String direction(ServerPlayerEntity p, GhostData d) {
		BlockPos c = AmityPark.state(p.getServer()).center;
		Vec3d goal = destination(d, c);
		if (goal == null) return "";
		if (p.getWorld() != p.getServer().getOverworld()) return " (it's in the Overworld)";
		Vec3d to = goal.subtract(p.getPos());
		int dist = (int) Math.sqrt(to.x * to.x + to.z * to.z);
		if (dist < 20) return "";
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
			if (d.storyMission >= LEGACY_DONE) {   // finished what was the last mission at the time: on to the next one
				d.storyVersion = VERSION;
				startMission(p, d, d.storyMission - 98 + 1);
				st.markDirty();
				continue;
			}
			if (d.storyVersion < VERSION) {
				d.storyVersion = VERSION;
				if (d.storyStep > 0) {
					d.storyStep = 0;
					d.storyCount = 0;
					p.sendMessage(Text.literal("Story missions are longer now, with more from the show! Your current mission's objectives start again.")
							.formatted(Formatting.YELLOW), false);
				}
				st.markDirty();
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
			if (d.storyMission >= 1 && d.storyMission <= MISSIONS) upkeep(p, d, st);
		}
	}

	private static boolean has(ServerPlayerEntity p, net.minecraft.item.Item item) {
		return p.getInventory().contains(new ItemStack(item));
	}

	private static boolean near(ServerPlayerEntity p, Vec3d at, double r) {
		return p.getWorld() == p.getServer().getOverworld() && p.getPos().squaredDistanceTo(at) < r * r;
	}

	/** Is the current objective done? */
	private static boolean check(ServerPlayerEntity p, GhostData d) {
		BlockPos c = AmityPark.state(p.getServer()).center;
		ServerWorld w = p.getServerWorld();
		return switch (goal(d)) {
			case LAB_DOOR -> near(p, AmityPark.labHouseDoor(c), 12);
			case LAB_FLOOR -> near(p, Vec3d.ofBottomCenter(AmityPark.lab(c)), 10) && p.getY() < c.getY() - 3;
			case CONSOLES -> near(p, AmityPark.labConsoles(c), 3.5);
			case SCAN -> d.storyCount >= SCAN_CHECKS;
			case POWERS -> d.powers;
			case TRANSFORMED -> d.transformed;
			case INTANGIBLE -> d.intangible;
			case INVISIBLE -> d.invisible;
			case FLY -> p.getAbilities().flying && d.transformed;
			case KILL3 -> d.storyCount >= 3;
			case HUMAN -> !d.transformed;
			case PROTO -> d.acneTicks > 0 || d.dark;
			case ACNE_GONE -> d.dark && d.acneTicks <= 0;
			case DARK -> d.dark && d.darkMode && d.transformed;
			case SUIT_ITEM -> p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.HUNTER_SUIT) || has(p, ModItems.HUNTER_SUIT);
			case SUIT_ON -> d.suitOn;
			case THERMOS -> has(p, ModItems.THERMOS);
			case TRAP_ANY, CHEF1, CHEF_TRAPPED, AMULET_PULLED -> d.storyCount > 0;
			case SCHOOL -> near(p, AmityPark.schoolDoor(c), 12);
			case FRIEND -> npcNear(w, p, "phantom_story_friend", 3.5);
			case CAFETERIA -> near(p, AmityPark.cafeteria(c), 9);
			case CHEF_DAZED -> d.storyCount > 0 || (near(p, AmityPark.cafeteria(c), 64) && !w.getEntitiesByClass(ChefGhoulashEntity.class,
					new Box(BlockPos.ofFloored(AmityPark.cafeteria(c))).expand(64), e -> e.storyMode() && e.dazed()).isEmpty());
			case AMULET -> has(p, ModItems.RAGE_AMULET);
			case TALK_PAIGE -> npcNear(w, p, "phantom_paige_park", 3.5);
			case SNEAK_PAIGE -> d.invisible && npcNear(w, p, "phantom_paige_park", 4);
			case OVERSHADOW -> d.possessing != null && d.possessing.getCommandTags().contains("phantom_story_teacher");
			case TUNIC -> p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.LEATHER_CHESTPLATE);
			case FOUNTAIN -> near(p, AmityPark.parkFountain(c), 9) && has(p, ModItems.RAGE_AMULET);
			case DANCE -> d.storyCount >= DANCE_CHECKS;
			case WYVERN_DAZED -> d.storyCount > 0 || (near(p, AmityPark.parkFountain(c), 64) && !w.getEntitiesByClass(RageWyvernEntity.class,
					new Box(BlockPos.ofFloored(AmityPark.parkFountain(c))).expand(64), e -> e.storyMode() && e.dazed()).isEmpty());
			case CHECK_PAIGE -> npcNear(w, p, "phantom_paige_saved", 4);
			case BOX, COLLECTOR_TRAPPED -> d.storyCount > 0;
			case ESCAPE -> near(p, AmityPark.labHouseDoor(c), 10);
			case NAP -> d.storyCount >= NAP_CHECKS || p.isSleeping();
			case LIBRARY, FOX -> d.storyCount >= STUDY_CHECKS;
			case COLLECTOR_DAZED -> d.storyCount > 0 || (near(p, AmityPark.parkMeadow(c), 80) && !w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class,
					new Box(BlockPos.ofFloored(AmityPark.parkMeadow(c))).expand(80), e -> e.storyMode() && !e.chasing() && e.dazed()).isEmpty());
			case TEACHER -> npcNear(w, p, "phantom_story_teacher", 3.5);
			default -> false;
		};
	}

	private static boolean npcNear(ServerWorld w, ServerPlayerEntity p, String tag, double r) {
		if (w != p.getServer().getOverworld()) return false;
		return !w.getEntitiesByClass(TownspersonEntity.class, p.getBoundingBox().expand(r), v -> v.getCommandTags().contains(tag)).isEmpty();
	}

	/** A new objective just started: set the scene. */
	private static void stepStarted(ServerPlayerEntity p, GhostData d) {
		ServerWorld w = p.getServerWorld();
		BlockPos c = AmityPark.state(p.getServer()).center;
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.8f, 1.4f);
		p.sendMessage(Text.literal("New objective: " + objective(d)).formatted(Formatting.YELLOW), true);
		switch (goal(d)) {
			case INTANGIBLE -> learn(p, d, GhostPowers.INTANGIBLE, "Intangibility", "Use it to walk through walls.");
			case INVISIBLE -> learn(p, d, GhostPowers.INVISIBLE, "Invisibility", "Nobody can see you while it's on.");
			case OVERSHADOW -> {
				learn(p, d, GhostPowers.POSSESS, "Overshadowing (Possession)", "Go ghost, look at someone and use it to take them over.");
				spawnTeacher(w, c);
			}
			case KILL3 -> spawnStoryGhosts(p, 3, "phantom_story");
			case TRAP_ANY -> spawnStoryGhosts(p, 1, "phantom_story_trap");
			case FRIEND -> spawnFriend(w, c);
			case CHEF1 -> spawnChef(p, true, true);
			case CHEF_DAZED -> spawnChef(p, true, false);
			case TALK_PAIGE, SNEAK_PAIGE -> spawnPaigeInPark(w, c);
			case FOUNTAIN -> spawnDance(w, c);
			case WYVERN_DAZED -> startWyvern(p, c);
			case BOX -> spawnBoxGhost(p, c);
			case ESCAPE -> spawnCollector(p, c, true);
			case NAP -> callOffChase(w, p);
			case FOX -> spawnFox(w, c);
			case COLLECTOR_DAZED -> {
				p.sendMessage(Text.literal("Sam: \"My tablet! It's gone! I had your whole schedule on it...\"").formatted(Formatting.LIGHT_PURPLE), false);
				spawnCollector(p, c, false);
			}
			case TEACHER -> spawnTeacher(w, c);
			case SCAN -> p.sendMessage(Text.literal("The amulet hums and gets warmer whenever someone nearby is angry...").formatted(Formatting.GRAY), false);
			case HUMAN -> p.sendMessage(Text.literal("Half human, half ghost... This is going to take some getting used to.").formatted(Formatting.GRAY), false);
			default -> {}
		}
	}

	/** Unlock a power for the story (if you didn't have it yet) and put it on a Power Slot. */
	private static void learn(ServerPlayerEntity p, GhostData d, int skill, String name, String how) {
		if (d.has(skill) && Slots.slotOf(d, skill) >= 0) return;
		d.unlocked |= 1 << skill;
		Slots.autoAdd(d, skill);
		if (Slots.slotOf(d, skill) < 0) {
			d.slots[Slots.COUNT - 1] = skill;
			Slots.ensure(d);
		}
		GhostPowers.syncNow(p);
		p.sendMessage(Text.literal("You discovered a new power: " + name + "! It's on Power Slot " + (Slots.slotOf(d, skill) + 1) + ". " + how)
				.formatted(Formatting.GREEN, Formatting.BOLD), false);
	}

	/** Every half second while a mission is running: keep the scene in place, count timers, glitch powers... */
	private static void upkeep(ServerPlayerEntity p, GhostData d, GhostState st) {
		ServerWorld w = p.getServerWorld();
		if (w != p.getServer().getOverworld() || !AmityPark.ready(p.getServer())) return;
		BlockPos c = AmityPark.state(p.getServer()).center;
		long t = w.getServer().getTicks();
		String g = goal(d);
		// mission 2: brand-new powers misfire on the way to school
		if (d.storyMission == 2 && d.storyStep <= 2 && d.storyPath != HUNTER && w.random.nextInt(24) == 0) glitch(p, w);
		switch (g) {
			case KILL3 -> {
				if (t % 40 == 0 && d.storyCount < 3 && w.getEntitiesByClass(com.phantompowers.entity.HostileGhostEntity.class, p.getBoundingBox().expand(40),
						e -> e.getCommandTags().contains("phantom_story")).isEmpty()) spawnStoryGhosts(p, 3 - d.storyCount, "phantom_story");
			}
			case TRAP_ANY -> {
				if (t % 40 == 0 && w.getEntitiesByClass(com.phantompowers.entity.HostileGhostEntity.class, p.getBoundingBox().expand(40),
						e -> e.getCommandTags().contains("phantom_story_trap")).isEmpty()) spawnStoryGhosts(p, 1, "phantom_story_trap");
			}
			case SCAN -> {
				if (near(p, AmityPark.labConsoles(c), 4) && has(p, ModItems.RAGE_AMULET)) {
					d.storyCount++;
					st.markDirty();
					Vec3d at = AmityPark.labConsoles(c);
					w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 1.2, at.z - 1.2, 4, 1.5, 0.3, 0.2, 0.02);
					if (d.storyCount % 4 == 0) w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.BLOCKS, 0.8f, 1.6f);
					if (d.storyCount == SCAN_CHECKS)
						p.sendMessage(Text.literal("Scan complete: a ghost artifact that feeds on anger. Whoever wears it and loses their temper... changes.").formatted(Formatting.AQUA), false);
				} else if (t % 40 == 0 && near(p, AmityPark.labConsoles(c), 12) && !has(p, ModItems.RAGE_AMULET))
					p.sendMessage(Text.literal("You need the amulet to scan it! (Lost it? It's back by the park pond.)").formatted(Formatting.YELLOW), true);
			}
			case FRIEND -> {
				if (t % 60 == 0) spawnFriend(w, c);
			}
			case CHEF1, CHEF_DAZED -> {
				if (t % 40 == 0 && d.storyCount == 0) spawnChef(p, false, g.equals(CHEF1));
			}
			case AMULET -> {
				Vec3d at = AmityPark.parkPond(c);
				if (t % 40 == 0) placeAmulet(w, p, at);
				if (near(p, at, 40)) w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.6, at.z, 3, 0.2, 0.3, 0.2, 0.01);
			}
			case TALK_PAIGE, SNEAK_PAIGE -> {
				if (t % 60 == 0) spawnPaigeInPark(w, c);
			}
			case OVERSHADOW -> {
				if (t % 60 == 0) spawnTeacher(w, c);
			}
			case FOUNTAIN, DANCE -> {
				Vec3d f = AmityPark.parkFountain(c);
				if (near(p, f, 48)) {
					if (t % 20 == 0) spawnDance(w, c);
					for (int i = 0; i < 3; i++) {
						double a = w.random.nextDouble() * Math.PI * 2;
						w.spawnParticles(ParticleTypes.NOTE, f.x + Math.cos(a) * 7, f.y + 2.2, f.z + Math.sin(a) * 7, 0, w.random.nextDouble(), 0, 0, 1);
					}
				}
				if (g.equals(DANCE)) {
					if (near(p, f, 9)) {
						d.storyCount++;
						st.markDirty();
						if (d.storyCount % 8 == 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.RECORDS, 0.8f, 0.8f + w.random.nextFloat() * 0.6f);
						if (d.storyCount == DANCE_CHECKS / 2) danceLine(p, d);
					} else if (t % 40 == 0) p.sendMessage(Text.literal("Get back on the dance floor by the fountain!").formatted(Formatting.YELLOW), true);
				}
				if (t % 40 == 0 && !has(p, ModItems.RAGE_AMULET)) {
					placeAmulet(w, p, AmityPark.parkPond(c));
					if (near(p, f, 16)) p.sendMessage(Text.literal("You need the amulet you found! (Lost it? It's back by the park pond.)").formatted(Formatting.YELLOW), true);
				}
			}
			case WYVERN_DAZED -> {
				if (t % 40 == 0 && d.storyCount == 0 && near(p, AmityPark.parkFountain(c), 40)
						&& w.getEntitiesByClass(RageWyvernEntity.class, new Box(BlockPos.ofFloored(AmityPark.parkFountain(c))).expand(80), RageWyvernEntity::storyMode).isEmpty())
					spawnWyvern(w, c, p, AmityPark.parkFountain(c).add(4, 3, 0));
			}
			case BOX -> {
				if (t % 40 == 0 && d.storyCount == 0 && near(p, AmityPark.store(c), 40) && w.getEntitiesByClass(com.phantompowers.entity.CrateKingEntity.class,
						new Box(BlockPos.ofFloored(AmityPark.store(c))).expand(64), e -> e.getCommandTags().contains("phantom_story_box")).isEmpty()) spawnBoxGhost(p, c);
			}
			case ESCAPE -> {
				if (t % 40 == 0 && w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class, p.getBoundingBox().expand(64), com.phantompowers.entity.CollectorEntity::chasing).isEmpty())
					spawnCollector(p, c, true);
			}
			case NAP -> {
				if (p.getVehicle() instanceof com.phantompowers.entity.SeatEntity && w.getBlockState(p.getVehicle().getBlockPos()).getBlock() instanceof SofaBlock) {
					d.storyCount++;
					st.markDirty();
					if (d.storyCount % 6 == 0) w.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.6, p.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
					if (d.storyCount == NAP_CHECKS) p.sendMessage(Text.literal("Zzz... You wake up feeling a lot better. Wait, the biology test was TODAY?!").formatted(Formatting.GRAY), false);
				}
			}
			case TEACHER -> {
				if (t % 60 == 0) spawnTeacher(w, c);
			}
			case LIBRARY -> {
				if (near(p, AmityPark.library(c), 9)) {
					d.storyCount++;
					st.markDirty();
					if (d.storyCount % 4 == 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1f);
					if (d.storyCount == STUDY_CHECKS) p.sendMessage(Text.literal("You read all about the rare snow fox. Somebody spotted one in the park!").formatted(Formatting.AQUA), false);
				}
			}
			case FOX -> {
				var foxes = w.getEntitiesByClass(net.minecraft.entity.passive.FoxEntity.class, new Box(BlockPos.ofFloored(AmityPark.parkMeadow(c))).expand(80),
						f -> f.getCommandTags().contains("phantom_story_fox"));
				if (foxes.isEmpty()) {
					if (t % 40 == 0) spawnFox(w, c);
				} else if (p.squaredDistanceTo(foxes.get(0)) < 7 * 7) {
					d.storyCount++;
					st.markDirty();
					if (d.storyCount == STUDY_CHECKS) p.sendMessage(Text.literal("Great notes! ...Uh oh. Something else is interested in the fox.").formatted(Formatting.AQUA), false);
				}
			}
			case COLLECTOR_DAZED -> {
				if (t % 40 == 0 && d.storyCount == 0 && near(p, AmityPark.parkMeadow(c), 48) && w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class,
						new Box(BlockPos.ofFloored(AmityPark.parkMeadow(c))).expand(80), e -> e.storyMode() && !e.chasing()).isEmpty()) spawnCollector(p, c, false);
			}
			case CHECK_PAIGE -> {
				if (t % 60 == 0 && w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(AmityPark.parkFountain(c))).expand(48),
						v -> v.getCommandTags().contains("phantom_paige_saved")).isEmpty()) {
					spawnNpc(w, AmityPark.parkFountain(c).add(3, 0, 3), "Princess Dorathea", "phantom_paige_saved", true, TownspersonEntity.PAIGE);
				}
			}
			default -> {}
		}
	}

	private static void danceLine(ServerPlayerEntity p, GhostData d) {
		String line = switch (d.storyPath) {
			case HUNTER -> "All quiet so far... but the amulet in your pocket is getting hotter.";
			case EVIL -> "Everyone's staring at you. Good.";
			default -> "Princess Dorathea: \"You're actually a pretty good dancer!\"";
		};
		p.sendMessage(Text.literal(line).formatted(Formatting.LIGHT_PURPLE), false);
	}

	private static void glitch(ServerPlayerEntity p, ServerWorld w) {
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

	// ------------------------------------------------------------ scenery

	private static void spawnStoryGhosts(ServerPlayerEntity p, int n, String tag) {
		ServerWorld w = p.getServerWorld();
		for (int i = 0; i < n; i++) {
			var g = ModEntities.HOSTILE_GHOST.create(w);
			if (g == null) continue;
			double a = i * Math.PI * 2 / Math.max(1, n) + w.random.nextDouble();
			g.refreshPositionAndAngles(p.getX() + Math.cos(a) * 4, p.getY() + 1, p.getZ() + Math.sin(a) * 4, 0, 0);
			g.addCommandTag(tag);
			if (tag.equals("phantom_story_trap")) g.setCustomName(Text.literal("Escaped Ghost"));
			w.spawnEntity(g);
			w.spawnParticles(GhostPowers.GREEN, g.getX(), g.getY() + 1, g.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1f, 1.2f);
	}

	/** A story character (standing still until the story lets them move). Only one per tag. */
	private static TownspersonEntity spawnNpc(ServerWorld w, Vec3d at, String name, String tag, boolean showName, int look) {
		if (!w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(at)).expand(64), v -> v.getCommandTags().contains(tag)).isEmpty()) return null;
		var v = ModEntities.TOWNSPERSON.create(w);
		if (v == null) return null;
		v.setVariant(look);
		v.refreshPositionAndAngles(at.x, at.y, at.z, w.random.nextFloat() * 360f, 0f);
		v.setCustomName(Text.literal(name));
		v.setCustomNameVisible(showName);
		v.setPersistent();
		v.setAiDisabled(true);
		v.addCommandTag(tag);
		v.addCommandTag("phantom_story_npc");
		w.spawnEntity(v);
		return v;
	}

	private static void spawnFriend(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.schoolDoor(c).add(-4, 0, 1.5), "Sam (your friend)", "phantom_story_friend", true, TownspersonEntity.LENA);
	}

	private static void spawnTeacher(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.schoolDoor(c).add(0, 0, 2), "Mr. Lancer (teacher)", "phantom_story_teacher", true, TownspersonEntity.PEMBERTON);
	}

	private static void spawnPaigeInPark(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.parkGate(c), "Princess Dorathea", "phantom_paige_park", true, TownspersonEntity.PAIGE);
	}

	/** The glowing amulet waiting by the pond (unless you've got it, or it's already there). */
	private static void placeAmulet(ServerWorld w, ServerPlayerEntity p, Vec3d at) {
		if (has(p, ModItems.RAGE_AMULET) || !near(p, at, 64)) return;
		if (!w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, new Box(BlockPos.ofFloored(at)).expand(6), e -> e.getStack().isOf(ModItems.RAGE_AMULET)).isEmpty()) return;
		var item = new net.minecraft.entity.ItemEntity(w, at.x, at.y + 0.3, at.z, new ItemStack(ModItems.RAGE_AMULET), 0, 0, 0);
		item.setNeverDespawn();
		item.setGlowing(true);
		w.spawnEntity(item);
	}

	/** The dance around the park fountain: your date and a few classmates. */
	private static void spawnDance(ServerWorld w, BlockPos c) {
		Vec3d f = AmityPark.parkFountain(c);
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(f)).expand(64), v -> v.getCommandTags().contains("phantom_paige_park"))) v.discard();
		if (!w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(f)).expand(24), v -> v.getCommandTags().contains("phantom_dance")).isEmpty()) return;
		int[] looks = {TownspersonEntity.PAIGE, TownspersonEntity.MARCUS, TownspersonEntity.TIFFANY, TownspersonEntity.LEON, TownspersonEntity.JASMINE, TownspersonEntity.OWEN};
		for (int i = 0; i < looks.length; i++) {
			var v = ModEntities.TOWNSPERSON.create(w);
			if (v == null) continue;
			v.setVariant(looks[i]);
			v.setDancing(true);
			String[] names = TownspersonEntity.NAMES;
			double a = i * Math.PI * 2 / looks.length;
			v.refreshPositionAndAngles(f.x + Math.cos(a) * 7, f.y, f.z + Math.sin(a) * 7, (float) Math.toDegrees(a) + 90f, 0f);
			v.setCustomName(Text.literal(names[looks[i]]));
			v.setCustomNameVisible(i == 0);
			v.setPersistent();
			v.setAiDisabled(true);
			v.addCommandTag("phantom_dance");
			v.addCommandTag("phantom_story_npc");
			if (i == 0) v.addCommandTag("phantom_dance_date");
			w.spawnEntity(v);
		}
	}

	/** You hand over the amulet... and Paige loses her temper. */
	private static void startWyvern(ServerPlayerEntity p, BlockPos c) {
		ServerWorld w = p.getServerWorld();
		removeOne(p, ModItems.RAGE_AMULET);
		Vec3d at = AmityPark.parkFountain(c).add(4, 1, 0);
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(AmityPark.parkFountain(c))).expand(24),
				v -> v.getCommandTags().contains("phantom_dance_date"))) {
			at = v.getPos();
			v.discard();
		}
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		switch (d.storyPath) {
			case HUNTER -> {
				p.sendMessage(Text.literal("Princess Dorathea spots the amulet sticking out of your pocket and grabs it. \"Ooh, pretty!\"").formatted(Formatting.GRAY), false);
				p.sendMessage(Text.literal("Princess Dorathea: \"Hey! Give that back? It's MINE now! Don't tell me what to do!\"").formatted(Formatting.LIGHT_PURPLE), false);
			}
			case EVIL -> {
				p.sendMessage(Text.literal("You toss the amulet to Princess Dorathea, just to see what happens.").formatted(Formatting.GRAY), false);
				p.sendMessage(Text.literal("Princess Dorathea: \"What is this? Is this a JOKE? I am so SICK of you!\"").formatted(Formatting.LIGHT_PURPLE), false);
			}
			default -> {
				p.sendMessage(Text.literal("You give Princess Dorathea the amulet. She puts it on...").formatted(Formatting.GRAY), false);
				p.sendMessage(Text.literal("Princess Dorathea: \"It's beautiful! ...Wait. You stepped on my foot. AGAIN!\"").formatted(Formatting.LIGHT_PURPLE), false);
			}
		}
		p.sendMessage(Text.literal("The amulet flares red as she gets angrier and angrier...").formatted(Formatting.RED, Formatting.ITALIC), false);
		spawnWyvern(w, c, p, at);
	}

	private static void spawnWyvern(ServerWorld w, BlockPos c, ServerPlayerEntity p, Vec3d at) {
		RageWyvernEntity wy = ModEntities.RAGE_WYVERN.create(w);
		if (wy == null) return;
		wy.refreshPositionAndAngles(at.x, at.y + 1, at.z, 0f, 0f);
		wy.addCommandTag(RageWyvernEntity.STORY_TAG);
		wy.setWearer("Princess Dorathea");
		wy.setHome(BlockPos.ofFloored(AmityPark.parkFountain(c)).up(4), 26f);
		wy.setTarget(p);
		w.spawnEntity(wy);
		w.spawnParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1, at.z, 120, 1.2, 1.5, 1.2, 0.15);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.5f, 1.1f);
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Princess Dorathea").formatted(Formatting.DARK_AQUA, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("\"NOBODY TELLS ME WHAT TO DO!\"").formatted(Formatting.GRAY)));
		// the other dancers run for it
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(at)).expand(24),
				v -> v.getCommandTags().contains("phantom_dance") && !v.getCommandTags().contains("phantom_dance_date"))) {
			v.setDancing(false);
			v.setAiDisabled(false);
		}
	}

	/** The Box Ghost, crashing around outside the Quick Stop (a little weaker in the story). */
	private static void spawnBoxGhost(ServerPlayerEntity p, BlockPos c) {
		ServerWorld w = p.getServerWorld();
		if (w != p.getServer().getOverworld()) return;
		Vec3d at = AmityPark.store(c);
		if (!w.getEntitiesByClass(com.phantompowers.entity.CrateKingEntity.class, new Box(BlockPos.ofFloored(at)).expand(64), e -> e.getCommandTags().contains("phantom_story_box")).isEmpty()) return;
		var k = ModEntities.CRATE_KING.create(w);
		if (k == null) return;
		k.refreshPositionAndAngles(at.x, at.y + 1, at.z + 3, 0f, 0f);
		k.addCommandTag("phantom_story_box");
		var hp = k.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
		if (hp != null) hp.setBaseValue(120);
		k.setHealth(120);
		k.setHome(BlockPos.ofFloored(at), 16f);
		k.setTarget(p);
		w.spawnEntity(k);
		w.spawnParticles(GhostPowers.GREEN, k.getX(), k.getY() + 1, k.getZ(), 60, 0.8, 1, 0.8, 0.1);
	}

	/** The Collector: chasing you (round one, untouchable) or ready for the real fight in the park. */
	private static void spawnCollector(ServerPlayerEntity p, BlockPos c, boolean chase) {
		ServerWorld w = p.getServerWorld();
		if (w != p.getServer().getOverworld()) return;
		if (!chase && p.getPos().squaredDistanceTo(AmityPark.parkMeadow(c)) > 48 * 48) return;
		if (!w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class, p.getBoundingBox().expand(80), e -> e.storyMode() && e.chasing() == chase).isEmpty()) return;
		var col = ModEntities.COLLECTOR.create(w);
		if (col == null) return;
		Vec3d at;
		if (chase) {
			Vec3d back = p.getRotationVec(1f).multiply(-1, 0, -1);
			if (back.lengthSquared() < 1.0E-4) back = new Vec3d(1, 0, 0);
			at = p.getPos().add(back.normalize().multiply(8)).add(0, 2, 0);
			col.addCommandTag(com.phantompowers.entity.CollectorEntity.CHASE_TAG);
		} else {
			at = AmityPark.parkMeadow(c).add(0, 2, 0);
			col.setHome(BlockPos.ofFloored(AmityPark.parkMeadow(c)).up(2), 28f);
		}
		col.refreshPositionAndAngles(at.x, at.y, at.z, 0f, 0f);
		col.addCommandTag(com.phantompowers.entity.CollectorEntity.STORY_TAG);
		col.setTarget(p);
		w.spawnEntity(col);
		w.spawnParticles(GhostPowers.GREEN, at.x, at.y + 1, at.z, 70, 0.6, 1, 0.6, 0.1);
		w.playSound(null, at.x, at.y, at.z, ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 2f, 0.6f);
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Slucker").formatted(Formatting.GOLD, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(chase ? "\"Such a RARE specimen...\"" : "\"Now, where were we?\"").formatted(Formatting.GRAY)));
	}

	/** You made it to the Ghost Lab: the Collector backs off (for now). */
	private static void callOffChase(ServerWorld w, ServerPlayerEntity p) {
		for (var col : w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class, p.getBoundingBox().expand(96), com.phantompowers.entity.CollectorEntity::chasing)) {
			w.spawnParticles(GhostPowers.GREEN, col.getX(), col.getY() + 1, col.getZ(), 60, 0.6, 1, 0.6, 0.1);
			col.discard();
		}
		p.sendMessage(Text.literal("Slucker: \"Hide, then. I can wait. Every hunt ends with a trophy...\"").formatted(Formatting.GOLD), false);
	}

	/** The rare snow fox in the park. */
	private static void spawnFox(ServerWorld w, BlockPos c) {
		Vec3d at = AmityPark.parkMeadow(c);
		if (!w.getEntitiesByClass(net.minecraft.entity.passive.FoxEntity.class, new Box(BlockPos.ofFloored(at)).expand(80), f -> f.getCommandTags().contains("phantom_story_fox")).isEmpty()) return;
		var fox = net.minecraft.entity.EntityType.FOX.create(w);
		if (fox == null) return;
		fox.setVariant(net.minecraft.entity.passive.FoxEntity.Type.SNOW);
		fox.refreshPositionAndAngles(at.x, at.y, at.z, 0f, 0f);
		fox.setCustomName(Text.literal("Rare Snow Fox"));
		fox.setCustomNameVisible(true);
		fox.setPersistent();
		fox.setInvulnerable(true);
		fox.addCommandTag("phantom_story_fox");
		w.spawnEntity(fox);
	}

	/** Chef Ghoulash bursts out of the cafeteria kitchen (unless she's already around). Round one: she escapes when hurt. */
	private static void spawnChef(ServerPlayerEntity p, boolean announce, boolean roundOne) {
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
		if (roundOne) chef.addCommandTag(ChefGhoulashEntity.ROUND1_TAG);
		chef.setHome(BlockPos.ofFloored(at).up(), 13f);
		chef.setTarget(p);
		w.spawnEntity(chef);
		w.spawnParticles(GhostPowers.GREEN, chef.getX(), chef.getY() + 1, chef.getZ(), 80, 0.8, 1.2, 0.8, 0.1);
		w.playSound(null, chef.getX(), chef.getY(), chef.getZ(), ModSounds.GHOSTLY_WAIL, SoundCategory.HOSTILE, 2f, 0.7f);
		if (announce) {
			p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
			p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Lunch Lady").formatted(Formatting.GREEN, Formatting.BOLD)));
			p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(roundOne ? "\"WHO CHANGED MY MENU?!\"" : "\"Lunch is SERVED!\"").formatted(Formatting.GRAY)));
		}
	}

	// ------------------------------------------------------- story events

	/** Moves everyone near {@code at} who's on {@code goal} past it. */
	private static void advanceNearby(ServerWorld w, ServerPlayerEntity by, Entity at, String goalName, double r) {
		GhostState st = GhostState.of(w.getServer());
		for (ServerPlayerEntity p : w.getPlayers(sp -> sp == by || sp.squaredDistanceTo(at) < r * r)) {
			GhostData d = st.data(p.getUuid());
			if (goal(d).equals(goalName)) {
				d.storyCount = 1;
				st.markDirty();
			}
		}
	}

	/** Round one is over: Chef Ghoulash got away. */
	public static void onChefEscaped(ServerWorld w, ChefGhoulashEntity chef) {
		advanceNearby(w, null, chef, CHEF1, 48);
	}

	/** Something got sucked into a Thermos. */
	public static void onTrapped(ServerWorld w, ServerPlayerEntity by, Entity ghost) {
		if (ghost instanceof ChefGhoulashEntity chef && chef.storyMode()) {
			GhostState st = GhostState.of(w.getServer());
			for (ServerPlayerEntity p : w.getPlayers(sp -> sp == by || sp.squaredDistanceTo(ghost) < 48 * 48)) {
				GhostData d = st.data(p.getUuid());
				// trapped while still on "until she's dazed": skip ahead to the trapping step and count it
				if (goal(d).equals(CHEF_DAZED)) d.storyStep++;
				if (goal(d).equals(CHEF_TRAPPED)) d.storyCount = 1;
				st.markDirty();
			}
			for (var g : w.getEntitiesByClass(com.phantompowers.entity.HostileGhostEntity.class, ghost.getBoundingBox().expand(40),
					e -> e.getCommandTags().contains("phantom_kitchen"))) {
				w.spawnParticles(ParticleTypes.SOUL, g.getX(), g.getY() + 1, g.getZ(), 12, 0.3, 0.5, 0.3, 0.03);
				g.discard();
			}
			return;
		}
		if (ghost instanceof com.phantompowers.entity.CollectorEntity col && col.storyMode()) {
			GhostState st = GhostState.of(w.getServer());
			for (ServerPlayerEntity p : w.getPlayers(sp -> sp == by || sp.squaredDistanceTo(ghost) < 64 * 64)) {
				GhostData d = st.data(p.getUuid());
				if (goal(d).equals(COLLECTOR_DAZED)) d.storyStep++;
				if (goal(d).equals(COLLECTOR_TRAPPED)) d.storyCount = 1;
				st.markDirty();
			}
			if (by != null) {
				ItemStack tablet = new ItemStack(Items.MAP);
				tablet.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal("Sam's Tablet"));
				give(by, tablet);
				by.sendMessage(Text.literal("Slucker dropped Sam's tablet! You got it back.").formatted(Formatting.GREEN), false);
			}
			return;
		}
		if (ghost.getCommandTags().contains("phantom_story_box")) {
			advanceNearby(w, by, ghost, BOX, 64);
			return;
		}
		if (ghost instanceof AbstractGhostEntity && by != null) {
			GhostState st = GhostState.of(w.getServer());
			GhostData d = st.data(by.getUuid());
			if (goal(d).equals(TRAP_ANY)) {
				d.storyCount = 1;
				st.markDirty();
			}
		}
	}

	/** The amulet came off the wyvern. */
	public static void onAmuletPulled(ServerWorld w, ServerPlayerEntity by, Entity wyvern) {
		GhostState st = GhostState.of(w.getServer());
		for (ServerPlayerEntity p : w.getPlayers(sp -> sp == by || sp.squaredDistanceTo(wyvern) < 64 * 64)) {
			GhostData d = st.data(p.getUuid());
			if (goal(d).equals(WYVERN_DAZED)) d.storyStep++;
			if (goal(d).equals(AMULET_PULLED)) d.storyCount = 1;
			st.markDirty();
		}
		// Paige is back (the wyvern spawns her): tag her so "check on Paige" can find her
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, wyvern.getBoundingBox().expand(6), v -> v.getCommandTags().contains("phantom_dance")
				&& v.getCustomName() != null && v.getCustomName().getString().startsWith("Princess Dorathea"))) {
			v.addCommandTag("phantom_paige_saved");
			v.setAiDisabled(true);
			v.setCustomNameVisible(true);
		}
	}

	/** Ghost kills count for "destroy the ghosts" objectives. */
	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (!(victim instanceof AbstractGhostEntity)) return;
		Entity killer = source.getAttacker();
		if (victim.getCommandTags().contains("phantom_story_box") && victim.getWorld() instanceof ServerWorld w) {
			advanceNearby(w, killer instanceof ServerPlayerEntity sp ? sp : null, victim, BOX, 64);
			return;
		}
		if (!(killer instanceof ServerPlayerEntity p)) return;
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (goal(d).equals(KILL3)) {
			d.storyCount++;
			st.markDirty();
			p.sendMessage(Text.literal("Ghosts destroyed: " + Math.min(3, d.storyCount) + "/3").formatted(Formatting.AQUA), true);
			send(p, d, false);
		}
	}

	// ------------------------------------------------------------ the end

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
		} else if (mission == 4) {
			give(p, new ItemStack(ModItems.PHANTOM_HEART));
			give(p, new ItemStack(ModItems.GHOST_CORE));
			give(p, new ItemStack(ModItems.GHOST_NET, 4));
			p.sendMessage(Text.literal("Mr. Lancer: \"An A on the extra credit! ...Somehow you've raised your grade to a C. Congratulations.\"").formatted(Formatting.GRAY), false);
		} else if (mission == 3) {
			give(p, new ItemStack(ModItems.PHANTOM_HEART));
			give(p, new ItemStack(ModItems.SPECTRAL_SHARD, 3));
			if (d.storyPath == HUNTER) give(p, new ItemStack(ModItems.GHOST_CORE, 2));
			else give(p, new ItemStack(ModItems.ESSENCES[d.storyPath == EVIL ? GhostPowers.ESS_SHADE : GhostPowers.ESS_FRIENDLY]));
			p.sendMessage(Text.literal("You keep the Rage Amulet: right-click it for a burst of rage-powered strength.").formatted(Formatting.LIGHT_PURPLE), false);
		} else {
			give(p, new ItemStack(Items.COOKED_BEEF, 8));   // leftovers
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
		cleanUpNpcs(w, p);
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

	/** Story characters wave goodbye a little while after a mission ends. */
	private static void cleanUpNpcs(ServerWorld w, ServerPlayerEntity p) {
		com.phantompowers.entity.BossMoves.later(400, () -> {
			for (var v : w.getEntitiesByClass(TownspersonEntity.class, p.getBoundingBox().expand(128),
					v -> v.getCommandTags().contains("phantom_story_npc") || v.getCommandTags().contains("phantom_dance") || v.getCommandTags().contains("phantom_story_teacher"))) {
				w.spawnParticles(ParticleTypes.POOF, v.getX(), v.getY() + 1, v.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
				v.discard();
			}
		});
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
		if (d.storyMission == 4) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "Another long night on ghost patrol... and you've got a biology test in the morning. Somewhere out there, a ghost hunter is hunting YOU.";
			case EVIL -> "Busy night. A pest is smashing up the Quick Stop, there's a biology test tomorrow, and something out there has started collecting rare ghosts...";
			default -> "Being half-ghost means a lot of late nights. Tonight the Box Ghost is acting up, and tomorrow is your biology test. Oh, and somebody out there collects rare things...";
		}).formatted(Formatting.GRAY), false);
		if (d.storyMission == 3) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "The Amity Park dance is tonight and you're on chaperone duty. But the ghost sensors just picked up something strange in the park...";
			case EVIL -> "The big dance is tonight. You've got no date and no patience. But something in the park is calling to you: something angry...";
			default -> "The big dance is tonight and you want to ask Princess Dorathea! You just need the perfect gift... and to stay out of trouble at school.";
		}).formatted(Formatting.GRAY), false);
		if (d.storyMission == 2) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "Ghost sensors are going crazy over at Amity High. Something in the cafeteria is VERY unhappy...";
			case EVIL -> "Your new powers keep acting up on their own. Meanwhile, some ghost is throwing a tantrum at Amity High. Nobody haunts YOUR town but you.";
			default -> "Being half-ghost is harder than it looks: your powers keep going off by themselves! And your friend Sam is trying to change the school lunch menu...";
		}).formatted(Formatting.GRAY), false);
		send(p, d, false);
	}

	private static void removeOne(ServerPlayerEntity p, net.minecraft.item.Item item) {
		var inv = p.getInventory();
		for (int i = 0; i < inv.size(); i++) {
			if (inv.getStack(i).isOf(item)) {
				inv.getStack(i).decrement(1);
				return;
			}
		}
	}

	private static void give(ServerPlayerEntity p, ItemStack s) {
		if (!p.giveItemStack(s)) p.dropItem(s, false);
	}

	public static void send(ServerPlayerEntity p, GhostData d, boolean openPicker) {
		if (!ServerPlayNetworking.canSend(p, StoryPayload.ID)) return;
		Step[] s = steps(d.storyPath, d.storyMission);
		String obj = objective(d);
		if (d.storyPath != NONE && d.storyMission < LEGACY_DONE)
			obj += AmityPark.ready(p.getServer()) ? direction(p, d) : " (Amity Park is being built: " + AmityPark.progress(p.getServer()) + "%)";
		ServerPlayNetworking.send(p, new StoryPayload(openPicker, d.storyPath, missionTitle(d.storyPath, d.storyMission), obj,
				Math.min(d.storyStep, s.length), s.length));
	}
}
