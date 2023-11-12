package elections.games;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Game {
	ASTRONEER("Astroneer", 4),
	BATTLEBLOCK_THEATER("BattleBlock Theater", 4),
	BOOMERANG_FU("Boomerang Fu", 6),
	CHESS("Chess (classic)", 4), // 2, 4, infinite max players?
	CIVILIZATION_6("Sid Meier's Civilization VI", 0),
	COSMOTEER("Cosmoteer", 8),
	DONT_STARVE_TOGETHER("Don't Starve Together", 6),
	ECO("Eco", 0),
	FACTORIO("Factorio", 0),
	FIVE_D_CHESS("5D Chess", -1),
	FOXHOLE("Foxhole", 0),
	HALO("Halo", -1),
	HEROES_OF_THE_STORM("Heroes of the Storm", 10),
	HOMEWORLD("Homeworld", 8),
	HUMANKIND("HUMANKIND", 8),
	IT_TAKES_TWO("It Takes Two", 2),
	KEEP_TALKING_AND_NOBODY_EXPLODES("Keep Talking and Nobody Explodes", 4),
	KINGDOM_TWO_CROWNS("Kingdom Two Crowns", 2),
	LEGION_TD_2("Legion TD 2", 4),
	LOVERS_IN_A_DANGEROUS_SPACETIME("Lovers in a Dangerous Spacetime", 4),
	MOVING_OUT("Moving Out", 4),
	NEVER_SPLIT_THE_PARTY("Never Split the Party", 4),
	NO_MANS_SKY("No Man's Sky", 32),
	OVERCOOKED_2("Overcooked! 2", 4),
	PLANETARY_ANNIHILATION_TITANS("Planetary Annihilation: TITANS", 10),
	PORTAL_2("Portal 2", 2),
	PUBG("PUBG: Battlegrounds", 4),
	RAFT("Raft", 8),
	RISK_OF_RAIN_2("Risk of Rain 2", 4),
	SATISFACTORY("Satisfactory", 4),
	SEVEN_DAYS_TO_DIE("7 Days to Die", 8),
	SPACE_ENGINEERS("Space Engineers", 16),
	STARCRAFT("StarCraft", 8),
	STELLARIS("Stellaris", 0),
	TABLETOP_SIMULATOR("Tabletop Simulator", 10),
	TERRARIA("Terraria", 16),
	TWELVE_ORBITS("12 orbits", 12),
	ULTIMATE_CHICKEN_HORSE("Ultimate Chicken Horse", 4),
	UNRAILED("Unrailed!", 4),
	VAINGLORY("Vainglory", 10),
	VALHEIM("Valheim", 10),
	WORMS_WMD("Worms W.M.D.", 6),
	;

	public final String title;
	public final int maxPlayers; // 0="unlimited"; -1="unknown"

	Game(String title, int maxPlayers) {
		this.title = title;
		this.maxPlayers = maxPlayers;
	}

	public String getTitle() { return title; }

	public int getMaxPlayers() { return maxPlayers; }

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
		shortList.add(CIVILIZATION_6);
		shortList.add(ECO);
		shortList.add(FACTORIO);
		shortList.add(FOXHOLE);
		shortList.add(HEROES_OF_THE_STORM);
		shortList.add(KEEP_TALKING_AND_NOBODY_EXPLODES);
		shortList.add(LOVERS_IN_A_DANGEROUS_SPACETIME);
		shortList.add(MOVING_OUT);
		shortList.add(NO_MANS_SKY);
		shortList.add(OVERCOOKED_2);
		shortList.add(PLANETARY_ANNIHILATION_TITANS);
		shortList.add(RISK_OF_RAIN_2);
		shortList.add(SEVEN_DAYS_TO_DIE);
		shortList.add(STELLARIS);
		shortList.add(STARCRAFT);
		shortList.add(TERRARIA);
		shortList.add(TWELVE_ORBITS);
		shortList.add(ULTIMATE_CHICKEN_HORSE);
		shortList.add(UNRAILED);
		shortList.add(VAINGLORY);
		shortList.add(WORMS_WMD);
		return shortList;
	}
}
