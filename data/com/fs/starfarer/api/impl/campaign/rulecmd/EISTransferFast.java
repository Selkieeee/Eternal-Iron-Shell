package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.impl.campaign.ids.Factions;

/**
 * Transfers a market from the player to a faction through Nexerelin. Does nothing without Nexerelin.
 * EISTransferFast <entity id> <faction id>
 */
public class EISTransferFast extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		eis_nex.transferMarket(Global.getSector().getEntityById(params.get(0).getString(memoryMap)).getMarket(), Global.getSector().getFaction(params.get(1).getString(memoryMap)), Global.getSector().getFaction(Factions.PLAYER));
		return true;
	}
}
