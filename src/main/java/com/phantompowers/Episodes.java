package com.phantompowers;

import java.util.List;

/**
 * The rest of the story, one mission per episode: from season 1 episode 4 to the season 3 finale, plus the TV movies
 * and specials as stand-alone missions. Each mission is a list of simple objectives (go somewhere, talk to someone,
 * fight a ghost until it's weak, trap it, clear out minions, find something, hold a spot, use a power).
 */
public final class Episodes {
	private Episodes() {}

	/** Where an objective happens (Story turns these into spots in Amity Park). */
	public enum P {
		LAB_DOOR, LAB, CONSOLES, TOWER, SCHOOL, CAFETERIA, DANCE_HALL, PARK_GATE, FOUNTAIN, POND, MEADOW, STORE, LIBRARY,
		DINER, TOWN_HALL, APARTMENTS, ENTRANCE, MANSION, VLADCO, AXION, HOSPITAL, MUSEUM, MALL, CINEMA, LAKE, GARDENS,
		WAREHOUSE, POOL, FIRE_STATION,
		/** anywhere in the Ghost Zone */
		ZONE,
		/** wherever you are */
		ANY
	}

	public static final String GO = "ep_go", ZONE = "ep_zone", TALK = "ep_talk", BOSS = "ep_boss", TRAP = "ep_trap", KILL = "ep_kill",
			ITEM = "ep_item", STAY = "ep_stay", POWER = "ep_power";

	/** Power objectives. */
	public static final String GHOST = "ghost", HUMAN = "human", INVISIBLE = "invisible", INTANGIBLE = "intangible", FLY = "fly";

	// townsperson looks (see TownspersonEntity)
	static final int SAM = 0, LANCER = 1, PRINCESS = 2, DASH = 3, PAULINA = 4, TUCKER = 5, JAZZ = 6, DAD = 7;
	static final int VLAD = DASH, VALERIE = JAZZ, MOM = JAZZ, OTHER = DAD;

	public record S(String kind, P place, String text, String id, String name, int n, int look, String[] lines) {}

	public record Ep(String title, String code, String intro, S[] steps) {}

	static S go(P place, String text) {
		return new S(GO, place, text, null, null, 0, 0, new String[0]);
	}

	static S zone(String text) {
		return new S(ZONE, P.ZONE, text, null, null, 0, 0, new String[0]);
	}

	static S talk(P place, String who, int look, String text, String... lines) {
		return new S(TALK, place, text, null, who, 0, look, lines);
	}

	/** A ghost to fight until it's weak (it can't be killed before then). */
	static S boss(P place, String id, String name, int hp, String text) {
		return new S(BOSS, place, text, id, name, hp, 0, new String[0]);
	}

	static S trap(String text) {
		return new S(TRAP, P.ANY, text, null, null, 0, 0, new String[0]);
	}

	static S kill(P place, int n, String text) {
		return kill(place, n, "phantom:hostile_ghost", null, text);
	}

	static S kill(P place, int n, String id, String name, String text) {
		return new S(KILL, place, text, id, name, n, 0, new String[0]);
	}

	static S item(P place, String itemId, String name, String text) {
		return new S(ITEM, place, text, itemId, name, 0, 0, new String[0]);
	}

	static S stay(P place, int seconds, String text) {
		return new S(STAY, place, text, null, null, seconds * 2, 0, new String[0]);
	}

	static S power(String kind, String text) {
		return new S(POWER, P.ANY, text, kind, null, 0, 0, new String[0]);
	}

	static Ep ep(String title, String code, String intro, S... steps) {
		return new Ep(title, code, intro, steps);
	}

