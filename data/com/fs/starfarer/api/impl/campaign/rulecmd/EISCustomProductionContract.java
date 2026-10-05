package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import data.scripts.EISContactFaction;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.CustomProductionContract;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission;
import static com.fs.starfarer.api.impl.campaign.missions.CustomProductionContract.DEALER_MAX_CAPACITY;
import static com.fs.starfarer.api.impl.campaign.missions.CustomProductionContract.DEALER_MIN_CAPACITY;
import static com.fs.starfarer.api.impl.campaign.missions.CustomProductionContract.DEALER_MULT;
import static com.fs.starfarer.api.impl.campaign.missions.CustomProductionContract.PROD_DAYS;
import static com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission.getRoundNumber;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.util.Misc;

//What do you mean you just borrowed this from Histidine? Well... you see... umm :flushed:

/*
 * NOTE: this mission is now only used by Hartley. It is started from his rules.csv option (HartleyCPCOfferSel, BeginMission eis_cpc),
 * and no other contact offers it: the eis_cpc row in person_missions.csv has frequency 0, so the contact hub never creates it.
 * A lot of what follows is therefore dead code (the eis_celeste branch in create(), the swp_conquest_xiv removal for non-Hartley contacts,
 * COST_MULT / COST_MULT_CELESTE). It is left in place because it isn't worth the effort to remove.
 * The live, Hartley-specific parts are COST_MULT_HARTLEY, the ship clearance discounts, and skipping the _bp tag filter in addMilitaryBlueprints().
 */
public class EISCustomProductionContract extends CustomProductionContract {
	
	public static final float COST_MULT = 1.3f;
        public static final float COST_MULT_CELESTE = 1.0f;
	public static final float COST_MULT_HARTLEY = 1.2f; // Hartley's starting cost, before the ship clearance discounts below
	// Hartley's contract costs this much less per ship clearance unlocked ($global.eisIllustriousUnlocked from Hartley, $global.eisDauntlessUnlocked from Ava), so 50% off with both.
	public static final float CLEARANCE_DISCOUNT = 0.25f;
	
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
		
		PersonAPI person = getPerson();
		if (person == null) return false;
		
		// An order offered through a rules.csv dialogue (BeginMission) that was never accepted leaves its ref on the person
		// (the contact hub normally aborts unaccepted offers). Drop it so it can't block new offers; accepted orders keep their ref until delivery.
		Object staleRef = person.getMemoryWithoutUpdate().get("$cpc_ref");
		if (staleRef instanceof BaseHubMission && ((BaseHubMission) staleRef).getCurrentStage() == null) {
			((BaseHubMission) staleRef).abort();
		}

		if (!setPersonMissionRef(person, "$cpc_ref")) {
			//Global.getLogger(this.getClass()).info("Mission ref already exists");
			return false;
		}
		
		market = getPerson().getMarket();
		if (market == null) {
			//Global.getLogger(this.getClass()).info("No market");
			return false;
		}
		if (Misc.getStorage(market) == null) {
			//Global.getLogger(this.getClass()).info("No storage on market");
			return false;
		}
		
		faction = EISContactFaction.get(person);
		
		if (true) { // don't care about ship production, since it's just acquisition from wherever
			PersonImportance imp = getPerson().getImportance();
			float mult = DEALER_MULT.get(imp); //Added number
			maxCapacity = getRoundNumber(mult * 0.75f *
						(DEALER_MIN_CAPACITY + (DEALER_MAX_CAPACITY - DEALER_MIN_CAPACITY) * getQuality()));
		}
				
