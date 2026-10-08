package com.fs.starfarer.api.impl.campaign.missions.cb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import data.scripts.EISContactFaction;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch.StarSystemRequirement;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

/**
 * Iron Shell bounty board, shared by every contact that offers eis_mcb.
 * Each contact (identified by their name tag) has their own set of bounty types and their own difficulty range for the
 * low / mid / high offers. Anyone without a profile (e.g. Kim) falls back to the full list and the vanilla difficulty logic.
 */
public class EISMilitaryCustomBounty extends BaseCustomBounty {

	private static final CustomBountyCreator PIRATE = new CBPirate();
	private static final CustomBountyCreator PATHER = new CBPather();
	private static final CustomBountyCreator DESERTER = new EIS_CBDeserter(); // builds deserters from Iron Shell even for recruited contacts
	private static final CustomBountyCreator DERELICT = new CBDerelict();
	private static final CustomBountyCreator MERC = new CBMerc();
	private static final CustomBountyCreator ENEMY_STATION = new EIS_CBEnemyStation(); // hostility is checked against Iron Shell even for recruited contacts
	private static final CustomBountyCreator REMNANT = new CBRemnant();
	private static final CustomBountyCreator REMNANT_STATION = new EIS_CBRemnantStation(); // CBRemnantStation is hardcoded to luddic church, path, and hegemony
	private static final CustomBountyCreator REMNANT_PLUS = new CBRemnantPlus();

	// default for anyone without a profile below
	public static List<CustomBountyCreator> CREATORS = new ArrayList<CustomBountyCreator>();
	static {
		CREATORS.add(PIRATE);
		CREATORS.add(DESERTER);
		CREATORS.add(DERELICT);
		CREATORS.add(MERC);
		CREATORS.add(PATHER);
		CREATORS.add(REMNANT);
		CREATORS.add(REMNANT_PLUS);
		CREATORS.add(REMNANT_STATION);
		CREATORS.add(ENEMY_STATION);
	}

	// Per-contact profiles, keyed by the contact's name tag.
	// Every list needs a type that covers the lowest difficulty of the contact's low tier (only pirate and pather go down to 0).
	private static final String[] CONTACT_TAGS = {"eis_ava", "eis_charlotte", "eis_hartley", "eis_celeste"};
	private static final Map<String, List<CustomBountyCreator>> CONTACT_CREATORS = new HashMap<String, List<CustomBountyCreator>>();
	// {min, max} difficulty for the LOW, NORMAL and HIGH offers (DifficultyChoice order)
	private static final Map<String, int[][]> CONTACT_RANGES = new HashMap<String, int[][]>();
	// Exact {LOW, NORMAL, HIGH} offers, indexed by base difficulty (the average of the last completed bounties, before the per-offer adjustment).
	// V keeps the vanilla value for that offer, and so does any base difficulty past the end of a table. Every result is clamped to the range above.
	private static final Map<String, int[][]> CONTACT_TABLES = new HashMap<String, int[][]>();
	private static final int V = -1;
	private static final int[][] EARLY_OFFERS = {{0, 1, 4}, {1, 2, 5}, {1, 3, 5}, {1, 4, 6}};
	private static final int[][] CELESTE_OFFERS = {
		{V, V, V}, {V, V, V}, {V, V, V}, {V, V, V}, {V, V, V},
		{V, 7, V}, {V, 7, V}, {V, 8, V}, {V, 9, V}, {V, 9, V}, {V, 9, V}};
	private static final int[][] HARTLEY_OFFERS = {
		{V, V, V}, {V, V, V}, {V, V, V}, {V, V, V}, {V, V, V}, {V, V, V},
		{V, 7, V}, {V, 8, V}, {V, 9, V}, {V, 9, V}, {V, 9, V}};
	static {
		CONTACT_CREATORS.put("eis_ava", Arrays.asList(PIRATE, PATHER, DESERTER, MERC, ENEMY_STATION));
		CONTACT_RANGES.put("eis_ava", new int[][]{{0, 3}, {1, 5}, {4, 7}});

		CONTACT_CREATORS.put("eis_charlotte", Arrays.asList(PIRATE, PATHER, DERELICT, MERC, ENEMY_STATION));
		CONTACT_RANGES.put("eis_charlotte", new int[][]{{0, 3}, {1, 5}, {4, 7}});
		CONTACT_TABLES.put("eis_ava", EARLY_OFFERS);
		CONTACT_TABLES.put("eis_charlotte", EARLY_OFFERS);

		CONTACT_CREATORS.put("eis_hartley", Arrays.asList(PIRATE, DESERTER, ENEMY_STATION, REMNANT, REMNANT_STATION, REMNANT_PLUS));
		CONTACT_RANGES.put("eis_hartley", new int[][]{{2, 4}, {5, 9}, {8, 10}});
		CONTACT_TABLES.put("eis_hartley", HARTLEY_OFFERS);

		CONTACT_CREATORS.put("eis_celeste", Arrays.asList(DERELICT, REMNANT, REMNANT_STATION, REMNANT_PLUS));
		CONTACT_RANGES.put("eis_celeste", new int[][]{{5, 5}, {6, 9}, {8, 10}});
		CONTACT_TABLES.put("eis_celeste", CELESTE_OFFERS);
	}

