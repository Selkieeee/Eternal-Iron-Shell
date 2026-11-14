package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

/**
 * Sends an Iron Shell colony expedition from Chitagupta if the target is unowned, otherwise an invasion.
 * Both are Nexerelin systems; does nothing without Nexerelin.
 * EISNexColonize <entity id>
 */
public class EISNexColonize extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		eis_nex.colonizeOrInvade(params.get(0).getString(memoryMap), dialog);
		return true;
	}
}
