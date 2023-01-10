package elections.games;

import java.util.ArrayList;
import java.util.List;

public enum Game {
	ASTRONEER("Astroneer"),
	BATTLEBLOCK_THEATER("BattleBlock Theater"),
	BOOMERANG_FU("Boomerang Fu"),
	CIVILIZATION_6("Sid Meier's Civilization IV"),
	COSMOTEER("Cosmoteer"),
	DONT_STARVE_TOGETHER("Don't Starve Together"),
	FACTORIO("Factorio"),
	FIVE_D_CHESS("5D Chess"),
	FOXHOLE("Foxhole"),
	HALO("Halo"),
	HOMEWORLD("Homeworld"),
	HUMANKIND("HUMANKING"),
	IT_TAKES_TWO("It Takes Two"),
	KEEP_TALKING_AND_NOBODY_EXPLODES("Keep Talking and Nobody Explodes"),
	KINGDOM_TWO_CROWNS("Kingdom Two Crowns"),
	LEGION_TD_2("Legion TD 2"),
	LOVERS_IN_A_DANGEROUS_SPACETIME("Lovers in a Dangerous Spacetime"),
	MOVING_OUT("Moving Out"),
	NEVER_SPLIT_THE_PARTY("Never Split the Party"),
	NO_MANS_SKY("No Man's Sky"),
	OVERCOOKED_2("Overcooked! 2"),
	PLANETARY_ANNIHILATION_TITANS("Planetary Annihilation: TITANS"),
	PORTAL_2("Portal 2"),
	PUBG("PUBG: Battlegrounds"),
	RAFT("Raft"),
	RISK_OF_RAIN_2("Risk of Rain 2"),
	SATISFACTORY("Satisfactory"),
	SEVEN_DAYS_TO_DIE("7 Days to Die"),
	SPACE_ENGINEERS("Space Engineers"),
	STELLARIS("Stellaris"),
	TABLETOP_SIMULATOR("Tabletop Simulator"),
	TERRARIA("Terraria"),
	TWELVE_ORBITS("12 orbits"),
	ULTIMATE_CHICKEN_HORSE("Ultimate Chicken Horse"),
	UNRAILED("Unrailed!"),
	VAINGLORY("Vainglory"),
	VALHEIM("Valheim"),
	WORMS_WMD("Worms W.M.D."),
	;

	public final String title;

	Game(String title) {
		this.title = title;
	}

	public String getTitle() { return title; }

	public static List<Game> shortList() {
		List<Game> shortList = new ArrayList<>();
		shortList.add(ASTRONEER);
		shortList.add(BOOMERANG_FU);
		shortList.add(FACTORIO);
		shortList.add(FOXHOLE);
		shortList.add(KEEP_TALKING_AND_NOBODY_EXPLODES);
		shortList.add(LOVERS_IN_A_DANGEROUS_SPACETIME);
		shortList.add(MOVING_OUT);
		shortList.add(NO_MANS_SKY);
		shortList.add(OVERCOOKED_2);
		shortList.add(PLANETARY_ANNIHILATION_TITANS);
		shortList.add(RISK_OF_RAIN_2);
		shortList.add(SEVEN_DAYS_TO_DIE);
		shortList.add(STELLARIS);
		shortList.add(TERRARIA);
		shortList.add(TWELVE_ORBITS);
		shortList.add(ULTIMATE_CHICKEN_HORSE);
		shortList.add(UNRAILED);
		shortList.add(VAINGLORY);
		shortList.add(WORMS_WMD);
		return shortList;
	}
}
