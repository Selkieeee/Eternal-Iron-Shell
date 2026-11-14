package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.util.Misc;

/**
 * Checks if the player is commissioned by the Hegemony or Iron Shell, or allied with them.
 * "Allied" is a shared Nexerelin alliance. Without Nexerelin, or when the faction is in no alliance at all,
 * it is 75+ reputation with both the Hegemony and Iron Shell instead (eis_nex.playerIsAllyOf).
 * EISOneOfUs
 * EISOneOfUs <faction id>   - the faction is only used with Nexerelin's random relationships, when the Hegemony and
 *                             Iron Shell aren't allied: is the player an ally of that faction
 */
public class EISOneOfUs extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		if (!eis_nex.isRandomRelationships() || eis_nex.hegemonyAndIronShellAllied()) return ("hegemony".equals(Misc.getCommissionFactionId()) || "ironshell".equals(Misc.getCommissionFactionId()) || eis_nex.playerIsAlly());
		if (!params.isEmpty()) {return eis_nex.playerIsAllyOf(params.get(0).getString(memoryMap));}
		return ("ironshell".equals(Misc.getCommissionFactionId()) || eis_nex.playerIsAlly());
	}
}
