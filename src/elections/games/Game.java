package elections.games;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Game {
	ASTRONEER("Astroneer", 361420, 4),
	BATTLEBLOCK_THEATER("BattleBlock Theater", 238460, 4),
	BOOMERANG_FU("Boomerang Fu", 965680, 6),
	CHESS("Chess (classic)", 4), // 2, 4, infinite max players?
	CIVILIZATION_6("Sid Meier's Civilization VI", 289070, 0),
	CIVILIZATION_7("Sid Meier's Civilization VII", 1295660, 0),
	COSMOTEER("Cosmoteer", 799600, 8),
	DONT_STARVE_TOGETHER("Don't Starve Together", 322330, 6),
	ECO("Eco", 382310, 0),
	ENDLESS_DUNGEON("Endless Dungeon", 1485590, 3),
	FACTORIO("Factorio", 427520, 0),
	FIVE_D_CHESS("5D Chess", 1349230, -1),
	FOXHOLE("Foxhole", 505460, 0),
	HALO("Halo", 976730, -1),
	HELLDIVERS_2("HELLDIVERS 2", 553850, 4),
	HEROES_OF_THE_STORM("Heroes of the Storm", 10),
	HOMEWORLD("Homeworld", 244160, 8),
	HUMANKIND("HUMANKIND", 1124300, 8),
	IT_TAKES_TWO("It Takes Two", 1426210, 2),
	KEEP_TALKING_AND_NOBODY_EXPLODES("Keep Talking and Nobody Explodes", 341800, 4),
	KINGDOM_TWO_CROWNS("Kingdom Two Crowns", 701160, 2),
	LEGION_TD_2("Legion TD 2", 469600, 4),
	LETHAL_COMPANY("Lethal Company", 1966720, 4),
	LOVERS_IN_A_DANGEROUS_SPACETIME("Lovers in a Dangerous Spacetime", 252110, 4),
	MINECRAFT("Minecraft", 0),
	MOVING_OUT("Moving Out", 996770, 4),
	NEBULOUS_FLEET_COMMAND("NEBULOUS: Fleet Command", 887570, -1),
	NEVER_SPLIT_THE_PARTY("Never Split the Party", 711810, 4),
	NO_MANS_SKY("No Man's Sky", 275850, 32),
	OVERCOOKED_2("Overcooked! 2", 728880, 4),
	PHASMOPHOBIA("Phasmophobia", 739630, 4),
	PLANETARY_ANNIHILATION_TITANS("Planetary Annihilation: TITANS", 386070, 10),
	POKER("Poker", -1),
	PORTAL_2("Portal 2", 620, 2),
	PUBG("PUBG: Battlegrounds", 578080, 4),
	RAFT("Raft", 648800, 8),
	RISK_OF_RAIN_2("Risk of Rain 2", 632360, 4),
	SATISFACTORY("Satisfactory", 526870, 4),
	SEVEN_DAYS_TO_DIE("7 Days to Die", 251570, 8),
	SLAY_THE_SPIRE_2("Slay the Spire 2", 2868840, 4),
	SPACE_ENGINEERS("Space Engineers", 244850, 16),
	STARCRAFT("StarCraft", 8),
	STELLARIS("Stellaris", 281990, 0),
	TABLETOP_SIMULATOR("Tabletop Simulator", 286160, 10),
	TERRARIA("Terraria", 105600, 16),
	TWELVE_ORBITS("12 orbits", 529950, 12),
	ULTIMATE_CHICKEN_HORSE("Ultimate Chicken Horse", 386940, 4),
	UNRAILED("Unrailed!", 1016920, 4),
	VAINGLORY("Vainglory", 10),
	VALHEIM("Valheim", 892970, 10),
	VALORANT("Valorant", 5),
	WORLD_OF_WARSHIPS("World of Warships", 552990, -1),
	WORMS_WMD("Worms W.M.D.", 327030, 6),
	;

	public final String title;
	public final int steamId;
	public final int maxPlayers; // 0="unlimited"; -1="unknown"

	Game(String title, int maxPlayers) {
		this(title, -1, maxPlayers);
	}

	Game(String title, int steamId, int maxPlayers) {
		this.title = title;
		this.steamId = steamId;
		this.maxPlayers = maxPlayers;
	}

	public String getTitle() { return title; }

	public int getMaxPlayers() { return maxPlayers; }

	@NotNull
	public static Optional<Game> interpret(String input) {
		return Arrays.stream(values()).filter(game -> game.title.toLowerCase().contains(input.toLowerCase())).findAny();
	}

	public static List<Game> fullList() {
		return Arrays.asList(values());
	}

	public static List<Game> shortList() {
		List<Game> shortList = new ArrayList<>();
		shortList.add(ASTRONEER);
		shortList.add(BOOMERANG_FU);
		shortList.add(CHESS);
		shortList.add(CIVILIZATION_7);
		shortList.add(ECO);
		shortList.add(FACTORIO);
		shortList.add(FOXHOLE);
		shortList.add(HALO);
		shortList.add(HELLDIVERS_2);
		shortList.add(HEROES_OF_THE_STORM);
		shortList.add(LETHAL_COMPANY);
		shortList.add(LOVERS_IN_A_DANGEROUS_SPACETIME);
		shortList.add(MINECRAFT);
		shortList.add(NEBULOUS_FLEET_COMMAND);
		shortList.add(NEVER_SPLIT_THE_PARTY);
		shortList.add(POKER);
		shortList.add(RISK_OF_RAIN_2);
		shortList.add(SEVEN_DAYS_TO_DIE);
		shortList.add(SLAY_THE_SPIRE_2);
		shortList.add(STARCRAFT);
		shortList.add(STELLARIS);
		shortList.add(TWELVE_ORBITS);
		shortList.add(UNRAILED);
		shortList.add(VAINGLORY);
		shortList.add(VALHEIM);
		shortList.add(VALORANT);
		return shortList;
	}
}
