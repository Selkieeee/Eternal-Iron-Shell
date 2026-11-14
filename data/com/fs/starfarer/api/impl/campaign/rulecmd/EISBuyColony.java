package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

/**
 * Hands the player governorship of a market through Nexerelin. Does nothing without Nexerelin.
 * EISBuyColony <entity id>
 */
public class EISBuyColony extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		eis_nex.buyColony(Global.getSector().getEntityById(params.get(0).getString(memoryMap)).getMarket(), dialog);
		return true;
	}
}