	static S[] join(S[] a, S[] b) {
		S[] out = java.util.Arrays.copyOf(a, a.length + b.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	static final String GHOST_ID = "phantom:hostile_ghost", KNIGHT = "phantom:specter_knight", GHOUL = "phantom:ghoul",
			SHADE = "phantom:shade", JAILER = "phantom:ghost_jailer", SPECTRA = "phantom:dolora", BOX = "phantom:crate_king",
			ROCKER = "phantom:voltage", GENIE = "phantom:wishkeeper", BIKER = "phantom:rev", KITTY = "phantom:roxy",
			HUNTER_GHOST = "phantom:collector", DRAGON = "phantom:rage_wyvern", AGENT = "minecraft:pillager";

	// ---------------------------------------------------------------- the two-part episodes (also the TV movies)

	static final S[] REIGN_1 = {
			talk(P.VLADCO, "Vlad Masters", VLAD, "Something's shaking the Ghost Zone. Ask Vlad what he's done (at Vladco)",
					"Vlad: \"I may have borrowed the Ring of Rage and the Crown of Fire from a certain sleeping king.\"",
					"Vlad: \"And he may have woken up. Pariah Dark, the Ghost King. Do try to keep up, Daniel.\""),
			kill(P.FOUNTAIN, 6, "Ghosts are fleeing the Ghost Zone into Amity Park! Fight them off in the park"),
			boss(P.TOWN_HALL, KNIGHT, "The Fright Knight", 320, "Pariah's general, the Fright Knight, is pulling the whole town into the Ghost Zone! Fight him at Town Hall"),
			go(P.LAB, "The Fright Knight fell back. Regroup in the Ghost Lab basement")};
	static final S[] REIGN_2 = {
			talk(P.LAB, "Dad", DAD, "Your parents built something big for this. Talk to your dad in the lab",
					"Dad: \"Behold! The Fenton Ghost Catcher Battle Suit! Strength of a hundred ghosts... might hurt a little.\"",
					"You: \"Might hurt a LOT. But it's the only way to beat him.\""),
			zone("Pariah's army is waiting. Enter the Ghost Zone through the portal"),
			kill(P.ZONE, 6, "Ghosts you've fought before are fighting beside you! Break through Pariah's skeleton army"),
			boss(P.ZONE, GHOUL, "Pariah Dark", 600, "Pariah Dark, the Ghost King! Hit him with everything you've got"),
			trap("He's weak! Seal the Ghost King away in the Thermos (it's like the Sarcophagus of Forever Sleep)"),
			go(P.LAB, "Head home. Amity Park is safe, and it's thanks to you")};
	static final S[] ENEMY_1 = {
			talk(P.SCHOOL, "Mr. Lancer", LANCER, "The big career test is tomorrow. Talk to Mr. Lancer outside Casper High",
					"Mr. Lancer: \"Your future depends on this test! The answers are locked away somewhere very, very safe.\"",
					"You: \"...Somewhere safe. Right.\""),
			stay(P.CAFETERIA, 8, "Sneak into the school and look at the test answers (stay in the cafeteria where they're hidden)"),
			kill(P.SCHOOL, 4, "Ghosts from the future are coming after you! Fight them"),
			boss(P.SCHOOL, SHADE, "Dan Phantom", 500, "You got thrown ten years into the future... and met your evil future self, Dan Phantom! Survive him"),
			go(P.LAB, "Escape back to the Ghost Lab before he finds you again")};
	static final S[] ENEMY_2 = {
			talk(P.LAB, "Clockwork", OTHER, "A ghost who controls time is waiting in the lab. Talk to Clockwork",
					"Clockwork: \"Your future self is your worst enemy. It doesn't have to be your future.\"",
					"Clockwork: \"Go home. Protect your friends at the Nasty Burger.\""),
			go(P.DINER, "The Nasty Burger is about to blow up with everyone you love inside! Get there NOW"),
			boss(P.DINER, SHADE, "Dan Phantom", 600, "Dan came back to make sure his future happens. Stop him!"),
			trap("Trap Dan in the Fenton Thermos. Forever"),
			talk(P.DINER, "Sam", SAM, "Make sure your friends are okay",
					"Sam: \"We're fine. Thanks to you.\"",
					"You: \"I'll never become him. I promise.\"")};
	static final S[] REALITY_1 = {
			talk(P.LAB_DOOR, "Jazz", JAZZ, "Your parents are missing! Talk to Jazz at Fenton Works",
					"Jazz: \"Freakshow kidnapped Mom and Dad! He wants the Reality Gems in exchange.\"",
					"You: \"Then we'd better find them before he does.\""),
			item(P.MUSEUM, "minecraft:emerald", "Reality Gem", "Find the first Reality Gem at the museum before Freakshow does"),
			kill(P.MUSEUM, 5, "Freakshow's ghost goons are after the gem! Fight them off"),
			boss(P.MUSEUM, GHOST_ID, "Lydia", 260, "Lydia, Freakshow's tattooed ghost, wants the gem! Beat her")};
	static final S[] REALITY_2 = {
			item(P.LAKE, "minecraft:emerald", "Reality Gem", "Find the last Reality Gem by the lake"),
			boss(P.FOUNTAIN, GHOST_ID, "Freakshow (Reality Gauntlet)", 450, "Freakshow put all the gems in the Reality Gauntlet and he's rewriting the world! Stop him"),
			trap("Freakshow dropped the gauntlet! Trap Lydia and him for good in the Thermos"),
			talk(P.LAB_DOOR, "Mom", MOM, "Check on your mom at home",
					"Mom: \"You saved us... and you're the ghost boy?! We'll love you no matter what, sweetie.\"",
					"Sam used the gauntlet to make everyone forget your secret. Except your family.")};
	static final S[] PLANET_1 = {
			talk(P.TOWN_HALL, "Vlad Masters", VLAD, "A giant ghost-proof asteroid is heading for Earth! Find out what's happening at Town Hall",
					"Vlad: \"I can save the world, and I will... if the world makes me its ruler.\"",
					"You: \"There's got to be another way.\""),
			zone("Ask the ghosts for help. Go into the Ghost Zone"),
			kill(P.ZONE, 6, "Some ghosts won't listen until you beat them! Prove yourself"),
			talk(P.ZONE, "Frostbite", OTHER, "Talk to Frostbite: maybe the ghosts can make the whole planet intangible",
					"Frostbite: \"If every ghost in the zone pushes together, the Earth can phase right past it.\"",
					"Frostbite: \"We will help you, Great One.\"")};
	static final S[] PLANET_2 = {
			go(P.LAB, "Gather everyone at the Ghost Lab: the plan is to turn the whole planet intangible"),
			stay(P.CONSOLES, 15, "Power up the giant intangibility towers from the lab consoles"),
			boss(P.TOWN_HALL, GHOST_ID, "Vlad Plasmius", 500, "Vlad's furious that his plan failed. Beat him one last time!"),
			power(GHOST, "The asteroid is passing through! Go ghost and help the ghosts push"),
			talk(P.SCHOOL, "Sam", SAM, "The world knows who you are now. Celebrate with Sam outside Casper High",
					"Sam: \"The whole world is cheering for Danny Phantom.\"",
					"You: \"I know. Weird, huh? ...Want to go flying?\"")};

	static final S[] XMAS = {talk(P.LAB_DOOR, "Jazz", JAZZ, "It's Christmas and you're NOT in the mood. Talk to Jazz",
							"Jazz: \"Dad's arguing with Santa again. Maybe just... try to enjoy it?\""),
					item(P.FOUNTAIN, "minecraft:book", "The Ghost Writer's Book", "You crashed into a ghost's story... Find his glowing book in the park"),
					kill(P.FOUNTAIN, 5, "The story came to life and it's attacking! Beat the ghosts"),
					boss(P.FOUNTAIN, GHOST_ID, "Ghost Writer", 260, "The Ghost Writer wants you to learn some holiday spirit! Fight him"),
					trap("Trap the Ghost Writer in the Thermos"),
					talk(P.LAB_DOOR, "Jazz", JAZZ, "Wish your family a merry Christmas",
							"You: \"Merry Christmas, Jazz.\"",
							"Jazz: \"Merry Christmas, little brother.\"")};

	// ---------------------------------------------------------------- main story: missions 5 onwards

	public static final List<Ep> MAIN = List.of(
			// ---------------- season 1
			ep("Attack of the Killer Garage Sale", "Season 1, Episode 4",
					"Your parents are clearing out old ghost gear at a garage sale. What could go wrong?",
					go(P.LAB_DOOR, "Your parents are selling their old ghost gear at a garage sale. Head home to Fenton Works"),
					talk(P.LAB_DOOR, "Jazz", JAZZ, "Ask Jazz how the garage sale went",
							"Jazz: \"We sold everything! Somebody bought a whole box of Dad's ecto-junk.\"",
							"You: \"Wait... the junk that was full of ectoplasm?\""),
					kill(P.STORE, 4, "Haunted gadgets are attacking people at the Quick Stop! Destroy them"),
					boss(P.STORE, GHOST_ID, "Technus", 180, "Technus, Master of All Things Electronic, built himself a robot body! Fight him until he's weak"),
					trap("Trap Technus in the Thermos before he takes over every computer in town"),
					go(P.LAB_DOOR, "Bring the leftover ghost junk home to Fenton Works where it's safe")),
			ep("Splitting Images", "Season 1, Episode 5",
					"Something's wrong with your reflection at school...",
					go(P.SCHOOL, "Something's off about your reflection. Go to Casper High"),
					stay(P.SCHOOL, 6, "Look closely at the trophy case outside the school doors"),
					talk(P.SCHOOL, "Sidney Poindexter", OTHER, "A ghost from the 1950s is staring back at you. Talk to him",
							"Sidney Poindexter: \"I've been stuck in this school since 1955! Time for you to see how it feels!\"",
							"Everything flips. You're on the wrong side of the mirror..."),
					boss(P.SCHOOL, GHOST_ID, "Sidney Poindexter", 160, "Poindexter swapped places with you! Fight him to get back"),
					trap("Trap Poindexter in the Thermos"),
					talk(P.SCHOOL, "Dash", DASH, "Stand up to Dash outside the school (it's something Poindexter taught you)",
							"You: \"Not today, Dash.\"",
							"Dash: \"Whoa. ...Fenton's got a backbone now?\"")),
			ep("What You Want", "Season 1, Episode 6",
					"Tucker's tired of being the sidekick. Be careful what you wish for...",
					talk(P.SCHOOL, "Tucker", TUCKER, "Tucker's sick of being the sidekick. Talk to him outside Casper High",
							"Tucker: \"I wish I had ghost powers, just like you!\"",
							"A voice echoes: \"So you have wished it... so shall it be!\""),
					boss(P.SCHOOL, GHOST_ID, "Ghost Tucker", 180, "Tucker turned into a ghost and his envy is taking over! Fight him until he calms down"),
					boss(P.FOUNTAIN, GENIE, "Desiree", 240, "The wish came from Desiree! Stop her in the park"),
					trap("Trap Desiree in the Thermos"),
					talk(P.SCHOOL, "Tucker", TUCKER, "Check on Tucker",
							"Tucker: \"Being a ghost was awful. I'll stick to being the tech guy.\"")),
			ep("Bitter Reunions", "Season 1, Episode 7",
					"Your dad's old college friend Vlad Masters is throwing a reunion.",
					go(P.VLADCO, "Your dad's old college friend Vlad Masters invited the family to a reunion. Go to his building (Vladco)"),
					talk(P.VLADCO, "Vlad Masters", VLAD, "Meet Vlad Masters",
							"Vlad: \"Jack! And your lovely wife, Maddie. And this must be young Daniel.\"",
							"Something about him makes your ghost sense go off..."),
					kill(P.VLADCO, 3, "Ghosts are crashing the reunion! Fight them"),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 300, "Vlad is half-ghost too: Vlad Plasmius! Fight him"),
					power(HUMAN, "He's too strong for now. Change back to human and slip away"),
					go(P.LAB_DOOR, "Get your family home safe")),
			ep("Prisoners of Love", "Season 1, Episode 8",
					"The Ghost Zone has rules. And a warden.",
					go(P.LAB, "Go down to the Ghost Lab"),
					zone("Enter the Ghost Zone through the portal"),
					boss(P.ZONE, JAILER, "Walker", 260, "Walker arrested you for breaking his rules! Fight your way out"),
					kill(P.ZONE, 4, "Walker's goons are coming! Beat them"),
					go(P.LAB, "Break out and get back home through the portal")),
			ep("My Brother's Keeper", "Season 1, Episode 9",
					"There's a new counselor at Casper High. Everyone seems to feel worse after seeing her.",
					talk(P.SCHOOL, "Penelope Spectra", JAZZ, "Meet the new school counselor outside Casper High",
							"Spectra: \"Hello, dear. You look troubled. Tell me ALL about your problems.\"",
							"Her smile is a little too wide..."),
					kill(P.SCHOOL, 4, "Shadows are feeding on everyone's misery! Destroy them"),
					talk(P.LAB_DOOR, "Jazz", JAZZ, "Jazz has been acting strange. Talk to her at home",
							"Jazz: \"I know your secret. I've known for a while. I just wanted you to tell me yourself.\""),
					boss(P.SCHOOL, SPECTRA, "Penelope Spectra", 280, "The counselor is a ghost, Penelope Spectra! Fight her"),
					trap("Trap Spectra in the Thermos")),
			ep("Shades of Gray", "Season 1, Episode 10",
					"Axion Labs is getting wrecked by... a puppy?",
					go(P.AXION, "A ghost dog is wrecking Axion Labs! Get over there"),
					boss(P.AXION, GHOST_ID, "Cujo the Ghost Dog", 160, "Calm the ghost dog down (by tiring him out). Fight him"),
					trap("Catch the pup in the Thermos"),
					talk(P.SCHOOL, "Valerie", VALERIE, "Valerie's dad lost his job because of the mess. Talk to her outside school",
							"Valerie: \"That ghost ruined my life. Someone gave me a ghost-hunting suit... and I'm going to use it.\""),
					boss(P.FOUNTAIN, AGENT, "Red Huntress", 160, "The Red Huntress is hunting YOU! Hold her off until she retreats")),
			ep("Fanning the Flames", "Season 1, Episode 11",
					"A new rock star is all anyone can talk about. Her song is stuck in everyone's head.",
					go(P.SCHOOL, "A new rock star, Ember, has the whole school obsessed. Go to Casper High"),
					stay(P.SCHOOL, 8, "Her song is stuck in everyone's head... stay strong outside the school"),
					talk(P.SCHOOL, "Sam", SAM, "Sam might know how to break the spell. Talk to her",
							"Sam: \"Her power grows when people chant her name. We have to stop the concert!\""),
					boss(P.FOUNTAIN, ROCKER, "Ember McLain", 280, "Ember's concert is hypnotizing the town! Stop her"),
					trap("Trap Ember in the Thermos")),
			ep("Teacher of the Year", "Season 1, Episode 12",
					"Mr. Lancer is up for Teacher of the Year, and you have a test to retake.",
					stay(P.LIBRARY, 10, "You have to retake Mr. Lancer's test. Study in the library"),
					talk(P.SCHOOL, "Mr. Lancer", LANCER, "Hand Mr. Lancer your retake",
							"Mr. Lancer: \"A B-minus! Wonders never cease.\""),
					go(P.TOWN_HALL, "Technus has taken over the town's online game! Go to Town Hall"),
					kill(P.TOWN_HALL, 4, "Technus's digital monsters are loose! Destroy them"),
					boss(P.TOWN_HALL, GHOST_ID, "Technus", 220, "Fight Technus!"),
					trap("Trap Technus in the Thermos")),
			ep("Fright Night", "Season 1, Episode 13",
					"It's Halloween, and the museum is hosting a haunted house contest.",
					go(P.MUSEUM, "The town's Halloween haunted-house contest is at the museum. Look around"),
					item(P.MUSEUM, "minecraft:iron_sword", "Soul Shredder", "Grab the creepy old sword on display (it'd make a great prop)"),
					boss(P.MUSEUM, KNIGHT, "The Fright Knight", 320, "Taking the sword woke up the Fright Knight! Fight him"),
					trap("Trap the Fright Knight in the Thermos and end the Halloween nightmare"),
					talk(P.LAB_DOOR, "Jazz", JAZZ, "Head home and tell Jazz about your night",
							"Jazz: \"So you DIDN'T win the contest? ...Happy Halloween, little brother.\"")),
			ep("13", "Season 1, Episode 14",
					"Jazz has a new boyfriend with a motorcycle. And a shadow that moves on its own.",
					talk(P.SCHOOL, "Jazz", JAZZ, "Jazz is dating a biker named Johnny. Talk to her outside the school",
							"Jazz: \"He's charming! And mysterious. And his shadow does that weird thing.\""),
					boss(P.FOUNTAIN, SHADE, "Johnny's Shadow", 160, "Johnny's living shadow is wrecking the park! Fight it"),
					boss(P.FOUNTAIN, BIKER, "Johnny 13", 220, "Johnny 13 wants to put Kitty's spirit into Jazz's body! Stop him"),
					trap("Trap Johnny 13 in the Thermos"),
					talk(P.LAB_DOOR, "Jazz", JAZZ, "Check on Jazz at home",
							"Jazz: \"Note to self: no more guys with ghost shadows.\"")),
			ep("Public Enemies", "Season 1, Episode 15",
					"Walker's in Amity Park, and he's making his own rules.",
					go(P.TOWN_HALL, "Walker's guards are taking over Amity Park! Head to Town Hall"),
					kill(P.TOWN_HALL, 5, "Walker's guards are everywhere! Beat them"),
					stay(P.MEADOW, 8, "You've been named Public Enemy #1! Hide in the park meadow until things cool down"),
					talk(P.SCHOOL, "Sam", SAM, "Make a plan with Sam",
							"Sam: \"The other ghosts hate Walker too. What if they fight on YOUR side?\""),
					boss(P.TOWN_HALL, JAILER, "Walker", 320, "Take Walker down at Town Hall!"),
					trap("Trap Walker in the Thermos")),
			ep("Lucky in Love", "Season 1, Episode 16",
					"Paulina suddenly wants to date you. That can't be right.",
					talk(P.SCHOOL, "Paulina", PAULINA, "Paulina suddenly wants to date YOU. Talk to her outside the school",
							"Paulina: \"You're my boyfriend now. Isn't that great?\"",
							"Her eyes flash green for a second..."),
					power(GHOST, "Something's wrong with Paulina. Go ghost"),
					boss(P.SCHOOL, KITTY, "Kitty", 200, "Kitty was overshadowing Paulina to make Johnny jealous! Fight her out"),
					boss(P.SCHOOL, BIKER, "Johnny 13", 220, "Johnny showed up, and he's jealous! Fight him too"),
					trap("Trap Johnny 13 in the Thermos")),
			ep("Maternal Instinct", "Season 1, Episode 17",
					"Your mom won a trip to a science convention. Funny, nobody else seems to be going.",
					talk(P.LAB_DOOR, "Mom", MOM, "Your mom won a trip to a science convention. Talk to her",
							"Mom: \"Just you and me, sweetie! Your father can watch the house.\""),
					go(P.VLADCO, "The 'convention' is at one of Vlad's buildings. Go there with your mom"),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 320, "It was Vlad's trick all along! Fight Plasmius"),
					kill(P.LAB_DOOR, 4, "Vlad sent ghosts after your dad at Fenton Works! Get home and fight them"),
					talk(P.LAB_DOOR, "Dad", DAD, "Check on your dad",
							"Dad: \"Ghosts attacked the house! I fought them all off myself!\" (You let him believe it.)")),
			ep("Life Lessons", "Season 1, Episode 18",
					"Skulker caught you and Valerie, and now you're both his prey.",
					go(P.LAKE, "Skulker dumped you and Valerie on his island! Get your bearings by the lake"),
					talk(P.LAKE, "Valerie", VALERIE, "You're handcuffed to Valerie. Talk to her",
							"Valerie: \"I don't like you, ghost. But I like Skulker even less.\""),
					kill(P.LAKE, 4, "Skulker's hunting drones are everywhere! Destroy them"),
					boss(P.LAKE, HUNTER_GHOST, "Skulker", 280, "Skulker's coming for you both! Fight him"),
					trap("Trap Skulker in the Thermos")),
			ep("The Million Dollar Ghost", "Season 1, Episode 19",
					"Somebody put a price on your head. A big one.",
					talk(P.TOWN_HALL, "Mayor", OTHER, "Vlad put a million-dollar reward on Danny Phantom! Find out more at Town Hall",
							"Mayor: \"One million dollars to whoever catches the ghost boy!\""),
					kill(P.FOUNTAIN, 6, "Every ghost in town is joining the hunt! Fight them off in the park"),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 340, "Confront Vlad at Vladco"),
					go(P.LAB_DOOR, "Lay low at home until the hunt dies down")),
			ep("Control Freaks", "Season 1, Episode 20",
					"Circus Gothica is in town. Its ringmaster has an eye on you.",
					go(P.MEADOW, "Circus Gothica set up in the park meadow! Go and see the show"),
					talk(P.MEADOW, "Freakshow", OTHER, "Talk to the ringmaster, Freakshow",
							"Freakshow: \"Your ghost powers would make a SPECTACULAR act. Look into the crystal ball...\""),
					stay(P.MEADOW, 8, "Freakshow's crystal ball is controlling you! Fight it (stay put)"),
					boss(P.MEADOW, GHOST_ID, "Lydia", 220, "Freakshow's ghost Lydia is guarding his crystal ball staff! Beat her"),
					trap("Trap Lydia in the Thermos and break the spell")),
			// ---------------- season 2
			ep("Memory Blank", "Season 2, Episode 1",
					"Sam's mad at you. A little too mad.",
					talk(P.SCHOOL, "Sam", SAM, "Sam's angry with you. Talk to her outside Casper High",
							"Sam: \"Sometimes I wish I'd never even met you!\"",
							"A voice echoes: \"So you have wished it... so shall it be!\""),
					stay(P.SCHOOL, 8, "Nobody remembers you! Walk around the school and hope something jogs their memory"),
					talk(P.SCHOOL, "Sam", SAM, "Remind Sam who you are",
							"Sam: \"...Danny? I remember now. I'm so sorry.\""),
					boss(P.FOUNTAIN, GENIE, "Desiree", 300, "Desiree is in the park! Make her undo the wish"),
					trap("Trap Desiree in the Thermos")),
			ep("Doctor's Disorders", "Season 2, Episode 2",
					"Kids at Casper High are getting weird, wild powers. And then getting sick.",
					go(P.HOSPITAL, "Kids are getting strange powers and getting sick! The sick ones were taken to Amity Medical"),
					kill(P.HOSPITAL, 6, "Ghost bugs are spreading it! Squash them"),
					talk(P.HOSPITAL, "Tucker", TUCKER, "Find Tucker in the quarantine",
							"Tucker: \"I'm fine! I'm totally fi- ACHOO! ...My sneeze just phased through the wall.\""),
					boss(P.HOSPITAL, GHOST_ID, "Ghost Bug Queen", 240, "The ghost bug queen is behind it! Squash her"),
					trap("Trap the queen in the Thermos so everyone gets better")),
			ep("Pirate Radio", "Season 2, Episode 3",
					"All the grown-ups in Amity Park have vanished. Except you can hear music...",
					talk(P.LAB_DOOR, "Jazz", JAZZ, "All the adults in town have disappeared! Talk to Jazz",
							"Jazz: \"Mom and Dad just walked out the door! It was that weird radio music.\""),
					go(P.LAKE, "Follow the strange pirate music to the lake"),
					kill(P.LAKE, 5, "Youngblood's skeleton crew! Beat them"),
					boss(P.LAKE, GHOST_ID, "Youngblood", 240, "Youngblood, the ghost pirate captain! Fight him"),
					boss(P.LAKE, ROCKER, "Ember McLain", 260, "Ember was playing the music that hypnotized the adults! Stop her"),
					trap("Trap Ember in the Thermos and bring the grown-ups home")),
			ep("Reign Storm, Part 1", "Season 2, Episode 4", "Something huge is waking up in the Ghost Zone.", REIGN_1),
			ep("Reign Storm, Part 2", "Season 2, Episode 5", "The Ghost King is pulling Amity Park into the Ghost Zone.", REIGN_2),
			ep("Identity Crisis", "Season 2, Episode 6",
					"Being two people at once is hard. What if you really were two people?",
					stay(P.LAB, 6, "Use the Fenton Ghost Catcher to split your human and ghost halves (stand in the lab)"),
					kill(P.SCHOOL, 4, "Your ghost half is out fighting ghosts at school! Help it"),
					boss(P.SCHOOL, GHOST_ID, "Your Ghost Half", 220, "Your ghost half has gone wild without you! Fight it"),
					stay(P.LAB, 6, "Merge back together using the Ghost Catcher in the lab")),
			ep("The Fenton Menace", "Season 2, Episode 7",
					"Jazz planned a ghost-free family vacation. Youngblood has other plans.",
					talk(P.LAB_DOOR, "Jazz", JAZZ, "Jazz planned a ghost-free family trip. Talk to her",
							"Jazz: \"No ghosts, no gadgets, just family fun. I made a schedule!\""),
					go(P.ENTRANCE, "Head out of town with the family"),
					kill(P.ENTRANCE, 4, "Youngblood followed you! His minions are attacking"),
					boss(P.ENTRANCE, GHOST_ID, "Youngblood", 260, "Youngblood is ruining the trip! Fight him"),
					trap("Trap Youngblood in the Thermos")),
			ep("The Ultimate Enemy, Part 1", "Season 2, Episode 8", "There's a big test tomorrow. And a lot riding on it.", ENEMY_1),
			ep("The Ultimate Enemy, Part 2", "Season 2, Episode 9", "Your future self is coming to make sure he exists.", ENEMY_2),
			ep("The Fright Before Christmas", "Season 2, Episode 10", "It's Christmas, and you're in a terrible mood.", XMAS),
			ep("Secret Weapons", "Season 2, Episode 11",
					"You and Jazz had a fight. A bad one.",
					talk(P.LAB_DOOR, "Jazz", JAZZ, "You and Jazz had a fight. Find her at home",
							"Jazz: \"Fine. I'll go somewhere I'm appreciated.\""),
					go(P.VLADCO, "Jazz went to Vlad! Go and get her back"),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 340, "Vlad wants to use Jazz against you! Fight him"),
					talk(P.VLADCO, "Jazz", JAZZ, "Make up with Jazz",
							"Jazz: \"I'm sorry. Let's go home.\"")),
			ep("Flirting with Disaster", "Season 2, Episode 12",
					"Valerie asked you out. On a date. Like, a real one.",
					talk(P.SCHOOL, "Valerie", VALERIE, "Valerie asked you out! Talk to her outside the school",
							"Valerie: \"Movies, Friday? It's a date.\""),
					go(P.CINEMA, "Take Valerie to the movies"),
					kill(P.CINEMA, 4, "Technus's machines crashed your date! Destroy them"),
					boss(P.CINEMA, GHOST_ID, "Technus", 260, "Technus has been setting you two up so you'd be distracted! Fight him"),
					trap("Trap Technus in the Thermos")),
			ep("Micro Management", "Season 2, Episode 13",
					"Dad's newest invention has a tiny problem.",
					stay(P.LAB, 6, "Dad's new Ghost Shrinker went off in the lab! Hold still..."),
					kill(P.LAB, 5, "Everything's huge now! Fight off the critters"),
					boss(P.LAB_DOOR, HUNTER_GHOST, "Skulker", 260, "Skulker got shrunk too, and he's hunting you! Fight him"),
					trap("Trap Skulker in the Thermos"),
					stay(P.LAB, 6, "Use the Shrinker again to get back to normal size")),
			ep("Beauty Marked", "Season 2, Episode 14",
					"A ghostly beauty pageant is calling every girl in town. Sam won.",
					talk(P.SCHOOL, "Sam", SAM, "Sam was picked for a ghost beauty pageant! Talk to her",
							"Sam: \"I didn't even enter! And now there's a dragon prince who wants to marry me.\""),
					zone("The pageant is in the Ghost Zone. Go through the portal"),
					boss(P.ZONE, DRAGON, "Prince Aragon", 420, "Prince Aragon turned into a dragon! Fight him"),
					talk(P.ZONE, "Princess Dorathea", PRINCESS, "Thank the princess for her help",
							"Princess Dorathea: \"My brother is a brute. Take your friend home, brave knight.\""),
					go(P.LAB, "Bring Sam home")),
			ep("King Tuck", "Season 2, Episode 15",
					"An Egyptian exhibit just opened at the museum. Tucker loves hats.",
					go(P.MUSEUM, "An Egyptian exhibit opened at the museum. Go see it with Tucker"),
					talk(P.MUSEUM, "Tucker", TUCKER, "Talk to Tucker",
							"Tucker: \"Check out this crown! ...Wait, why is it glowing?\""),
					kill(P.MUSEUM, 5, "Tucker became a ghostly pharaoh and summoned mummies! Fight them"),
					boss(P.MUSEUM, GHOST_ID, "Hotep-Ra", 260, "Hotep-Ra, the pharaoh's ghost, is behind it! Fight him"),
					trap("Trap Hotep-Ra in the Thermos")),
			ep("Masters of All Time", "Season 2, Episode 16",
					"What if Vlad's accident had never happened?",
					stay(P.LAB, 6, "Use the time medallion to go back to before Vlad's accident (stand in the lab)"),
					talk(P.LAB, "Young Vlad", VLAD, "Talk to the young Vlad Masters",
							"Young Vlad: \"Jack's portal is perfectly safe. What could possibly go wrong?\""),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 380, "You changed the past... now Vlad runs everything! Fight him"),
					go(P.LAB, "Put time right")),
			ep("Kindred Spirits", "Season 2, Episode 17",
					"A girl with powers just like yours is in the park.",
					talk(P.FOUNTAIN, "Danielle", JAZZ, "A girl with powers just like yours is in the park! Talk to her",
							"Danielle: \"Call me Dani. With an i.\""),
					go(P.VLADCO, "Dani led you to Vlad's lab! Go"),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 380, "Vlad wants your DNA to make a perfect clone! Fight him"),
					talk(P.VLADCO, "Dani", JAZZ, "Talk to Dani",
							"Dani: \"He lied to me. I'm not going to be his puppet anymore.\"")),
			ep("Double Cross My Heart", "Season 2, Episode 18",
					"There's a new exchange student, and Sam likes him.",
					talk(P.SCHOOL, "Sam", SAM, "Sam has a crush on the new exchange student, Gregor. Talk to her",
							"Sam: \"He's mysterious. And he likes the same things I do!\""),
					kill(P.MALL, 4, AGENT, "Guys in White Agent", "Gregor is a Guys in White agent and they're on to you! Fight them at the mall"),
					talk(P.MALL, "Sam", SAM, "Make sure Sam's okay",
							"Sam: \"Gregor was a fake. ...Thanks for having my back.\"")),
			ep("Reality Trip, Part 1", "Season 2, Episode 19", "Freakshow is back, and he's taken your parents.", REALITY_1),
			ep("Reality Trip, Part 2", "Season 2, Episode 20", "Freakshow has almost every Reality Gem.", REALITY_2),
			// ---------------- season 3
			ep("Eye for an Eye", "Season 3, Episode 1",
					"Vlad is running for mayor. And somehow winning.",
					go(P.TOWN_HALL, "Vlad is running for mayor... and winning? Go to Town Hall"),
					talk(P.TOWN_HALL, "Mayor Vlad", VLAD, "Talk to Mayor Vlad",
							"Vlad: \"New rules, Daniel. Ghosts are banned. Which makes things... complicated for you.\""),
					kill(P.TOWN_HALL, 5, "Vlad's ghosts are terrorizing his 'anti-ghost' rally! Fight them"),
					stay(P.SCHOOL, 8, "Lay low at school and follow Vlad's new rules for now")),
			ep("Infinite Realms", "Season 3, Episode 2",
					"Frostbite has something important: the Infi-map.",
					zone("Frostbite needs you in the Ghost Zone. Go through the portal"),
					talk(P.ZONE, "Frostbite", OTHER, "Talk to Frostbite",
							"Frostbite: \"Great One! Vlad Plasmius stole the Infi-map. It leads to any place, any time.\""),
					boss(P.ZONE, GHOST_ID, "Vlad Plasmius", 400, "Chase Vlad down and fight him for the map!"),
					item(P.ZONE, "minecraft:map", "Infi-map", "Grab the Infi-map"),
					go(P.LAB, "Bring the map home safely")),
			ep("Girls' Night Out", "Season 3, Episode 3",
					"Every boy in Amity Park is disappearing.",
					talk(P.SCHOOL, "Sam", SAM, "All the boys in town are vanishing! Talk to Sam",
							"Sam: \"Spectra, Kitty and Ember teamed up! Valerie and I are on it. You help!\""),
					boss(P.MALL, KITTY, "Kitty", 240, "Kitty's at the mall! Fight her"),
					boss(P.MALL, ROCKER, "Ember McLain", 260, "Ember's here too! Fight her"),
					boss(P.MALL, SPECTRA, "Penelope Spectra", 300, "Spectra is behind it all! Fight her"),
					trap("Trap Spectra in the Thermos and bring everyone back")),
			ep("Torrent of Terror", "Season 3, Episode 4",
					"The weather over Amity Park has gone wild.",
					go(P.TOWER, "Crazy storms over Amity Park! Climb up to the Fenton Works tower to see"),
					boss(P.FOUNTAIN, ROCKER, "Vortex", 360, "Vortex, the weather ghost, is wrecking the town! Fight him"),
					trap("Trap Vortex in the Thermos")),
			ep("Forever Phantom", "Season 3, Episode 5",
					"Somebody's going around being Danny Phantom. And it isn't you.",
					talk(P.FOUNTAIN, "\"Danny Phantom\"", OTHER, "Someone's pretending to be you in the park! Talk to them",
							"\"Danny Phantom\": \"Who, me? I'm the real deal.\"",
							"His face melts for a second... it's Amorpho, the ghost of a thousand faces!"),
					boss(P.FOUNTAIN, GHOST_ID, "Amorpho", 260, "Fight Amorpho!"),
					trap("Trap Amorpho in the Thermos")),
			ep("Urban Jungle", "Season 3, Episode 6",
					"Giant plants are taking over Amity Park.",
					go(P.GARDENS, "Giant plants are growing all over town! Check the Community Gardens"),
					kill(P.GARDENS, 6, GHOUL, "Plant Monster", "Plant monsters! Fight them"),
					boss(P.GARDENS, GHOUL, "Undergrowth", 500, "Undergrowth, the plant ghost, has Sam under his control! Fight him"),
					talk(P.GARDENS, "Sam", SAM, "Check on Sam",
							"Sam: \"He wanted me to be his daughter. ...Thanks for getting me out of there.\"")),
			ep("Livin' Large", "Season 3, Episode 7",
					"Your parents sold Fenton Works. To the Guys in White.",
					talk(P.LAB_DOOR, "Dad", DAD, "Your dad sold Fenton Works! Talk to him",
							"Dad: \"The Guys in White paid a fortune! We're moving into a mansion!\""),
					kill(P.LAB_DOOR, 5, AGENT, "Guys in White Agent", "The Guys in White are planning a missile strike on the Ghost Zone! Stop the agents"),
					stay(P.CONSOLES, 8, "Shut down the missile from the lab consoles"),
					talk(P.LAB_DOOR, "Dad", DAD, "Talk to your dad",
							"Dad: \"I'm buying it back. Home sweet ghost-infested home!\"")),
			ep("Boxed-Up Fury", "Season 3, Episode 8",
					"The Box Ghost found something he really shouldn't have.",
					go(P.STORE, "The Box Ghost is causing trouble at the Quick Stop again"),
					zone("He stole Pandora's Box! Find Pandora in the Ghost Zone"),
					talk(P.ZONE, "Pandora", JAZZ, "Talk to Pandora",
							"Pandora: \"That little thief! Bring my box back and I'll make him regret it.\""),
					boss(P.STORE, BOX, "The Box Ghost", 320, "The Box Ghost is actually scary with Pandora's Box! Fight him"),
					trap("Trap the Box Ghost in the Thermos")),
			ep("Frightmare", "Season 3, Episode 9",
					"Everyone in town is falling asleep, and they won't wake up.",
					stay(P.LAB_DOOR, 6, "Everyone in town is falling asleep! Stay awake at home"),
					kill(P.FOUNTAIN, 5, "Nocturn's sleepwalkers are out in the park! Fight them"),
					boss(P.FOUNTAIN, SHADE, "Nocturn", 360, "Nocturn, the ghost of sleep, is stealing everyone's dreams! Fight him"),
					trap("Trap Nocturn in the Thermos and wake the town")),
			ep("Claw of the Wild", "Season 3, Episode 10",
					"Class camping trip! What could go wrong in the woods?",
					go(P.MEADOW, "Class camping trip! Head to the park meadow"),
					talk(P.MEADOW, "Wulf", OTHER, "A werewolf ghost is hiding in the woods. Talk to Wulf",
							"Wulf: \"Amiko! Walker hunts me. Please... help.\""),
					kill(P.MEADOW, 5, "Walker's guards are hunting Wulf! Fight them"),
					boss(P.MEADOW, JAILER, "Walker", 360, "Walker is here! Fight him"),
					trap("Trap Walker in the Thermos")),
			ep("D-Stabilized", "Season 3, Episode 11",
					"Dani's back, and she's in trouble.",
					talk(P.FOUNTAIN, "Dani", JAZZ, "Dani's back in the park, and she's melting! Talk to her",
							"Dani: \"I'm falling apart. Literally. Vlad says he can fix me.\""),
					boss(P.VLADCO, GHOST_ID, "Vlad Plasmius", 420, "Vlad wants to melt Dani down for his experiments! Fight him"),
					item(P.VLADCO, "minecraft:potion", "Ecto-Dejecto", "Grab the Ecto-Dejecto: it can stabilize Dani"),
					talk(P.FOUNTAIN, "Dani", JAZZ, "Give Dani the Ecto-Dejecto",
							"Dani: \"I'm stable! Thanks, cousin. See you around!\"")),
			ep("Phantom Planet, Part 1", "Season 3, Episode 12", "Something enormous is heading for Earth.", PLANET_1),
			ep("Phantom Planet, Part 2", "Season 3, Episode 13", "It's time to save the whole world.", PLANET_2));

	/** The TV movies and specials, playable any time from the Movies and Specials tab. */
	public static final List<Ep> SPECIALS = List.of(
			ep("Reign Storm", "TV movie", "Vlad woke the Ghost King, and his army is coming for Amity Park.", join(REIGN_1, REIGN_2)),
			ep("The Ultimate Enemy", "TV movie", "A test, a cheat sheet, and a glimpse of the worst possible future.", join(ENEMY_1, ENEMY_2)),
			ep("The Fright Before Christmas", "Holiday special", "It's Christmas, and you're in a terrible mood.", XMAS),
			ep("Reality Trip", "TV movie", "Freakshow wants the Reality Gems, and he has your parents.", join(REALITY_1, REALITY_2)),
			ep("Phantom Planet", "TV movie", "An asteroid is heading for Earth, and only the ghosts can help.", join(PLANET_1, PLANET_2)));

	/** Missions 1-4 are the original story; this list carries on from mission 5. */
	public static final int FIRST = 5, SPECIAL_BASE = 200;

	public static Ep get(int mission) {
		if (mission >= SPECIAL_BASE && mission < SPECIAL_BASE + SPECIALS.size()) return SPECIALS.get(mission - SPECIAL_BASE);
		int i = mission - FIRST;
		return i >= 0 && i < MAIN.size() ? MAIN.get(i) : null;
	}

	public static boolean special(int mission) {
		return mission >= SPECIAL_BASE && mission < SPECIAL_BASE + SPECIALS.size();
	}

	public static S step(int mission, int step) {
		Ep e = get(mission);
		return e == null || step < 0 || step >= e.steps().length ? null : e.steps()[step];
	}

	public static final int LAST = FIRST + MAIN.size() - 1;
}
