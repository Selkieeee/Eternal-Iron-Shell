package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

/**
 * Creates a Nexerelin diplomacy event. Does nothing without Nexerelin.
 * EISCreateDiplomacyEvent <faction id> <other faction id> <event id>
 */
public class EISCreateDiplomacyEvent extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		eis_nex.createDiplomacyEvent(Global.getSector().getFaction(params.get(0).getString(memoryMap)), Global.getSector().getFaction(params.get(1).getString(memoryMap)), params.get(2).getString(memoryMap));
		return true;
	}
}
