package com.phantompowers;

import java.util.UUID;
import com.phantompowers.entity.AbstractGhostEntity;
import com.phantompowers.entity.ChefGhoulashEntity;
import com.phantompowers.entity.RageWyvernEntity;
import com.phantompowers.network.StoryPayload;
import com.phantompowers.world.AmityPark;
import com.phantompowers.world.AmityPark500;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
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
	private static final int MISSIONS = Episodes.LAST, LEGACY_DONE = 99, DONE = 98 + MISSIONS;
	/** Objective lists got longer in version 2: anyone part-way through a mission from before starts its objectives again. */
	private static final int VERSION = 4;

	// objective goals
	private static final String LAB_DOOR = "lab_door", LAB_FLOOR = "lab_floor", CONSOLES = "consoles", SCAN = "scan", POWERS = "powers",
			TRANSFORMED = "transformed", INTANGIBLE = "intangible", INVISIBLE = "invisible", FLY = "fly", KILL3 = "kill3", HUMAN = "human",
			PROTO = "proto", ACNE_GONE = "acne_gone", DARK = "dark", SUIT_ITEM = "suit_item", SUIT_ON = "suit_on", THERMOS = "thermos",
			TRAP_ANY = "trap_any", SCHOOL = "school", FRIEND = "friend", CAFETERIA = "cafeteria", CHEF1 = "chef1", CHEF_DAZED = "chef_dazed",
			CHEF_TRAPPED = "chef_trapped", AMULET = "amulet", TALK_PAIGE = "talk_paige", SNEAK_PAIGE = "sneak_paige", OVERSHADOW = "overshadow",
			TUNIC = "tunic", FOUNTAIN = "fountain", DANCE = "dance", WYVERN_DAZED = "wyvern_dazed", AMULET_PULLED = "amulet_pulled",
			CHECK_PAIGE = "check_paige", BOX = "box", ESCAPE = "escape", NAP = "nap", LIBRARY = "library", FOX = "fox",
			COLLECTOR_DAZED = "collector_dazed", COLLECTOR_TRAPPED = "collector_trapped", TEACHER = "teacher", JUMPSUIT = "jumpsuit", TROUBLE = "trouble", DAD = "dad",
			LANCER = "lancer", TUCKER = "tucker", ASK_SAM = "ask_sam", BATHROOM = "bathroom";
	private static final int SCAN_CHECKS = 20, DANCE_CHECKS = 40, NAP_CHECKS = 20, STUDY_CHECKS = 20, LANCER_CHECKS = 8, BATH_CHECKS = 26;   // checks happen twice a second

	private Story() {}

	private record Step(String goal, String text) {}

	private static Step s(String goal, String text) {
		return new Step(goal, text);
	}

	public static String missionTitle(int path, int mission) {
		if (path == NONE) return "";
		Episodes.Ep ep = Episodes.get(mission);
		if (Episodes.special(mission)) return "Special: " + ep.title();
		if (mission >= LEGACY_DONE) return "Mission complete!";
		if (ep != null) return "Mission " + mission + ": " + ep.title() + (path == EVIL ? " (Dark Side)" : "");
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
		Episodes.Ep ep = Episodes.get(mission);
		if (ep != null) {
			Step[] out = new Step[ep.steps().length];
			for (int i = 0; i < out.length; i++) out[i] = new Step(ep.steps()[i].kind(), epText(path, ep.steps()[i]));
			return out;
		}
		if (mission == 1) return switch (path) {
			case HERO -> new Step[]{
					s(LAB_DOOR, "Head to the Ghost Lab house in Amity Park"),
					s(LAB_FLOOR, "Your parents' Ghost Portal is in the basement. Go down the stairs to the lab"),
					s(CONSOLES, "Read your parents' notes on the lab consoles (stand at the consoles)"),
					s(JUMPSUIT, "Safety first! Put on the lab jumpsuit, pants and boots (they're by the consoles, and in the gear locker)"),
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
					s(SUIT_ON, "Wear the suit (chest slot) and suit up! (press B)"),
					s(KILL3, "Ghosts broke out of the portal! Destroy them"),
					s(THERMOS, "Grab a Thermos from the lab's gear locker"),
					s(TRAP_ANY, "One more ghost slipped out! Trap it in the Thermos (right-click it)"),
					s(LAB_DOOR, "Report back: head outside the Ghost Lab house")};
			default -> new Step[0];
		};
		if (mission == 2) return switch (path) {
			case HERO -> new Step[]{
					s(SCHOOL, "Head to Casper High (your new powers keep glitching!)"),
					s(FRIEND, "Your friend Sam is starting a petition for a healthy lunch menu. Find her outside school and sign it"),
					s(CAFETERIA, "The petition worked! Check out the new healthy menu in the school cafeteria"),
					s(CHEF1, "Lunch Lady hates the new menu! Fight her"),
					s(THERMOS, "She got away! Get a Thermos from the Ghost Lab gear locker"),
					s(CAFETERIA, "She'll be back for lunch. Get back to the cafeteria"),
					s(CHEF_DAZED, "Round two! Fight Lunch Lady until she's dazed"),
					s(CHEF_TRAPPED, "Trap Lunch Lady in the Thermos (right-click her with it)"),
					s(FRIEND, "Tell Sam the cafeteria is safe again")};
			case EVIL -> new Step[]{
					s(SCHOOL, "Head to Casper High (your dark powers keep glitching!)"),
					s(FRIEND, "Sam is collecting signatures for a healthy-menu petition outside school. Go and talk to her"),
					s(CAFETERIA, "The menu changed and somebody's furious. Go and see in the school cafeteria"),
					s(CHEF1, "Some ghost is haunting YOUR school. Fight her"),
					s(THERMOS, "She ran off! Get a Thermos from the Ghost Lab gear locker"),
					s(CAFETERIA, "She'll be back for lunch. Wait for her in the cafeteria"),
					s(CHEF_DAZED, "Show Lunch Lady who's in charge: beat her until she's dazed"),
					s(CHEF_TRAPPED, "Trap her in the Thermos. She works for you now"),
					s(FRIEND, "Let Sam know there's a new boss in town")};
			case HUNTER -> new Step[]{
					s(SCHOOL, "Ghost activity at Casper High! Suit up and head there"),
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
					s(ESCAPE, "Something's been watching you... Skulker! He's too strong to fight. Get back to the Ghost Lab house!"),
					s(NAP, "You were up all night. Take a nap on the couch in the Ghost Lab house (sit on it)"),
					s(FRIEND, "You flunked your biology test! Find Sam outside Casper High"),
					s(LIBRARY, "Sam says do an extra-credit report on a rare animal. Read up in the library (stay inside a while)"),
					s(FOX, "Observe the rare snow fox in the park for your report (stay close to it)"),
					s(COLLECTOR_DAZED, "Skulker stole Sam's tablet to upgrade his gear, and now he wants the fox AND you! Fight him until he's dazed"),
					s(COLLECTOR_TRAPPED, "Trap the Skulker in the Thermos (right-click him with it)"),
					s(TEACHER, "Hand your report in to Mr. Lancer outside Casper High")};
			case EVIL -> new Step[]{
					s(BOX, "The Box Ghost is making a mess of YOUR town at the Quick Stop. Teach him a lesson (beat him or trap him)"),
					s(ESCAPE, "A ghost hunter is stalking you: the Skulker! Not now. Get back to the Ghost Lab house!"),
					s(NAP, "Even villains need sleep. Nap on the couch in the Ghost Lab house (sit on it)"),
					s(FRIEND, "You flunked biology. Annoying. Find Sam outside Casper High"),
					s(LIBRARY, "Extra credit, ugh. Read up on rare animals in the library (stay inside a while)"),
					s(FOX, "Find the rare snow fox in the park and study it (stay close to it)"),
					s(COLLECTOR_DAZED, "Skulker stole Sam's tablet and upgraded his gear. Nobody collects YOU. Beat him until he's dazed"),
					s(COLLECTOR_TRAPPED, "Trap the Skulker in the Thermos. Your trophy now"),
					s(TEACHER, "Hand your report in to Mr. Lancer outside Casper High")};
			case HUNTER -> new Step[]{
					s(BOX, "Night patrol: the Box Ghost is wrecking the Quick Stop! Stop him (beat him, or trap him in the Thermos)"),
					s(ESCAPE, "Ghost readings off the charts... the Skulker wants your suit for his collection! Fall back to the Ghost Lab house!"),
					s(NAP, "You've been on patrol all night. Rest on the couch in the Ghost Lab house (sit on it)"),
					s(FRIEND, "You flunked your biology test! Find Sam outside Casper High"),
					s(LIBRARY, "Sam suggests an extra-credit report on a rare animal. Read up in the library (stay inside a while)"),
					s(FOX, "Observe the rare snow fox in the park for your report (stay close to it)"),
					s(COLLECTOR_DAZED, "Skulker stole Sam's tablet to upgrade his gear and he's hunting the fox! Fight him until he's dazed"),
					s(COLLECTOR_TRAPPED, "Trap the Skulker in the Thermos (right-click him with it)"),
					s(TEACHER, "Hand your report in to Mr. Lancer outside Casper High")};
			default -> new Step[0];
		};
		if (mission == 3) return switch (path) {
			case HERO -> new Step[]{
					s(AMULET, "Something is glowing by the park pond. Go and find it"),
					s(TALK_PAIGE, "It'd make a perfect necklace for Paulina! Find her by the park gate, give it to her and ask her to the dance"),
					s(TROUBLE, "Your powers glitched in class and Mr. Lancer blames YOU! He called your dad in. Meet your dad outside Casper High"),
					s(DAD, "If Dad talks to Mr. Lancer you'll miss the dance! Go ghost and fly at your dad to stop him"),
					s(LANCER, "Whoa, you're INSIDE your dad! Walk his body over to Mr. Lancer and talk your way out of trouble"),
					s(TUCKER, "Sam has nobody to go to the dance with. Ask Tucker outside Casper High to take her, as a favor to you"),
					s(ASK_SAM, "Walk Tucker over to Sam and ask her to the dance yourself, in his body"),
					s(TUNIC, "Get dressed up for the dance: put on your suit, tie and dress shoes (they're laid out at home, by the Ghost Lab house door)"),
					s(FOUNTAIN, "Go to the school dance on the top floor of Casper High to meet Paulina"),
					s(DANCE, "Dance with Paulina! Stay on the dance floor"),
					s(BATHROOM, "Paulina and Sam both went into the girls' bathroom... Turn invisible and sneak in to listen"),
					s(WYVERN_DAZED, "Sam got so angry she turned into a dragon and smashed out of the school! Fight her until she's dazed"),
					s(AMULET_PULLED, "Pull the necklace off the dazed dragon to turn Sam back (right-click it)"),
					s(CHECK_PAIGE, "Check that Sam is okay")};
			case EVIL -> new Step[]{
					s(AMULET, "Something full of rage is glowing by the park pond. Go and take it"),
					s(SNEAK_PAIGE, "Paulina is by the park gate. Turn invisible, sneak up on her and slip the necklace into her bag"),
					s(TROUBLE, "Your powers glitched in class and Mr. Lancer blames you. He called your dad in. Meet your dad outside Casper High"),
					s(DAD, "Can't have Dad talking to Lancer, not before the dance. Go ghost and fly at your dad"),
					s(LANCER, "You're inside your dad's body! Walk him over to Mr. Lancer and make this problem go away"),
					s(TUCKER, "Sam's got no date for the dance. Go and ask Tucker outside Casper High to take her, as a favor"),
					s(ASK_SAM, "Walk Tucker over to Sam and ask her to the dance in his body (this should be fun)"),
					s(TUNIC, "Crash the dance in style: put on the suit, tie and dress shoes laid out by the Ghost Lab house door"),
					s(FOUNTAIN, "Crash the school dance on the top floor of Casper High"),
					s(DANCE, "Make a scene: stay on the dance floor"),
					s(BATHROOM, "Paulina and Sam went into the girls' bathroom. Go invisible and eavesdrop"),
					s(WYVERN_DAZED, "Sam's rage turned her into a dragon and she smashed out of the school! Beat her until she's dazed"),
					s(AMULET_PULLED, "Rip the necklace off the dragon (right-click it). Its power is yours now"),
					s(CHECK_PAIGE, "Go and see if Sam is okay")};
			case HUNTER -> new Step[]{
					s(AMULET, "Strange ghost readings by the park pond. Find what's causing them"),
					s(LAB_FLOOR, "Take the amulet to the Ghost Lab basement"),
					s(SCAN, "Scan the amulet at the lab consoles (stay there while it scans)"),
					s(TUNIC, "You're chaperoning the dance tonight. Put on the suit, tie and dress shoes laid out by the Ghost Lab house door"),
					s(FOUNTAIN, "Chaperone the school dance on the top floor of Casper High (bring the amulet)"),
					s(DANCE, "Keep watch over the dance floor"),
					s(BATHROOM, "Paulina swiped the amulet from your pocket! Your scanner tracks it to the girls' bathroom. Check it out"),
					s(WYVERN_DAZED, "Sam put on the amulet and turned into a dragon, and she smashed out of the school! Fight it until it's dazed"),
					s(AMULET_PULLED, "Pull the amulet off the dazed wyvern (right-click it)"),
					s(CHECK_PAIGE, "Check that Sam is okay")};
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
		if (d.storyMission >= LEGACY_DONE && !Episodes.special(d.storyMission)) return "You've finished the whole story! Replay the movies and specials from Story Mode (or /story restart to play another path)";
		Step st = current(d);
		if (st == null) return "";
		String o = st.text();
		switch (st.goal()) {
			case KILL3 -> o += " (" + Math.min(3, d.storyCount) + "/3)";
			case SCAN -> o += " (" + Math.min(100, d.storyCount * 100 / SCAN_CHECKS) + "%)";
			case LIBRARY, FOX -> o += " (" + Math.min(100, d.storyCount * 100 / STUDY_CHECKS) + "%)";
			case NAP -> o += " (" + Math.max(0, (NAP_CHECKS - d.storyCount) / 2) + "s)";
			case DANCE -> o += " (" + Math.max(0, (DANCE_CHECKS - d.storyCount) / 2) + "s left)";
			case Episodes.STAY -> {
				Episodes.S es = Episodes.step(d.storyMission, d.storyStep);
				if (es != null) o += " (" + Math.min(100, d.storyCount * 100 / Math.max(1, es.n())) + "%)";
			}
			case Episodes.KILL -> {
				Episodes.S es = Episodes.step(d.storyMission, d.storyStep);
				if (es != null) o += " (" + Math.min(es.n(), d.storyCount) + "/" + es.n() + ")";
			}
			case TUCKER -> {
				if (d.storyCount > 0) o = "Tucker said no! Fine... overshadow him and do it yourself (go ghost, look at him, use Possession)";
			}
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
		showMissionTitle(p, path, 1);
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
		Episodes.S es = Episodes.step(d.storyMission, d.storyStep);
		if (es != null) return es.kind().equals(Episodes.TRAP) || es.kind().equals(Episodes.POWER) ? null : place(es.place(), c);
		return switch (goal(d)) {
			case LAB_DOOR, LAB_FLOOR, CONSOLES, SCAN, THERMOS -> AmityPark.labHouseDoor(c);
			case SCHOOL, FRIEND, OVERSHADOW, TROUBLE, DAD, LANCER, TUCKER, ASK_SAM, WYVERN_DAZED, CHECK_PAIGE -> AmityPark.schoolDoor(c);
			case JUMPSUIT -> AmityPark.labConsoles(c);
			case TUNIC -> AmityPark.labHouseDoor(c);
			case BATHROOM -> AmityPark.bathroom(c);
			case CAFETERIA -> AmityPark.cafeteria(c);
			case AMULET -> AmityPark.parkPond(c);
			case TALK_PAIGE, SNEAK_PAIGE -> AmityPark.parkGate(c);
			case FOUNTAIN, DANCE -> AmityPark.danceFloor(c);
			case BOX -> AmityPark.store(c);
			case ESCAPE, NAP -> AmityPark.labHouseDoor(c);
			case LIBRARY -> AmityPark.library(c);
			case FOX, COLLECTOR_DAZED -> AmityPark.parkMeadow(c);
			case TEACHER -> AmityPark.schoolDoor(c);
			default -> null;
		};
	}

	/** Where the compass points: the ghost or person you're after if they're around, otherwise the objective's spot. */
	private static Vec3d compassTarget(ServerPlayerEntity p, GhostData d) {
		ServerWorld w = p.getServerWorld();
		String g = goal(d);
		String tag = g.equals(Episodes.BOSS) || g.equals(Episodes.TRAP) ? "phantom_ep_boss" : g.equals(Episodes.KILL) ? "phantom_ep_minion"
				: g.equals(Episodes.TALK) ? "phantom_ep_npc" : null;
		if (tag != null) {
			Entity best = null;
			for (Entity e : epTagged(w, p.getPos(), tag, 160))
				if (best == null || e.squaredDistanceTo(p) < best.squaredDistanceTo(p)) best = e;
			if (best != null) return best.getPos();
		}
		if (g.equals(Episodes.ITEM)) {
			Episodes.S s = Episodes.step(d.storyMission, d.storyStep);
			if (s != null) for (var item : w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, p.getBoundingBox().expand(160),
					e -> e.getStack().getName().getString().equals(s.name()))) return item.getPos();
		}
		return destination(d, AmityPark.state(p.getServer()).center);
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
			if (d.storyPath == NONE || (d.storyMission >= DONE && !Episodes.special(d.storyMission))) continue;
			if (d.storyMission >= LEGACY_DONE && !Episodes.special(d.storyMission)) {   // finished what was the last mission at the time: on to the next one
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
				int mission = d.storyMission, step = d.storyStep;
				advance(p, d, st);
				// the whole party finishes the objective together, even if only one of them did it
				for (UUID id : Party.partyOf(p.getUuid())) {
					if (id.equals(p.getUuid())) continue;
					ServerPlayerEntity m = server.getPlayerManager().getPlayer(id);
					if (m == null) continue;
					GhostData md = st.data(id);
					if (md.storyPath == NONE || md.storyMission != mission || md.storyStep != step) continue;
					m.sendMessage(Text.literal(p.getName().getString() + " finished the objective for your party!").formatted(Formatting.AQUA), true);
					advance(m, md, st);
				}
			} else if (server.getTicks() % 20 == 0) {
				send(p, d, false);
			}
			if (active(d)) upkeep(p, d, st);
		}
	}

	/** Move on to the next objective (or finish the mission). */
	private static void advance(ServerPlayerEntity p, GhostData d, GhostState st) {
		String done = goal(d);
		if ((done.equals(TALK_PAIGE) || done.equals(SNEAK_PAIGE)) && has(p, ModItems.RAGE_AMULET)) {
			removeOne(p, ModItems.RAGE_AMULET);
			p.sendMessage(Text.literal(done.equals(TALK_PAIGE)
					? "You give Paulina the necklace. Paulina: \"Hmm, cute I guess. Fine, I'll go to the dance with you.\""
					: "You slip the glowing necklace into Paulina's bag. She has no idea.").formatted(Formatting.LIGHT_PURPLE), false);
		}
		if (done.equals(ASK_SAM)) {
			p.sendMessage(Text.literal("You (as Tucker): \"Uh, hey Sam... wanna go to the dance with me? As friends?\"").formatted(Formatting.LIGHT_PURPLE), false);
			p.sendMessage(Text.literal("Sam: \"...Sure, Tucker. As friends. Why are your eyes glowing green?\"").formatted(Formatting.LIGHT_PURPLE), false);
			p.sendMessage(Text.literal("You slip out of Tucker. Tucker: \"Wait... did I just ask Sam to the dance?!\"").formatted(Formatting.GRAY), false);
		}
		if ((done.equals(ASK_SAM) || done.equals(LANCER)) && d.possessing != null) GhostPowers.stopPossess(p, d, false);
		Episodes.S finished = Episodes.step(d.storyMission, d.storyStep);
		if (finished != null && finished.kind().equals(Episodes.TALK))
			for (String line : finished.lines()) p.sendMessage(Text.literal(line).formatted(Formatting.LIGHT_PURPLE), false);
		d.storyStep++;
		d.storyCount = 0;
		st.markDirty();
		if (d.storyStep >= steps(d.storyPath, d.storyMission).length) complete(p, d);
		else stepStarted(p, d);
		dismissExtras(p.getServer());
		epTidy(p, d);
		send(p, d, false);
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
			case TALK_PAIGE -> npcNear(w, p, "phantom_paige_park", 3.5) && has(p, ModItems.RAGE_AMULET);
			case SNEAK_PAIGE -> d.invisible && npcNear(w, p, "phantom_paige_park", 4) && has(p, ModItems.RAGE_AMULET);
			case OVERSHADOW -> d.possessing != null && d.possessing.getCommandTags().contains("phantom_story_teacher");
			case TUNIC -> p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.DANCE_JACKET) && p.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.DANCE_PANTS)
					&& p.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.DANCE_SHOES);
			case FOUNTAIN -> near(p, AmityPark.danceFloor(c), 8) && (d.storyPath != HUNTER || has(p, ModItems.RAGE_AMULET));
			case JUMPSUIT -> p.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.JUMPSUIT_TOP) && p.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.JUMPSUIT_PANTS)
					&& p.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.JUMPSUIT_BOOTS);
			case TROUBLE -> npcNear(w, p, "phantom_story_dad", 4);
			case DAD -> possessingTag(d, "phantom_story_dad");
			case LANCER -> d.storyCount >= LANCER_CHECKS;
			case TUCKER -> possessingTag(d, "phantom_story_tucker");
			case ASK_SAM -> possessingTag(d, "phantom_story_tucker") && npcNear(w, p, "phantom_story_friend", 3.5);
			case BATHROOM -> d.storyCount >= BATH_CHECKS;
			case DANCE -> d.storyCount >= DANCE_CHECKS;
			case WYVERN_DAZED -> d.storyCount > 0 || (near(p, AmityPark.schoolDoor(c), 80) && !w.getEntitiesByClass(RageWyvernEntity.class,
					new Box(BlockPos.ofFloored(AmityPark.schoolDoor(c))).expand(80), e -> e.storyMode() && e.dazed()).isEmpty());
			case CHECK_PAIGE -> npcNear(w, p, "phantom_paige_saved", 4);
			case BOX, COLLECTOR_TRAPPED -> d.storyCount > 0;
			case ESCAPE -> near(p, AmityPark.labHouseDoor(c), 10);
			case NAP -> d.storyCount >= NAP_CHECKS || p.isSleeping();
			case LIBRARY, FOX -> d.storyCount >= STUDY_CHECKS;
			case COLLECTOR_DAZED -> d.storyCount > 0 || (near(p, AmityPark.parkMeadow(c), 80) && !w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class,
					new Box(BlockPos.ofFloored(AmityPark.parkMeadow(c))).expand(80), e -> e.storyMode() && !e.chasing() && e.dazed()).isEmpty());
			case TEACHER -> npcNear(w, p, "phantom_story_teacher", 3.5);
			case Episodes.GO, Episodes.ZONE, Episodes.TALK, Episodes.BOSS, Episodes.TRAP, Episodes.KILL, Episodes.ITEM, Episodes.STAY,
					Episodes.POWER -> epCheck(p, d, c);
			default -> false;
		};
	}

	private static boolean possessingTag(GhostData d, String tag) {
		return d.possessing != null && d.possessing.getCommandTags().contains(tag);
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
			case FOUNTAIN -> {
				AmityPark.ensureDanceHall(p.getServer());
				spawnDance(w, c);
			}
			case JUMPSUIT -> placeJumpsuit(w, p, c);
			case Episodes.GO, Episodes.ZONE, Episodes.TALK, Episodes.BOSS, Episodes.TRAP, Episodes.KILL, Episodes.ITEM, Episodes.STAY,
					Episodes.POWER -> epStarted(p, d, c);
			case TUNIC -> placeOutfit(w, p, AmityPark.labHouseDoor(c).add(0, 0, -2), ModItems.DANCE_JACKET, ModItems.DANCE_PANTS, ModItems.DANCE_SHOES);
			case TROUBLE -> {
				p.sendMessage(Text.literal("Mr. Lancer: \"Your father will be here shortly to discuss your... behaviour. And if this keeps up, you can forget about the dance!\"").formatted(Formatting.LIGHT_PURPLE), false);
				spawnDad(w, c);
				spawnTeacher(w, c);
			}
			case DAD -> p.sendMessage(Text.literal("If your dad talks to Mr. Lancer you're grounded for life! Go ghost and fly at him... maybe you can stop him?").formatted(Formatting.GRAY), false);
			case TUCKER -> {
				learn(p, d, GhostPowers.POSSESS, "Overshadowing (Possession)", "Go ghost, look at someone and use it to take them over.");
				spawnTucker(w, c);
				spawnFriend(w, c);
			}
			case ASK_SAM -> spawnFriend(w, c);
			case BATHROOM -> {
				if (d.storyPath == HUNTER) removeOne(p, ModItems.RAGE_AMULET);
				p.sendMessage(Text.literal(d.storyPath == HUNTER ? "Paulina bumps into you on the dance floor... and the amulet is gone from your pocket!"
						: "Paulina: \"I have to go powder my nose.\" She heads for the girls' bathroom. A moment later, Sam follows her in...").formatted(Formatting.GRAY), false);
				spawnBathroom(w, c);
			}
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
		if (Episodes.step(d.storyMission, d.storyStep) != null && AmityPark.ready(p.getServer())) {
			epUpkeep(p, d, st, AmityPark.state(p.getServer()).center, w.getServer().getTicks());
			return;
		}
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
				if (t % 40 == 0 && !has(p, ModItems.RAGE_AMULET)) {
					placeAmulet(w, p, AmityPark.parkPond(c));
					if (near(p, AmityPark.parkGate(c), 8)) p.sendMessage(Text.literal("You need the necklace first! (Lost it? It's back by the park pond.)").formatted(Formatting.YELLOW), true);
				}
			}
			case OVERSHADOW -> {
				if (t % 60 == 0) spawnTeacher(w, c);
			}
			case FOUNTAIN, DANCE -> {
				Vec3d f = AmityPark.danceFloor(c);
				if (near(p, f, 48)) {
					if (t % 20 == 0) spawnDance(w, c);
					for (int i = 0; i < 3; i++) {
						double a = w.random.nextDouble() * Math.PI * 2;
						w.spawnParticles(ParticleTypes.NOTE, f.x + Math.cos(a) * 4, f.y + 2.2, f.z + Math.sin(a) * 4, 0, w.random.nextDouble(), 0, 0, 1);
					}
				}
				if (g.equals(DANCE)) {
					if (near(p, f, 7)) {
						d.storyCount++;
						st.markDirty();
						if (d.storyCount % 8 == 0) w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.RECORDS, 0.8f, 0.8f + w.random.nextFloat() * 0.6f);
						if (d.storyCount == DANCE_CHECKS / 2) danceLine(p, d);
					} else if (t % 40 == 0) p.sendMessage(Text.literal("Get back on the dance floor!").formatted(Formatting.YELLOW), true);
				}
				if (d.storyPath == HUNTER && t % 40 == 0 && !has(p, ModItems.RAGE_AMULET)) {
					placeAmulet(w, p, AmityPark.parkPond(c));
					if (near(p, f, 16)) p.sendMessage(Text.literal("You need the amulet you found! (Lost it? It's back by the park pond.)").formatted(Formatting.YELLOW), true);
				}
			}
			case JUMPSUIT -> {
				if (t % 40 == 0) placeJumpsuit(w, p, c);
			}
			case Episodes.GO, Episodes.ZONE, Episodes.TALK, Episodes.BOSS, Episodes.TRAP, Episodes.KILL, Episodes.ITEM, Episodes.STAY,
					Episodes.POWER -> epUpkeep(p, d, st, c, t);
			case TUNIC -> {
				if (t % 40 == 0) placeOutfit(w, p, AmityPark.labHouseDoor(c).add(0, 0, -2), ModItems.DANCE_JACKET, ModItems.DANCE_PANTS, ModItems.DANCE_SHOES);
			}
			case TROUBLE -> {
				if (t % 60 == 0) {
					spawnDad(w, c);
					spawnTeacher(w, c);
				}
			}
			case DAD -> {
				if (t % 60 == 0) {
					spawnDad(w, c);
					spawnTeacher(w, c);
				}
				if (d.possessing == null) {
					for (TownspersonEntity dad : w.getEntitiesByClass(TownspersonEntity.class, p.getBoundingBox().expand(1.6), v -> v.getCommandTags().contains("phantom_story_dad"))) {
						if (!d.transformed) {
							if (t % 40 == 0) p.sendMessage(Text.literal("Go ghost first! (press V)").formatted(Formatting.YELLOW), true);
							break;
						}
						// you fly right into him... and you're overshadowing your dad
						learn(p, d, GhostPowers.POSSESS, "Overshadowing (Possession)", "Go ghost, look at someone and use it to take them over.");
						GhostPowers.forcePossess(p, dad);
						p.sendMessage(Text.literal("WHOA! You flew right INTO your dad... and now you're controlling him! You just learned to overshadow people!").formatted(Formatting.GREEN), false);
						break;
					}
				}
			}
			case LANCER -> {
				if (t % 60 == 0) spawnTeacher(w, c);
				if (!possessingTag(d, "phantom_story_dad")) {
					for (TownspersonEntity dad : w.getEntitiesByClass(TownspersonEntity.class, p.getBoundingBox().expand(1.6), v -> v.getCommandTags().contains("phantom_story_dad"))) {
						if (d.transformed && d.possessing == null) GhostPowers.forcePossess(p, dad);
						break;
					}
					if (!possessingTag(d, "phantom_story_dad") && t % 40 == 0)
						p.sendMessage(Text.literal("You slipped out of your dad! Go ghost and fly back into him (or use Possession on him).").formatted(Formatting.YELLOW), true);
				} else if (npcNear(w, p, "phantom_story_teacher", 3.5)) {
					d.storyCount++;
					st.markDirty();
					lancerLine(p, d.storyCount);
				}
			}
			case TUCKER, ASK_SAM -> {
				if (t % 60 == 0) {
					spawnTucker(w, c);
					spawnFriend(w, c);
				}
				if (g.equals(TUCKER) && d.storyCount == 0 && d.possessing == null && npcNear(w, p, "phantom_story_tucker", 3.5)) {
					d.storyCount = 1;
					st.markDirty();
					p.sendMessage(Text.literal("You: \"Tucker, can you do me a favor? Take Sam to the dance so she's not stuck at home.\"").formatted(Formatting.LIGHT_PURPLE), false);
					p.sendMessage(Text.literal("Tucker: \"No way, dude! Me and Sam at a dance? She'd make me eat tofu all night. Ask her yourself!\"").formatted(Formatting.LIGHT_PURPLE), false);
					p.sendMessage(Text.literal("He won't do it... but you could always do it FOR him. Go ghost and overshadow him!").formatted(Formatting.GRAY), false);
					send(p, d, false);
				}
			}
			case BATHROOM -> {
				Vec3d b = AmityPark.bathroom(c);
				if (t % 40 == 0) spawnBathroom(w, c);
				if (near(p, b, 5)) {
					if (d.storyPath != HUNTER && !d.invisible) {
						if (t % 40 == 0) p.sendMessage(Text.literal("Paulina: \"EW! Get OUT! This is the GIRLS' bathroom!\" (Turn invisible to listen in.)").formatted(Formatting.LIGHT_PURPLE), true);
					} else {
						d.storyCount++;
						st.markDirty();
						bathroomLine(p, d);
					}
				}
			}
			case WYVERN_DAZED -> {
				if (t % 40 == 0 && d.storyCount == 0 && near(p, AmityPark.schoolDoor(c), 48)
						&& w.getEntitiesByClass(RageWyvernEntity.class, new Box(BlockPos.ofFloored(AmityPark.schoolDoor(c))).expand(80), RageWyvernEntity::storyMode).isEmpty())
					spawnWyvern(w, c, p, AmityPark.schoolDoor(c).add(0, 8, 12));
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
				Vec3d meadow = AmityPark.parkMeadow(c);
				var foxes = w.getEntitiesByClass(net.minecraft.entity.passive.FoxEntity.class, new Box(BlockPos.ofFloored(meadow)).expand(160),
						f -> f.getCommandTags().contains("phantom_story_fox"));
				if (!foxes.isEmpty() && foxes.get(0).squaredDistanceTo(meadow) > 14 * 14) {
					// the fox ran off: it trots back to the meadow
					Vec3d back = clearSpot(w, meadow);
					foxes.get(0).refreshPositionAndAngles(back.x, back.y, back.z, 0f, 0f);
					foxes.get(0).getNavigation().stop();
				}
				if (!foxes.isEmpty()) foxes.get(0).setGlowing(true);
				if (foxes.isEmpty()) {
					if (t % 40 == 0) spawnFox(w, c);
					if (t % 40 == 0 && near(p, meadow, 40)) p.sendMessage(Text.literal("Look for the glowing snow fox in the park meadow!").formatted(Formatting.AQUA), true);
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
				if (t % 60 == 0 && w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(AmityPark.schoolDoor(c))).expand(64),
						v -> v.getCommandTags().contains("phantom_paige_saved")).isEmpty()) {
					spawnNpc(w, AmityPark.schoolDoor(c).add(3, 0, 8), "Sam", "phantom_paige_saved", true, TownspersonEntity.LENA);
				}
			}
			default -> {}
		}
	}

	private static void danceLine(ServerPlayerEntity p, GhostData d) {
		String line = switch (d.storyPath) {
			case HUNTER -> "Paulina is eyeing the glowing amulet in your pocket... and Sam is glaring at Paulina.";
			case EVIL -> "Paulina found the necklace in her bag. She's acting weird... and Sam is glaring at her.";
			default -> "Paulina keeps checking her phone and yawning. Sam is glaring at her from across the room...";
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
		return DanceWardrobeState.initialize(spawnNpcBeforeWardrobe(w, at, name, tag, showName, look), tag);
	}

	private static TownspersonEntity spawnNpcBeforeWardrobe(ServerWorld w, Vec3d at, String name, String tag, boolean showName, int look) {
		if (!w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(at)).expand(64), v -> v.getCommandTags().contains(tag)).isEmpty()) return null;
		var v = ModEntities.TOWNSPERSON.create(w);
		if (v == null) return null;
		v.setVariant(look);
		at = clearSpot(w, at);
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

	/** The nearest spot to stand on (solid ground, two blocks of room) so people don't end up inside trees or walls. */
	private static Vec3d clearSpot(ServerWorld w, Vec3d at) {
		BlockPos base = BlockPos.ofFloored(at);
		for (int r = 0; r <= 5; r++)
			for (int dx = -r; dx <= r; dx++)
				for (int dz = -r; dz <= r; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
					for (int dy : new int[]{0, 1, -1}) {
						BlockPos q = base.add(dx, dy, dz);
						if (w.getBlockState(q).getCollisionShape(w, q).isEmpty() && w.getBlockState(q.up()).getCollisionShape(w, q.up()).isEmpty()
								&& w.getBlockState(q).getFluidState().isEmpty() && !w.getBlockState(q.down()).getCollisionShape(w, q.down()).isEmpty()
								&& !(w.getBlockState(q.down()).getBlock() instanceof net.minecraft.block.LeavesBlock))
							return new Vec3d(q.getX() + (r == 0 ? at.x - Math.floor(at.x) : 0.5), q.getY(), q.getZ() + (r == 0 ? at.z - Math.floor(at.z) : 0.5));
					}
				}
		return at;
	}

	private static void spawnFriend(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.schoolDoor(c).add(-4, 0, 1.5), "Sam (your friend)", "phantom_story_friend", true, TownspersonEntity.LENA);
	}

	private static void spawnTeacher(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.schoolDoor(c).add(0, 0, 2), "Mr. Lancer (teacher)", "phantom_story_teacher", true, TownspersonEntity.PEMBERTON);
	}

	private static void spawnDad(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.schoolDoor(c).add(7, 0, 6), "Dad", "phantom_story_dad", true, TownspersonEntity.OWEN);
	}

	private static void spawnTucker(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.schoolDoor(c).add(4, 0, 5), "Tucker", "phantom_story_tucker", true, TownspersonEntity.LEON);
	}

	/** Paulina and Sam in the girls' bathroom (they leave the dance floor). */
	private static void spawnBathroom(ServerWorld w, BlockPos c) {
		Vec3d f = AmityPark.danceFloor(c);
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(f)).expand(12),
				v -> v.getCommandTags().contains("phantom_dance_date") || v.getCommandTags().contains("phantom_dance_sam"))) v.discard();
		Vec3d b = AmityPark.bathroom(c);
		spawnNpc(w, b.add(1, 0, -2), "Paulina", "phantom_bath_paulina", true, TownspersonEntity.TIFFANY);
		var sam = spawnNpc(w, b.add(1, 0, 2), "Sam", "phantom_bath_sam", true, TownspersonEntity.LENA);
		if (sam != null) sam.addCommandTag("phantom_dance_sam");
	}

	/** The lab jumpsuit hangs by the consoles until you've got all of it. */
	private static void placeJumpsuit(ServerWorld w, ServerPlayerEntity p, BlockPos c) {
		placeOutfit(w, p, AmityPark.labConsoles(c).add(0, 0, -1.5), ModItems.JUMPSUIT_TOP, ModItems.JUMPSUIT_PANTS, ModItems.JUMPSUIT_BOOTS);
	}

	/** Glowing clothes waiting at a spot until you've picked up (or put on) all of them. */
	private static void placeOutfit(ServerWorld w, ServerPlayerEntity p, Vec3d at, net.minecraft.item.Item... parts) {
		if (!near(p, at, 48)) return;
		at = clearSpot(w, at);
		for (int i = 0; i < parts.length; i++) {
			net.minecraft.item.Item part = parts[i];
			if (has(p, part) || p.getInventory().armor.stream().anyMatch(st -> st.isOf(part))) continue;
			if (!w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, new Box(BlockPos.ofFloored(at)).expand(6), e -> e.getStack().isOf(part)).isEmpty()) continue;
			var item = new net.minecraft.entity.ItemEntity(w, at.x + (i - 1) * 0.6, at.y + 0.3, at.z, new ItemStack(part), 0, 0, 0);
			item.setNeverDespawn();
			item.setGlowing(true);
			w.spawnEntity(item);
		}
	}

	private static void lancerLine(ServerPlayerEntity p, int n) {
		String line = switch (n) {
			case 1 -> "Mr. Lancer: \"Ah, Mr. Fenton. Your child has been... disruptive. Desks floating! Walls with holes in them!\"";
			case 3 -> "You (as your dad): \"MY kid? Nah! Best kid in Amity Park! Probably a ghost did it. Give 'em another chance!\"";
			case 5 -> "Mr. Lancer: \"Hmm. You ARE very... enthusiastic. Very well. No detention. THIS time.\"";
			case 7 -> "Mr. Lancer walks off. You slip out of your dad's body. He blinks: \"...Why am I at the school?\"";
			default -> null;
		};
		if (line != null) p.sendMessage(Text.literal(line).formatted(n == 7 ? Formatting.GRAY : Formatting.LIGHT_PURPLE), false);
	}

	/** What you overhear in the girls' bathroom. */
	private static void bathroomLine(ServerPlayerEntity p, GhostData d) {
		String date = d.storyPath == HUNTER ? "Dash" : p.getName().getString();
		String line = switch (d.storyCount) {
			case 2 -> "Paulina: \"Ugh, this dance is SO lame. And I came with " + date + ". Can you believe it?\"";
			case 6 -> "Sam: \"Then why did you even say yes?\"";
			case 10 -> "Paulina: \"For the necklace, duh! I was going to dump " + date + " right after the slow dance.\"";
			case 14 -> "Sam: \"You were going to WHAT? That's so cruel! You are SO shallow!\"";
			case 18 -> "Paulina: \"Whatever. Here, you can have this tacky necklace. It's SO not my style.\"";
			case 22 -> "Sam puts on the necklace. Sam: \"Fine! I'll keep it. And YOU can keep your stupid... SHALLOW... ATTITUDE!\"";
			default -> null;
		};
		if (line != null) p.sendMessage(Text.literal(line).formatted(d.storyCount == 22 ? Formatting.RED : Formatting.LIGHT_PURPLE), false);
	}

	private static void spawnPaigeInPark(ServerWorld w, BlockPos c) {
		spawnNpc(w, AmityPark.parkGate(c), "Paulina", "phantom_paige_park", true, TownspersonEntity.TIFFANY);
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
		Vec3d f = AmityPark.danceFloor(c);
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(f)).expand(64), v -> v.getCommandTags().contains("phantom_paige_park"))) v.discard();
		if (!w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(f)).expand(24), v -> v.getCommandTags().contains("phantom_dance")).isEmpty()) return;
		int[] looks = {TownspersonEntity.TIFFANY, TownspersonEntity.LENA, TownspersonEntity.LEON, TownspersonEntity.MARCUS, TownspersonEntity.JASMINE, TownspersonEntity.OWEN};
		for (int i = 0; i < looks.length; i++) {
			var v = ModEntities.TOWNSPERSON.create(w);
			if (v == null) continue;
			v.setVariant(looks[i]);
			v.setDancing(true);
			String[] names = TownspersonEntity.NAMES;
			double a = i * Math.PI * 2 / looks.length;
			v.refreshPositionAndAngles(f.x + Math.cos(a) * 3.5, f.y, f.z + Math.sin(a) * 3.5, (float) Math.toDegrees(a) + 90f, 0f);
			v.setCustomName(Text.literal(i == 0 ? "Paulina" : i == 2 ? "Tucker" : names[looks[i]]));
			v.setCustomNameVisible(i < 3);
			v.setPersistent();
			v.setAiDisabled(true);
			v.addCommandTag("phantom_dance");
			v.addCommandTag("phantom_story_npc");
			if (i == 0) v.addCommandTag("phantom_dance_date");
			if (i == 1) v.addCommandTag("phantom_dance_sam");
			w.spawnEntity(v);
		}
	}

	/** Sam loses her temper in the girls' bathroom... and the dragon smashes out of the school. */
	private static void startWyvern(ServerPlayerEntity p, BlockPos c) {
		ServerWorld w = p.getServerWorld();
		Vec3d from = AmityPark.bathroom(c);
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(from)).expand(32),
				v -> v.getCommandTags().contains("phantom_dance_sam"))) {
			from = v.getPos();
			v.discard();
		}
		p.sendMessage(Text.literal("The necklace flares red as Sam gets angrier and angrier... and she TRANSFORMS!").formatted(Formatting.RED, Formatting.ITALIC), false);
		p.sendMessage(Text.literal("CRASH! A huge dragon bursts out of the bathroom and smashes its way outside the school!").formatted(Formatting.RED), false);
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, from.x, from.y + 1, from.z, 1, 0, 0, 0, 0);
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, from.x, from.y + 1, from.z, 80, 1.2, 1.2, 1.2, 0.2);
		w.playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 2f, 0.8f);
		spawnWyvern(w, c, p, AmityPark.schoolDoor(c).add(0, 8, 12));
	}

	private static void spawnWyvern(ServerWorld w, BlockPos c, ServerPlayerEntity p, Vec3d at) {
		RageWyvernEntity wy = ModEntities.RAGE_WYVERN.create(w);
		if (wy == null) return;
		wy.refreshPositionAndAngles(at.x, at.y + 1, at.z, 0f, 0f);
		wy.addCommandTag(RageWyvernEntity.STORY_TAG);
		wy.setWearer("Sam", TownspersonEntity.LENA);
		wy.setCustomName(Text.literal("Sam (Dragon)"));
		wy.setHome(BlockPos.ofFloored(AmityPark.schoolDoor(c).add(0, 0, 14)).up(6), 26f);
		wy.setTarget(p);
		w.spawnEntity(wy);
		w.spawnParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
		w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1, at.z, 120, 1.2, 1.5, 1.2, 0.15);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 2.5f, 1.1f);
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 15));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("DRAGON!").formatted(Formatting.DARK_AQUA, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Sam turned into a dragon!").formatted(Formatting.GRAY)));
		// the other dancers run for it
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, new Box(BlockPos.ofFloored(at)).expand(24),
				v -> v.getCommandTags().contains("phantom_dance") && !v.getCommandTags().contains("phantom_dance_sam"))) {
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
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Skulker").formatted(Formatting.GOLD, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(chase ? "\"Such a RARE specimen...\"" : "\"Now, where were we?\"").formatted(Formatting.GRAY)));
	}

	/** You made it to the Ghost Lab: the Collector backs off (for now). */
	private static void callOffChase(ServerWorld w, ServerPlayerEntity p) {
		for (var col : w.getEntitiesByClass(com.phantompowers.entity.CollectorEntity.class, p.getBoundingBox().expand(96), com.phantompowers.entity.CollectorEntity::chasing)) {
			w.spawnParticles(GhostPowers.GREEN, col.getX(), col.getY() + 1, col.getZ(), 60, 0.6, 1, 0.6, 0.1);
			col.discard();
		}
		p.sendMessage(Text.literal("Skulker: \"Hide, then. I can wait. Every hunt ends with a trophy...\"").formatted(Formatting.GOLD), false);
	}

	/** The rare snow fox in the park. */
	private static void spawnFox(ServerWorld w, BlockPos c) {
		Vec3d at = AmityPark.parkMeadow(c);
		if (!w.getEntitiesByClass(net.minecraft.entity.passive.FoxEntity.class, new Box(BlockPos.ofFloored(at)).expand(80), f -> f.getCommandTags().contains("phantom_story_fox")).isEmpty()) return;
		var fox = net.minecraft.entity.EntityType.FOX.create(w);
		if (fox == null) return;
		fox.setVariant(net.minecraft.entity.passive.FoxEntity.Type.SNOW);
		at = clearSpot(w, at);
		fox.refreshPositionAndAngles(at.x, at.y, at.z, 0f, 0f);
		fox.setGlowing(true);   // easy to spot, even at night
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
		if (ghost.getCommandTags().contains("phantom_ep_boss")) {
			GhostState st = GhostState.of(w.getServer());
			for (ServerPlayerEntity p : w.getPlayers(sp -> sp == by || sp.squaredDistanceTo(ghost) < 64 * 64)) {
				GhostData d = st.data(p.getUuid());
				if (goal(d).equals(Episodes.BOSS) || goal(d).equals(Episodes.TRAP)) {
					d.storyCount = 1;
					st.markDirty();
				}
			}
			return;
		}
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
				by.sendMessage(Text.literal("Skulker dropped Sam's tablet! You got it back.").formatted(Formatting.GREEN), false);
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
		// Sam is back (the dragon spawns her): tag her so "check on Paige" can find her
		for (var v : w.getEntitiesByClass(TownspersonEntity.class, wyvern.getBoundingBox().expand(6), v -> v.getCommandTags().contains("phantom_dance")
				&& v.getCustomName() != null && v.getCustomName().getString().startsWith("Sam"))) {
			v.addCommandTag("phantom_paige_saved");
			v.setAiDisabled(true);
			v.setCustomNameVisible(true);
		}
	}

	/** Ghost kills count for "destroy the ghosts" objectives. */
	public static void onDeath(LivingEntity victim, DamageSource source) {
		if (victim.getCommandTags().contains("phantom_ep_minion") && victim.getWorld() instanceof ServerWorld ew) {
			GhostState st = GhostState.of(ew.getServer());
			for (ServerPlayerEntity p : ew.getPlayers(sp -> sp.squaredDistanceTo(victim) < 64 * 64 || sp == source.getAttacker())) {
				GhostData d = st.data(p.getUuid());
				if (!goal(d).equals(Episodes.KILL)) continue;
				d.storyCount++;
				st.markDirty();
				Episodes.S es = Episodes.step(d.storyMission, d.storyStep);
				if (es != null) p.sendMessage(Text.literal("Defeated: " + Math.min(es.n(), d.storyCount) + "/" + es.n()).formatted(Formatting.AQUA), true);
				send(p, d, false);
			}
			return;
		}
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

	// ------------------------------------------------------------ episodes (missions 5+, and the specials)

	/** On a mission right now (a story mission or a special)? */
	static boolean active(GhostData d) {
		return (d.storyMission >= 1 && d.storyMission <= MISSIONS) || Episodes.special(d.storyMission);
	}

	/** Ghost hunters have no ghost powers: power objectives become "suit up" for them. */
	private static String epText(int path, Episodes.S s) {
		if (path == HUNTER && s.kind().equals(Episodes.POWER))
			return s.id().equals(Episodes.HUMAN) ? "Take the suit off (press B) and blend in" : "Suit up! (press B)";
		return s.text();
	}

	private static boolean inZone(ServerPlayerEntity p) {
		return p.getWorld().getRegistryKey().equals(PortalLogic.GHOST_ZONE);
	}

	/** A spot in Amity Park (the bigger buildings are out in the new neighbourhoods). */
	static Vec3d place(Episodes.P where, BlockPos c) {
		return switch (where) {
			case LAB_DOOR -> AmityPark.labHouseDoor(c);
			case LAB -> Vec3d.ofBottomCenter(AmityPark.lab(c));
			case CONSOLES -> AmityPark.labConsoles(c);
			case TOWER -> new Vec3d(c.getX() + 30.5, c.getY() + 22, c.getZ() - 33.5);
			case SCHOOL -> AmityPark.schoolDoor(c);
			case CAFETERIA -> AmityPark.cafeteria(c);
			case DANCE_HALL -> AmityPark.danceFloor(c);
			case PARK_GATE -> AmityPark.parkGate(c);
			case FOUNTAIN -> AmityPark.parkFountain(c);
			case POND -> AmityPark.parkPond(c);
			case MEADOW -> AmityPark.parkMeadow(c);
			case STORE -> AmityPark.store(c);
			case LIBRARY -> AmityPark.library(c);
			case DINER -> new Vec3d(c.getX() - 37.5, c.getY(), c.getZ() - 12.5);
			case TOWN_HALL -> new Vec3d(c.getX() - 79.5, c.getY(), c.getZ() - 11.5);
			case APARTMENTS -> new Vec3d(c.getX() - 31.5, c.getY(), c.getZ() + 9.5);
			case ENTRANCE -> AmityPark.entrance(c);
			case MANSION -> new Vec3d(c.getX() - 197.5, c.getY(), c.getZ() - 178.5);
			case VLADCO -> new Vec3d(c.getX() + 26.5, c.getY(), c.getZ() - 178.5);
			case AXION -> new Vec3d(c.getX() + 138.5, c.getY(), c.getZ() - 178.5);
			case HOSPITAL -> new Vec3d(c.getX() - 135.5, c.getY(), c.getZ() - 178.5);
			case MUSEUM -> new Vec3d(c.getX() - 197.5, c.getY(), c.getZ() - 66.5);
			case MALL -> new Vec3d(c.getX() - 78.5, c.getY(), c.getZ() - 178.5);
			case CINEMA -> new Vec3d(c.getX() + 138.5, c.getY(), c.getZ() - 118.5);
			case LAKE -> new Vec3d(c.getX() + 200.5, c.getY(), c.getZ() + 138.5);
			case GARDENS -> new Vec3d(c.getX() + 200.5, c.getY(), c.getZ() + 200.5);
			case WAREHOUSE -> new Vec3d(c.getX() - 197.5, c.getY(), c.getZ() - 118.5);
			case POOL -> new Vec3d(c.getX() - 197.5, c.getY(), c.getZ() + 45.5);
			case FIRE_STATION -> new Vec3d(c.getX() + 200.5, c.getY(), c.getZ() - 178.5);
			default -> null;
		};
	}

	/** Is the player at this objective's spot (the Ghost Zone counts anywhere in it; "anywhere" always does)? */
	private static boolean atPlace(ServerPlayerEntity p, Episodes.P where, BlockPos c, double r) {
		if (where == Episodes.P.ZONE) return inZone(p);
		if (where == Episodes.P.ANY) return true;
		Vec3d at = place(where, c);
		return at != null && near(p, at, r);
	}

	/** Where to put things for this objective: its spot, or near the player in the Ghost Zone / anywhere. */
	private static Vec3d spawnSpot(ServerPlayerEntity p, Episodes.P where, BlockPos c, double ahead) {
		if (where == Episodes.P.ZONE || where == Episodes.P.ANY || place(where, c) == null || atPlace(p, where, c, 40)) {
			Vec3d look = Vec3d.fromPolar(0, p.getYaw());
			return p.getPos().add(look.multiply(ahead));
		}
		return place(where, c).add(0, 0, 3);
	}

	private static java.util.List<Entity> epTagged(ServerWorld w, Vec3d around, String tag, double r) {
		return w.getEntitiesByClass(Entity.class, new Box(BlockPos.ofFloored(around)).expand(r), e -> e.getCommandTags().contains(tag) && e.isAlive());
	}

	private static boolean epCheck(ServerPlayerEntity p, GhostData d, BlockPos c) {
		Episodes.S s = Episodes.step(d.storyMission, d.storyStep);
		if (s == null) return false;
		return switch (s.kind()) {
			case Episodes.GO -> atPlace(p, s.place(), c, 10);
			case Episodes.ZONE -> inZone(p);
			case Episodes.TALK -> !epTagged(p.getServerWorld(), p.getPos(), "phantom_ep_npc", 3.5).isEmpty();
			case Episodes.BOSS, Episodes.TRAP -> d.storyCount > 0;
			case Episodes.KILL, Episodes.STAY -> d.storyCount >= s.n();
			case Episodes.ITEM -> hasNamed(p, s.name());
			case Episodes.POWER -> d.storyPath == HUNTER
					? (s.id().equals(Episodes.HUMAN) ? !d.suitOn : d.suitOn)
					: switch (s.id()) {
						case Episodes.HUMAN -> !d.transformed;
						case Episodes.INVISIBLE -> d.invisible;
						case Episodes.INTANGIBLE -> d.intangible;
						case Episodes.FLY -> d.transformed && p.getAbilities().flying;
						default -> d.transformed;
					};
			default -> false;
		};
	}

	private static boolean hasNamed(ServerPlayerEntity p, String name) {
		for (int i = 0; i < p.getInventory().size(); i++) {
			ItemStack st = p.getInventory().getStack(i);
			if (!st.isEmpty() && st.contains(net.minecraft.component.DataComponentTypes.CUSTOM_NAME) && st.getName().getString().equals(name)) return true;
		}
		return false;
	}

	private static void epStarted(ServerPlayerEntity p, GhostData d, BlockPos c) {
		Episodes.S s = Episodes.step(d.storyMission, d.storyStep);
		if (s == null) return;
		ServerWorld w = p.getServerWorld();
		switch (s.kind()) {
			case Episodes.TALK -> {
				for (Entity e : epTagged(p.getServer().getOverworld(), Vec3d.ofCenter(c), "phantom_ep_npc", 300)) e.discard();
				for (Entity e : epTagged(w, p.getPos(), "phantom_ep_npc", 200)) e.discard();
				epNpc(p, s, c);
			}
			case Episodes.BOSS -> {
				// whoever you fought before slips away
				for (Entity e : epTagged(w, p.getPos(), "phantom_ep_boss", 160)) {
					w.spawnParticles(ParticleTypes.POOF, e.getX(), e.getY() + 1, e.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
					e.discard();
				}
				if (atPlace(p, s.place(), c, 40)) spawnEpBoss(p, s, c);
			}
			case Episodes.KILL -> {
				if (atPlace(p, s.place(), c, 30)) spawnEpMinions(p, s, c, s.n());
			}
			case Episodes.ITEM -> placeEpItem(p, s, c);
			case Episodes.TRAP -> p.sendMessage(Text.literal(d.storyPath == HUNTER || has(p, ModItems.THERMOS)
					? "Aim the Thermos at the weakened ghost and right-click!"
					: "You need a Thermos! There's one in the Ghost Lab gear locker.").formatted(Formatting.AQUA), true);
			default -> {}
		}
	}

	private static void epUpkeep(ServerPlayerEntity p, GhostData d, GhostState st, BlockPos c, long t) {
		Episodes.S s = Episodes.step(d.storyMission, d.storyStep);
		if (s == null) return;
		ServerWorld w = p.getServerWorld();
		switch (s.kind()) {
			case Episodes.TALK -> {
				if (t % 60 == 0) epNpc(p, s, c);
			}
			case Episodes.BOSS -> {
				var bosses = epTagged(w, p.getPos(), "phantom_ep_boss", 96);
				if (bosses.isEmpty()) {
					if (t % 40 == 0 && d.storyCount == 0 && atPlace(p, s.place(), c, 40)) spawnEpBoss(p, s, c);
				} else if (bosses.get(0) instanceof LivingEntity boss) {
					if (!boss.getCommandTags().contains("phantom_ep_dazed") && boss.getHealth() <= boss.getMaxHealth() * 0.25f) dazeBoss(boss);
					if (boss.getCommandTags().contains("phantom_ep_dazed") && d.storyCount == 0) {
						d.storyCount = 1;
						st.markDirty();
					}
				}
			}
			case Episodes.TRAP -> {
				if (t % 20 == 0 && d.storyCount == 0 && epTagged(w, p.getPos(), "phantom_ep_boss", 96).isEmpty()) {
					d.storyCount = 1;   // it's gone (trapped by someone else, or finished off)
					st.markDirty();
				}
			}
			case Episodes.KILL -> {
				if (t % 20 == 0 && d.storyCount < s.n() && atPlace(p, s.place(), c, 30)
						&& epTagged(w, p.getPos(), "phantom_ep_minion", 64).isEmpty()) spawnEpMinions(p, s, c, s.n() - d.storyCount);
			}
			case Episodes.ITEM -> {
				if (t % 40 == 0) placeEpItem(p, s, c);
			}
			case Episodes.STAY -> {
				if (atPlace(p, s.place(), c, 7)) {
					d.storyCount++;
					st.markDirty();
					if (d.storyCount % 4 == 0) w.spawnParticles(GhostPowers.GREEN, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.6, 0.4, 0.01);
				} else if (t % 40 == 0 && atPlace(p, s.place(), c, 30)) {
					p.sendMessage(Text.literal("Get a little closer!").formatted(Formatting.YELLOW), true);
				}
			}
			default -> {}
		}
	}

	private static void epNpc(ServerPlayerEntity p, Episodes.S s, BlockPos c) {
		ServerWorld w = s.place() == Episodes.P.ZONE || s.place() == Episodes.P.ANY ? p.getServerWorld() : p.getServer().getOverworld();
		if (s.place() == Episodes.P.ZONE && !inZone(p)) return;
		Vec3d at = spawnSpot(p, s.place(), c, 4);
		if (!epTagged(w, at, "phantom_ep_npc", 64).isEmpty()) return;
		var v = spawnNpc(w, at, s.name(), "phantom_ep_npc", true, s.look());
		if (v != null) v.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, p.getEyePos());
	}

	/** Create one of the mod's (or Minecraft's) creatures by id. */
	private static Entity epCreate(ServerWorld w, String id) {
		var type = net.minecraft.registry.Registries.ENTITY_TYPE.get(net.minecraft.util.Identifier.of(id));
		return type == null ? null : type.create(w);
	}

	private static void spawnEpBoss(ServerPlayerEntity p, Episodes.S s, BlockPos c) {
		ServerWorld w = p.getServerWorld();
		if (!epTagged(w, p.getPos(), "phantom_ep_boss", 96).isEmpty()) return;
		if (w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
			p.sendMessage(Text.literal("Ghosts can't appear on Peaceful! Turn the difficulty up to Easy or higher for this mission.").formatted(Formatting.RED), true);
			return;
		}
		Entity e = epCreate(w, s.id());
		if (!(e instanceof LivingEntity boss)) return;
		Vec3d at = clearSpot(w, spawnSpot(p, s.place(), c, 7));
		boss.refreshPositionAndAngles(at.x, at.y + (boss instanceof AbstractGhostEntity ? 1 : 0), at.z, p.getYaw() + 180f, 0f);
		var hp = boss.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
		if (hp != null) hp.setBaseValue(s.n());
		boss.setHealth(boss.getMaxHealth());
		boss.setCustomName(Text.literal(s.name()));
		boss.setCustomNameVisible(true);
		boss.addCommandTag("phantom_ep_boss");
		if (boss instanceof MobEntity m) {
			m.setPersistent();
			m.setTarget(p);
		}
		if (boss instanceof AbstractGhostEntity g) g.setHome(BlockPos.ofFloored(at), 24f);
		w.spawnEntity(boss);
		w.spawnParticles(ParticleTypes.FLASH, at.x, at.y + 1, at.z, 1, 0, 0, 0, 0);
		w.spawnParticles(GhostPowers.GREEN, at.x, at.y + 1, at.z, 60, 0.8, 1.2, 0.8, 0.1);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6f, 1.4f);
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 10));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(s.name()).formatted(Formatting.RED, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("Weaken it, then trap it!").formatted(Formatting.GRAY)));
	}

	private static void spawnEpMinions(ServerPlayerEntity p, Episodes.S s, BlockPos c, int n) {
		ServerWorld w = p.getServerWorld();
		if (w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
			p.sendMessage(Text.literal("Ghosts can't appear on Peaceful! Turn the difficulty up to Easy or higher for this mission.").formatted(Formatting.RED), true);
			return;
		}
		Vec3d base = spawnSpot(p, s.place(), c, 6);
		for (int i = 0; i < n; i++) {
			Entity e = epCreate(w, s.id());
			if (e == null) continue;
			double a = i * Math.PI * 2 / Math.max(1, n) + w.random.nextDouble();
			Vec3d at = clearSpot(w, base.add(Math.cos(a) * 4, 0, Math.sin(a) * 4));
			e.refreshPositionAndAngles(at.x, at.y + (e instanceof AbstractGhostEntity ? 1 : 0), at.z, 0f, 0f);
			e.addCommandTag("phantom_ep_minion");
			e.setGlowing(true);
			if (s.name() != null) e.setCustomName(Text.literal(s.name()));
			if (e instanceof MobEntity m) {
				m.setPersistent();
				m.setTarget(p);
			}
			w.spawnEntity(e);
			w.spawnParticles(GhostPowers.GREEN, at.x, at.y + 1, at.z, 20, 0.4, 0.6, 0.4, 0.05);
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1f, 1.2f);
		p.sendMessage(Text.literal("Here they come!").formatted(Formatting.RED), true);
	}

	private static void placeEpItem(ServerPlayerEntity p, Episodes.S s, BlockPos c) {
		if (hasNamed(p, s.name())) return;
		ServerWorld w = p.getServerWorld();
		if (s.place() == Episodes.P.ZONE ? !inZone(p) : !atPlace(p, s.place(), c, 64)) return;
		Vec3d at = clearSpot(w, spawnSpot(p, s.place(), c, 3));
		if (!w.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, new Box(BlockPos.ofFloored(at)).expand(16),
				e -> e.getStack().getName().getString().equals(s.name())).isEmpty()) return;
		var type = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of(s.id()));
		ItemStack stack = new ItemStack(type);
		stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_NAME, Text.literal(s.name()).formatted(Formatting.GREEN));
		var item = new net.minecraft.entity.ItemEntity(w, at.x, at.y + 0.3, at.z, stack, 0, 0, 0);
		item.setNeverDespawn();
		item.setGlowing(true);
		w.spawnEntity(item);
	}

	/** An episode ghost has had enough: it slows right down and can be trapped. */
	private static void dazeBoss(LivingEntity boss) {
		boss.addCommandTag("phantom_ep_dazed");
		boss.setHealth(Math.max(1f, Math.min(boss.getHealth(), boss.getMaxHealth() * 0.2f)));
		boss.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 20 * 600, 4, false, true));
		boss.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.WEAKNESS, 20 * 600, 2, false, true));
		boss.setGlowing(true);
		if (boss instanceof MobEntity m) m.setTarget(null);
		boss.setCustomName(Text.literal((boss.getCustomName() == null ? boss.getName().getString() : boss.getCustomName().getString()) + " (weak!)"));
		if (boss.getWorld() instanceof ServerWorld w) {
			w.spawnParticles(ParticleTypes.WAX_ON, boss.getX(), boss.getY() + boss.getHeight(), boss.getZ(), 20, 0.5, 0.3, 0.5, 0.05);
			for (ServerPlayerEntity p : w.getPlayers(p -> p.squaredDistanceTo(boss) < 64 * 64))
				p.sendMessage(Text.literal(boss.getName().getString() + " is worn out!").formatted(Formatting.YELLOW), true);
		}
	}

	/** Episode ghosts can't be finished off before they're weakened (then they can be trapped). */
	public static boolean allowDeath(LivingEntity e) {
		if (!e.getCommandTags().contains("phantom_ep_boss") || e.getCommandTags().contains("phantom_ep_dazed")) return true;
		dazeBoss(e);
		return false;
	}

	/** Clear away episode creatures nobody's current objective needs. */
	private static void epTidy(ServerPlayerEntity p, GhostData d) {
		String g = goal(d);
		ServerWorld w = p.getServerWorld();
		if (!g.equals(Episodes.BOSS) && !g.equals(Episodes.TRAP))
			for (Entity e : epTagged(w, p.getPos(), "phantom_ep_boss", 200)) e.discard();
		if (!g.equals(Episodes.KILL))
			for (Entity e : epTagged(w, p.getPos(), "phantom_ep_minion", 200)) e.discard();
		if (!g.equals(Episodes.TALK))
			for (Entity e : epTagged(w, p.getPos(), "phantom_ep_npc", 300)) e.discard();
	}

	private static void epRewards(ServerPlayerEntity p, GhostData d, int mission) {
		give(p, new ItemStack(ModItems.SPECTRAL_SHARD, 2));
		give(p, new ItemStack(d.storyPath == HUNTER ? ModItems.GHOST_CORE : ModItems.ESSENCES[d.storyPath == EVIL ? GhostPowers.ESS_SHADE : GhostPowers.ESS_FRIENDLY]));
		Episodes.Ep ep = Episodes.get(mission);
		boolean big = ep != null && (Episodes.special(mission) || ep.title().contains("Part 2") || mission == Episodes.LAST);
		if (big) {
			give(p, new ItemStack(ModItems.PHANTOM_HEART));
			give(p, new ItemStack(ModItems.GHOST_CORE, 2));
		}
	}

	/** Play one of the TV movies or specials (your story waits until you're done). */
	public static void startSpecial(ServerPlayerEntity p, int index) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (index < 0 || index >= Episodes.SPECIALS.size()) return;
		if (d.storyPath == NONE) {
			p.sendMessage(Text.literal("Pick a story path first (Story Mode), then you can play the movies and specials.").formatted(Formatting.YELLOW), false);
			return;
		}
		if (Episodes.special(d.storyMission)) {
			p.sendMessage(Text.literal("You're already playing a special. Finish it or leave it first.").formatted(Formatting.YELLOW), false);
			return;
		}
		if (d.possessing != null) GhostPowers.stopPossess(p, d, false);
		d.storyResumeMission = d.storyMission;
		d.storyResumeStep = d.storyStep;
		d.storyMission = Episodes.SPECIAL_BASE + index;
		d.storyStep = 0;
		d.storyCount = 0;
		st.markDirty();
		dismissExtras(p.getServer());
		epTidy(p, d);
		announce(p, d);
		stepStarted(p, d);
		send(p, d, false);
	}

	/** /story info: what the story thinks is going on (handy when something seems stuck). */
	public static void debug(ServerPlayerEntity p) {
		GhostData d = GhostState.of(p.getServer()).data(p.getUuid());
		Episodes.S s = Episodes.step(d.storyMission, d.storyStep);
		BlockPos c = AmityPark.state(p.getServer()).center;
		String where = s == null || place(s.place(), c) == null ? "-" : (int) Math.sqrt(p.getPos().squaredDistanceTo(place(s.place(), c))) + " blocks away";
		p.sendMessage(Text.literal("Mission " + d.storyMission + ", objective " + (d.storyStep + 1) + ", progress " + d.storyCount
				+ (s == null ? "" : ", type " + s.kind() + ", place " + s.place() + " (" + where + ")")
				+ ", minions nearby " + epTagged(p.getServerWorld(), p.getPos(), "phantom_ep_minion", 64).size()
				+ ", bosses nearby " + epTagged(p.getServerWorld(), p.getPos(), "phantom_ep_boss", 96).size()
				+ ", difficulty " + p.getServerWorld().getDifficulty().getName()).formatted(Formatting.GRAY), false);
	}

	/** Leave a special early and go back to your story. */
	public static void quitSpecial(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (!Episodes.special(d.storyMission)) return;
		d.storyMission = d.storyResumeMission;
		d.storyStep = d.storyResumeStep;
		d.storyCount = 0;
		d.storyResumeMission = 0;
		st.markDirty();
		epTidy(p, d);
		dismissExtras(p.getServer());
		p.sendMessage(Text.literal("You left the special. Back to your story!").formatted(Formatting.YELLOW), false);
		if (active(d)) stepStarted(p, d);
		send(p, d, false);
	}

	// ------------------------------------------------------------ the end

	private static void complete(ServerPlayerEntity p, GhostData d) {
		int mission = d.storyMission;
		ServerWorld w = p.getServerWorld();
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 80, 20));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("Complete!").formatted(Formatting.GOLD, Formatting.BOLD)));
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
		if (mission >= Episodes.FIRST) epRewards(p, d, mission);
		cleanUpNpcs(w, p);
		epTidy(p, d);
		if (Episodes.special(mission)) {
			p.sendMessage(Text.literal("Special complete! You got some rewards. Back to your story...").formatted(Formatting.GOLD), false);
			d.storyMission = d.storyResumeMission;
			d.storyStep = d.storyResumeStep;
			d.storyCount = 0;
			d.storyResumeMission = 0;
			java.util.UUID id = p.getUuid();
			MinecraftServer server = p.getServer();
			com.phantompowers.entity.BossMoves.later(120, () -> {
				ServerPlayerEntity sp = server.getPlayerManager().getPlayer(id);
				if (sp != null) send(sp, GhostState.of(server).data(id), false);
			});
			return;
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
			p.sendMessage(Text.literal("You finished the whole story! The world knows Danny Phantom... and you saved it. Replay the movies and specials any time from Story Mode.").formatted(Formatting.GOLD), false);
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

	/**
	 * Un-stick the story: let go of anyone you were overshadowing, clear away story characters that got left in the
	 * wrong place (they come straight back where they belong), and set the current objective up again.
	 * {@code full} also restarts the objective's timer and re-runs its scene (/story fix).
	 */
	public static void fix(ServerPlayerEntity p, boolean full) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.storyPath == NONE || !active(d)) return;
		if (d.possessing != null) GhostPowers.stopPossess(p, d, false);
		if (AmityPark.ready(p.getServer())) {
			ServerWorld ow = p.getServer().getOverworld();
			BlockPos c = AmityPark.state(p.getServer()).center;
			for (var v : ow.getEntitiesByClass(TownspersonEntity.class, new Box(c).expand(AmityPark500.HALF, 64, AmityPark500.HALF),
					v -> (v.getCommandTags().contains("phantom_story_npc") || v.getCommandTags().contains("phantom_dance"))
							&& !v.getCommandTags().contains("phantom_paige_saved"))) v.discard();
		}
		dismissExtras(p.getServer());
		if (full) {
			d.storyCount = 0;
			st.markDirty();
			stepStarted(p, d);
			p.sendMessage(Text.literal("Story fixed! Your objective has been set up again: " + objective(d)).formatted(Formatting.GREEN), false);
		}
		send(p, d, false);
	}

	/** The story characters an objective needs in town (everyone else goes home). */
	private static java.util.Set<String> castFor(String goal) {
		return switch (goal) {
			case FRIEND -> java.util.Set.of("phantom_story_friend");
			case TEACHER, OVERSHADOW -> java.util.Set.of("phantom_story_teacher");
			case TROUBLE, DAD, LANCER -> java.util.Set.of("phantom_story_dad", "phantom_story_teacher");
			case TUCKER, ASK_SAM -> java.util.Set.of("phantom_story_tucker", "phantom_story_friend");
			case TALK_PAIGE, SNEAK_PAIGE -> java.util.Set.of("phantom_paige_park");
			case FOUNTAIN, DANCE -> java.util.Set.of("phantom_dance");
			case BATHROOM -> java.util.Set.of("phantom_bath_paulina", "phantom_bath_sam");
			case WYVERN_DAZED, AMULET_PULLED, CHECK_PAIGE -> java.util.Set.of("phantom_paige_saved");
			case Episodes.TALK -> java.util.Set.of("phantom_ep_npc");
			default -> java.util.Set.of();
		};
	}

	/** After an objective: story characters nobody's current objective needs leave town (with a puff). */
	private static void dismissExtras(MinecraftServer server) {
		if (!AmityPark.ready(server)) return;
		GhostState st = GhostState.of(server);
		java.util.Set<String> keep = new java.util.HashSet<>();
		for (ServerPlayerEntity q : server.getPlayerManager().getPlayerList()) {
			GhostData qd = st.data(q.getUuid());
			if (qd.storyPath != NONE && active(qd)) keep.addAll(castFor(goal(qd)));
		}
		ServerWorld ow = server.getOverworld();
		BlockPos c = AmityPark.state(server).center;
		for (var v : ow.getEntitiesByClass(TownspersonEntity.class, new Box(c).expand(AmityPark500.HALF, 64, AmityPark500.HALF),
				v -> v.getCommandTags().contains("phantom_story_npc") || v.getCommandTags().contains("phantom_dance"))) {
			if (GhostFlags.HOSTS.contains(v.getUuid())) continue;   // someone's inside them right now
			boolean needed = false;
			for (String tag : v.getCommandTags()) if (keep.contains(tag)) { needed = true; break; }
			if (needed) continue;
			ow.spawnParticles(ParticleTypes.POOF, v.getX(), v.getY() + 1, v.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
			v.discard();
		}
	}

	/** Skip the current objective (for when something's gone wrong and it can't be finished). */
	public static void skip(ServerPlayerEntity p) {
		GhostState st = GhostState.of(p.getServer());
		GhostData d = st.data(p.getUuid());
		if (d.storyPath == NONE || !active(d)) {
			p.sendMessage(Text.literal("You're not on a story mission right now.").formatted(Formatting.YELLOW), false);
			return;
		}
		p.sendMessage(Text.literal("Skipped: " + objective(d)).formatted(Formatting.YELLOW), false);
		if (d.possessing != null) GhostPowers.stopPossess(p, d, false);
		advance(p, d, st);
	}

	private static void startMission(ServerPlayerEntity p, GhostData d, int mission) {
		d.storyMission = mission;
		d.storyStep = 0;
		d.storyCount = 0;
		announce(p, d);
	}

	/** "Mission 2" big, with the mission's name underneath (the whole title in big letters doesn't fit on screen). */
	private static void showMissionTitle(ServerPlayerEntity p, int path, int mission) {
		String full = missionTitle(path, mission).replace(" (Dark Side)", "");
		int colon = full.indexOf(':');
		String big = colon > 0 ? full.substring(0, colon) : full, small = colon > 0 ? full.substring(colon + 1).trim() : "";
		if (path == EVIL) small += " (Dark Side)";
		p.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
		p.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(big).formatted(path == EVIL ? Formatting.LIGHT_PURPLE : Formatting.GREEN, Formatting.BOLD)));
		p.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(small).formatted(Formatting.WHITE)));
	}

	/** Show a mission's title and intro. */
	private static void announce(ServerPlayerEntity p, GhostData d) {
		showMissionTitle(p, d.storyPath, d.storyMission);
		Episodes.Ep ep = Episodes.get(d.storyMission);
		if (ep != null) p.sendMessage(Text.literal(ep.code() + ": " + ep.intro()).formatted(Formatting.GRAY), false);
		p.getServerWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.8f, 1.2f);
		if (d.storyMission == 4) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "Another long night on ghost patrol... and you've got a biology test in the morning. Somewhere out there, a ghost hunter is hunting YOU.";
			case EVIL -> "Busy night. A pest is smashing up the Quick Stop, there's a biology test tomorrow, and something out there has started collecting rare ghosts...";
			default -> "Being half-ghost means a lot of late nights. Tonight the Box Ghost is acting up, and tomorrow is your biology test. Oh, and somebody out there collects rare things...";
		}).formatted(Formatting.GRAY), false);
		if (d.storyMission == 3) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "The Amity Park dance is tonight and you're on chaperone duty. But the ghost sensors just picked up something strange in the park...";
			case EVIL -> "The big dance is tonight. You've got no date and no patience. But something in the park is calling to you: something angry...";
			default -> "The big dance is tonight and you want to ask Paulina, the most popular girl at Casper High! You just need the perfect gift... and to stay out of trouble at school.";
		}).formatted(Formatting.GRAY), false);
		if (d.storyMission == 2) p.sendMessage(Text.literal(switch (d.storyPath) {
			case HUNTER -> "Ghost sensors are going crazy over at Casper High. Something in the cafeteria is VERY unhappy...";
			case EVIL -> "Your new powers keep acting up on their own. Meanwhile, some ghost is throwing a tantrum at Casper High. Nobody haunts YOUR town but you.";
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
		Vec3d target = null;
		String dim = "minecraft:overworld";
		if (d.storyPath != NONE && active(d)) {
			if (!AmityPark.ready(p.getServer())) obj += " (Amity Park is being built: " + AmityPark.progress(p.getServer()) + "%)";
			else {
				target = compassTarget(p, d);
				if (target != null && (goal(d).equals(Episodes.BOSS) || goal(d).equals(Episodes.TRAP) || goal(d).equals(Episodes.KILL)
						|| goal(d).equals(Episodes.TALK) || goal(d).equals(Episodes.ITEM))) dim = p.getWorld().getRegistryKey().getValue().toString();
			}
		}
		ServerPlayNetworking.send(p, new StoryPayload(openPicker, d.storyPath, missionTitle(d.storyPath, d.storyMission), obj,
				Math.min(d.storyStep, s.length), s.length, target != null, target == null ? 0 : target.x, target == null ? 0 : target.y,
				target == null ? 0 : target.z, dim));
	}
}