	private String getContactKey() {
		PersonAPI person = getPerson();
		if (person == null) return null;
		for (String tag : CONTACT_TAGS) {
			if (person.hasTag(tag)) return tag;
		}
		return null;
	}

	// Bounty targets (systems and stations) must be within this many light-years of the contact's market.
	public static final float MAX_TARGET_DISTANCE_LY = 25f;
	// Failsafe only: if no valid system exists within range, widen the search in steps rather than hand the creator a null system.
	private static final float WIDEN_STEP_LY = 10f;
	private static final float MAX_WIDEN_LY = 300f;

	// Every creator that picks a system goes through here (the station creators filter their own lists, see EIS_CBEnemyStation / EIS_CBRemnantStation).
	@Override
	public StarSystemAPI pickSystem(boolean resetSearch) {
		PersonAPI person = getPerson();
		MarketAPI origin = person == null ? null : person.getMarket();
		if (origin == null) return super.pickSystem(resetSearch);
		final Vector2f from = origin.getLocationInHyperspace();
		final float[] range = {MAX_TARGET_DISTANCE_LY};
		getSearch().systemReqs.add(new StarSystemRequirement() {
			public boolean systemMatchesRequirement(StarSystemAPI system) {
				return Misc.getDistanceLY(from, system.getLocation()) <= range[0];
			}
		});
		// resetSearch=false keeps the creator's own requirements in place for the widening retries
		StarSystemAPI system = super.pickSystem(false);
		while (system == null && range[0] < MAX_WIDEN_LY) {
			range[0] += WIDEN_STEP_LY;
			system = super.pickSystem(false);
		}
		if (resetSearch) resetSearch();
		return system;
	}

	@Override
	public List<CustomBountyCreator> getCreators() {
		String key = getContactKey();
		if (key != null) return CONTACT_CREATORS.get(key);
		return CREATORS;
	}

	// The vanilla history-based value is kept, then clamped to the contact's range for that offer.
	// Contacts with a table use its exact value for that offer and base difficulty, unless it is V.
	@Override
	protected int pickDifficulty(DifficultyChoice choice) {
		int difficulty = super.pickDifficulty(choice);
		String key = getContactKey();
		if (key == null) return difficulty;
		int[][] table = CONTACT_TABLES.get(key);
		if (table != null && difficultyOverride == null) {
			int base = getBaseDifficulty();
			if (base < table.length && table[base][choice.ordinal()] != V) difficulty = table[base][choice.ordinal()];
		}
		int[] range = CONTACT_RANGES.get(key)[choice.ordinal()];
		return Math.max(range[0], Math.min(range[1], difficulty));
	}

