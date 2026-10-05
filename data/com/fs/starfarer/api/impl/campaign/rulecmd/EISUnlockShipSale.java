package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISUnlockShipSale <hull id>
 *
 * Lifts the hull's no_sell tag (set in ship_data.csv), so the ship can be stocked by markets and dealers and picked by the
 * surplus ship / production contract offers, which all skip no_sell hulls. The unlock is remembered per save in sector memory.
 * Hull specs are rebuilt from the CSV on every launch, so eis_modPlugin.onGameLoad calls applyAll() to re-apply this save's unlocks.
 *
 * Markets only pick the hull up when their stock next refreshes.
 */
public class EISUnlockShipSale extends BaseCommandPlugin {

	private static final String MEM_KEY = "$eis_saleUnlockedHulls";

	// Hulls this launch has stripped no_sell from. applyAll() puts the tag back first, so unlocks of one save
	// don't carry over into another save loaded in the same session.
	private static final Set<String> strippedThisSession = new HashSet<String>();

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (params.size() == 0) return false;
		String hullId = params.get(0).getString(memoryMap);
		if (Global.getSettings().getHullSpec(hullId) == null) return false;

		getUnlocked(true).add(hullId);
		unlock(hullId);
		return true;
	}

	/** Called from eis_modPlugin.onGameLoad. */
	public static void applyAll() {
		for (String hullId : strippedThisSession) {
			ShipHullSpecAPI spec = Global.getSettings().getHullSpec(hullId);
			if (spec != null) spec.addTag(Tags.NO_SELL);
		}
		strippedThisSession.clear();

		Set<String> unlocked = getUnlocked(false);
		if (unlocked == null) return;
		for (String hullId : unlocked) {
			unlock(hullId);
		}
	}

	private static void unlock(String hullId) {
		ShipHullSpecAPI spec = Global.getSettings().getHullSpec(hullId);
		if (spec == null || !spec.hasTag(Tags.NO_SELL)) return;
		spec.getTags().remove(Tags.NO_SELL);
		strippedThisSession.add(hullId);
	}

	@SuppressWarnings("unchecked")
	private static Set<String> getUnlocked(boolean create) {
		MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
		Set<String> unlocked = (Set<String>) mem.get(MEM_KEY);
		if (unlocked == null && create) {
			unlocked = new HashSet<String>();
			mem.set(MEM_KEY, unlocked);
		}
		return unlocked;
	}
}