			//1f - MILITARY_MAX_COST_DECREASE * getRewardMultFraction();
                if (person.getTags().contains("eis_celeste")) {
                    costMult = COST_MULT_CELESTE;
                } else {
                    costMult = "eisdarren".equals(person.getId()) ? COST_MULT_HARTLEY : COST_MULT;
                }
                if ("eisdarren".equals(person.getId())) {
                    MemoryAPI sectorMem = Global.getSector().getMemoryWithoutUpdate();
                    int clearances = 0;
                    if (sectorMem.getBoolean("$eisIllustriousUnlocked")) clearances++;
                    if (sectorMem.getBoolean("$eisDauntlessUnlocked")) clearances++;
                    costMult *= 1f - CLEARANCE_DISCOUNT * clearances;
                }
                addMilitaryBlueprints();
		if (ships.isEmpty() && weapons.isEmpty() && fighters.isEmpty()) return false;
                if (!person.getTags().contains("eis_celeste")) {
                    if (Global.getSettings().getModManager().isModEnabled("TORCHSHIPS")){
                        //ships.add("TADA_tinnitus_xiv");
                        //ships.add("TADA_gunwall_XIV");
                    }
                    if (Global.getSettings().getModManager().isModEnabled("tahlan")) {
                        //ships.add("tahlan_nelson_xiv");
                        //ships.add("tahlan_tower_xiv");
                    }
                    if (Global.getSettings().getModManager().isModEnabled("swp")) {
                        //ships.add("swp_alastor_xiv");
                        if (!"eisdarren".equals(person.getId())) {
                           ships.remove("swp_conquest_xiv"); //I swear there's special lore behind this.
                        }
                        /*ships.add("swp_gryphon_xiv");
                        ships.add("swp_hammerhead_xiv");
                        ships.add("swp_lasher_xiv");
                        ships.add("swp_sunder_xiv");*/
                    }
                } else { //for Celeste really
                    ships.remove("dominator_xiv");
                    //ships.remove("eagle_xiv");
                    ships.remove("enforcer_xiv");
                    //ships.remove("falcon_xiv");
                    ships.remove("onslaught_xiv");
                    fighters.add("eis_piranha_wing");
                    if (Global.getSettings().getModManager().isModEnabled("TORCHSHIPS")){
                        //ships.add("TADA_tinnitus_xiv");
                        ships.remove("TADA_gunwall_XIV");
                    }
                    if (Global.getSettings().getModManager().isModEnabled("swp")) {
                        //ships.add("swp_alastor_xiv");
                        ships.remove("swp_conquest_xiv");
                        ships.remove("swp_gryphon_xiv");
                        ships.remove("swp_hammerhead_xiv");
                        ships.remove("swp_lasher_xiv");
                        //ships.add("swp_sunder_xiv");
                    }
                }
		
		setStartingStage(Stage.WAITING);
		setSuccessStage(Stage.DELIVERED);
		setFailureStage(Stage.FAILED);
		setNoAbandon();
		
		connectWithDaysElapsed(Stage.WAITING, Stage.DELIVERED, PROD_DAYS);
		setStageOnMarketDecivilized(Stage.FAILED, market);
		
		return true;
	}
	
	@Override
	protected void updateInteractionDataImpl() {
		super.updateInteractionDataImpl();
		set("$eis_cpc_ref", true);
	}
	
	@Override
	protected void addMilitaryBlueprints() {
		// Hartley offers every ship and wing Iron Shell knows (no _bp tag filter); the no_sell / station / unboardable / no_drop checks still apply.
		// Every other contact keeps the _bp tag filter.
		boolean anyBlueprint = "eisdarren".equals(getPerson().getId());
		for (String id : faction.getKnownShips()) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec(id);
			if (!anyBlueprint && !(spec.hasTag("eisceleste_bp") || spec.hasTag("eis_bp") || spec.hasTag("heg_aux_bp"))) continue;
                        if (spec.hasTag(Tags.NO_SELL)) continue;
			if (spec.getHints().contains(ShipTypeHints.STATION)) continue;
			if (spec.getHints().contains(ShipTypeHints.UNBOARDABLE) && !spec.getTags().contains(Tags.AUTOMATED_RECOVERABLE))continue;
			ships.add(id);
		}
		for (String id : faction.getKnownWeapons()) {
			WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(id);
                        //if (!spec.hasTag("eisceleste_bp")) continue;
			if (spec.hasTag(Tags.NO_DROP)) continue;
			if (spec.hasTag(Tags.NO_SELL)) continue;
			weapons.add(id);
		}
		for (String id : faction.getKnownFighters()) {
			FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(id);
			if (!anyBlueprint && !(spec.hasTag("eisceleste_bp") || spec.hasTag("eis_bp") || spec.hasTag("heg_aux_bp"))) continue;
                        if (spec.hasTag(Tags.NO_DROP)) continue;
			//if (spec.hasTag(Tags.NO_SELL)) continue;
			fighters.add(id);
		}
	}
}
