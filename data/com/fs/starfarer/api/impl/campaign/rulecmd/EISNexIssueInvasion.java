package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

/**
 * Has Iron Shell and the Hegemony declare war on the target's owner and gives the player a Nexerelin
 * conquest mission for it. Does nothing without Nexerelin.
 * EISNexIssueInvasion <entity id> <float: days>
 */
public class EISNexIssueInvasion extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		eis_nex.issueInvasion(params.get(0).getString(memoryMap), params.get(1).getFloat(memoryMap), dialog);
		return true;
	}
}
