package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import data.scripts.EISContactFaction;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepRewards;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.WeightedRandomPicker;

public class EISSurplusShipHull extends HubMissionWithSearch {

	public static final float BASE_PRICE_MULT = 0.4f;
	// Weight of a hull that is not tagged eis_bp, relative to 1 for an eis_bp hull.
	public static final float NON_BP_WEIGHT = 0.5f;
	// Hulls tagged no_sell that this mission may still offer.
	private static final Set<String> NO_SELL_ALLOWED = new HashSet<String>(Arrays.asList("armaa_monitor_xiv"));
	
	protected FleetMemberAPI member;
	protected int price;

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
		MarketAPI market = person.getMarket();
		if (market == null) return false;
		
		//if (!Misc.isMilitary(market) && market.getSize() < 7) return false;
		
		if (!setPersonMissionRef(person, "$sShip_ref")) {
			return false;
		}
		
		//genRandom = Misc.random;
		
		
		// Every hull the contact's faction knows is a candidate. The pick ignores contact importance, campaign age, reputation and fleet doctrine.
		// Hulls tagged eis_bp have full weight; every other hull (vanilla and cross-mod alike) has NON_BP_WEIGHT.
		FactionAPI pickFaction = EISContactFaction.get(person);
		WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(genRandom);
		for (String hullId : pickFaction.getKnownShips()) {
			ShipHullSpecAPI spec;
			try {
				spec = Global.getSettings().getHullSpec(hullId);
			} catch (Throwable t) {
				continue;
			}
			if (spec == null) continue;
			// freighters, tankers and stations are not warship hulls
			if (spec.isCivilianNonCarrier() || spec.getHints().contains(ShipTypeHints.STATION)) continue;
			// no_sell hulls are skipped unless this mission is explicitly allowed to offer them
			if (spec.hasTag(Tags.NO_SELL) && !NO_SELL_ALLOWED.contains(spec.getHullId())) continue;
			// cruisers and capitals must be Iron Shell blueprint hulls
			HullSize pickedSize = spec.getHullSize();
			if ((pickedSize == HullSize.CRUISER || pickedSize == HullSize.CAPITAL_SHIP) && !spec.hasTag("eis_bp")) continue;
			picker.add(spec.getHullId(), spec.hasTag("eis_bp") ? 1f : NON_BP_WEIGHT);
		}
		
		// A drawn hull can still be rejected below, so keep drawing (without replacement) until one passes. Rejections don't change the relative odds.
		ShipVariantAPI variant = null;
		while (!picker.isEmpty()) {
			String hullId = picker.pickAndRemove();
			// the stock hull variant has to exist
			ShipVariantAPI candidate;
			try {
				ShipVariantAPI stock = Global.getSettings().getVariant(hullId + "_Hull");
				if (stock == null) continue;
				candidate = stock.clone();
			} catch (Throwable t) {
				continue;
			}
			// eis_aquila can't be installed on top of Safety Overrides, and every ship sold here must have it
			if (candidate.hasHullMod(HullMods.SAFETYOVERRIDES)) continue;
			variant = candidate;
			break;
		}
		if (variant == null) return false;
			
		member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant.clone());
		//assignShipName(member, Factions.INDEPENDENT);
                // Every ship is an Iron Shell refit with the built-in Aquila Reactor (hulls with Safety Overrides are rejected in the pick loop above)
                assignShipName(member, "ironshell");
                if (!member.getVariant().hasHullMod("eis_aquila")) {
                    member.getVariant().addPermaMod("eis_aquila", true);
                    member.getVariant().setSource(VariantSource.REFIT);
                    member.getVariant().addTag(Tags.VARIANT_ALWAYS_RETAIN_SMODS_ON_SALVAGE);
                }
                
                
		/*float quality = ShipQuality.getShipQuality(market, person.getFaction().getId());
		float averageDmods = DefaultFleetInflater.getAverageDmodsForQuality(quality);
		int addDmods = DefaultFleetInflater.getNumDModsToAdd(variant, averageDmods, genRandom);
		if (addDmods > 0) {
			DModManager.setDHull(variant);
			DModManager.addDMods(member, true, addDmods, genRandom);
		}*/
		member.getCrewComposition().setCrew(100000);
		member.getRepairTracker().setCR(0.7f);
		
		/*if (BASE_PRICE_MULT == 1f) {
			price = (int) Math.round(variant.getHullSpec().getBaseValue());
		} else {*/
			price = getRoundNumber(variant.getHullSpec().getBaseValue() * BASE_PRICE_MULT);
		//}
		
		// reputation on purchase: person +5 / faction +3 (penalties unchanged: person -1 / faction 0)
		setRepChanges(0.05f, RepRewards.TINY, 0.03f, 0f);
		
		return true;
	}
	
	protected void updateInteractionDataImpl() {
		// this is weird - in the accept() method, the mission is aborted, which unsets
		// $sShip_ref. So: we use $sShip_ref2 in the ContactPostAccept rule
		// and $sShip_ref2 has an expiration of 0, so it'll get unset on its own later.
		set("$sShip_ref2", this);
                set("$sShip_aquila", true);
		set("$sShip_hullSize", member.getHullSpec().getDesignation().toLowerCase());
		set("$sShip_hullClass", member.getHullSpec().getHullNameWithDashClass());
		set("$sShip_price", Misc.getWithDGS(price));
		set("$sShip_manOrWoman", getPerson().getManOrWoman());
		set("$sShip_rank", getPerson().getRank().toLowerCase());
		set("$sShip_rankAOrAn", getPerson().getRankArticle());
                set("$sShip_heOrShe", getPerson().getHeOrShe());
		set("$sShip_hisOrHer", getPerson().getHisOrHer());
		set("$sShip_member", member);
	}
	
	@Override
	protected boolean callAction(String action, String ruleId, InteractionDialogAPI dialog, List<Token> params,
							     Map<String, MemoryAPI> memoryMap) {
		if ("showShip".equals(action)) {
			dialog.getVisualPanel().showFleetMemberInfo(member, true);
			return true;
		} else if ("showPerson".equals(action)) {
			dialog.getVisualPanel().showPersonInfo(getPerson(), true);
			return true;
		}
		return false;
	}

	@Override
	public String getBaseName() {
		return "Surplus Ship Hull"; // not used I don't think
	}
	
	@Override
	public void accept(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		// it's just an transaction immediate transaction handled in rules.csv
		// no intel item etc
		
		currentStage = new Object(); // so that the abort() assumes the mission was successful
		abort();
	}
	
}