	// The same average of the last completed bounties that the vanilla pickDifficulty starts from.
	private int getBaseDifficulty() {
		List<Integer> completed = getAggregateData().completedDifficulty;
		if (completed.isEmpty()) return 0;
		float total = 0f;
		for (Integer diff : completed) total += diff;
		return (int) (total / Math.max(completed.size(), NUM_TO_TRACK_FOR_DIFFICULTY));
	}

	// Vanilla randomly skips types based on contact quality (more often for higher-minimum types). A contact whose list has no
	// minimum-0 type could have every eligible type skipped, which would drop the whole board, so fall back to ignoring the skip roll.
	@Override
	protected CustomBountyCreator pickCreator(int difficulty, DifficultyChoice choice) {
		CustomBountyCreator picked = super.pickCreator(difficulty, choice);
		if (picked != null) return picked;
		WeightedRandomPicker<CustomBountyCreator> picker = new WeightedRandomPicker<CustomBountyCreator>(genRandom);
		for (CustomBountyCreator curr : getCreators()) {
			if (curr.getMinDifficulty() > difficulty) continue;
			if (curr.getMaxDifficulty() < difficulty) continue;
			float frequency = curr.getFrequency(this, difficulty);
			if (frequency > 0f) picker.add(curr, frequency);
		}
		return picker.pick();
	}

	// Faction rep goes to Iron Shell even after a recruited contact has switched to the player faction.
	@Override
	protected void adjustRep(TextPanelAPI textPanel, HubMissionResult result, RepActions action) {
		String previousFaction = EISContactFaction.enter(getPerson());
		try {
			super.adjustRep(textPanel, result, action);
		} finally {
			EISContactFaction.exit(getPerson(), previousFaction);
		}
	}

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (Factions.PIRATES.equals(createdAt.getFaction().getId())) {
			return false;
		}
		if (!super.create(createdAt, barEvent)) return false;
		// accept() copies these onto the mission
		applyRepByDifficulty(dataLow);
		applyRepByDifficulty(dataNormal);
		applyRepByDifficulty(dataHigh);
		return true;
	}

	// Reputation on completion by final difficulty (0 to 10), person / faction: +6/+4 at difficulty 0 rising to +16/+10 at difficulty 10.
	private static final float[] REP_PERSON  = {0.06f, 0.07f, 0.08f, 0.09f, 0.10f, 0.11f, 0.12f, 0.13f, 0.14f, 0.15f, 0.16f};
	private static final float[] REP_FACTION = {0.04f, 0.04f, 0.05f, 0.05f, 0.06f, 0.07f, 0.07f, 0.08f, 0.09f, 0.09f, 0.10f};

	private static void applyRepByDifficulty(CustomBountyData data) {
		if (data == null) return;
		int index = Math.max(0, Math.min(REP_PERSON.length - 1, data.difficulty));
		data.repPerson = REP_PERSON[index];
		data.repFaction = REP_FACTION[index];
	}

	@Override
	protected void createBarGiver(MarketAPI createdAt) {
		List<String> posts = new ArrayList<String>();
		if (Misc.isMilitary(createdAt)) {
			posts.add(Ranks.POST_BASE_COMMANDER);
		}
		if (Misc.hasOrbitalStation(createdAt)) {
			posts.add(Ranks.POST_STATION_COMMANDER);
		}
		if (posts.isEmpty()) {
			posts.add(Ranks.POST_GENERIC_MILITARY);
		}
		String post = pickOne(posts);
		setGiverPost(post);
		if (post.equals(Ranks.POST_GENERIC_MILITARY)) {
			setGiverRank(Ranks.SPACE_COMMANDER);
			setGiverImportance(pickImportance());
		} else if (post.equals(Ranks.POST_BASE_COMMANDER)) {
			setGiverRank(Ranks.GROUND_COLONEL);
			setGiverImportance(pickImportance());
		} else if (post.equals(Ranks.POST_STATION_COMMANDER)) {
			setGiverRank(Ranks.SPACE_CAPTAIN);
			setGiverImportance(pickHighImportance());
		}
		setGiverTags(Tags.CONTACT_MILITARY);
		findOrCreateGiver(createdAt, false, false);
		setGiverIsPotentialContactOnSuccess();
	}

}
