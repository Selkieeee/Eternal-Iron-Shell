package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Map;

/**
 * Puts a confirmation prompt on an option. Does the same as Nexerelin's Nex_AddOptionConfirmation,
 * without needing Nexerelin installed.
 * EIS_AddOptionConfirmation <option id> <prompt text> <confirm text> <cancel text>
 */
public class EIS_AddOptionConfirmation extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		String optionId = params.get(0).getString(memoryMap);
		String text = params.get(1).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		String yes = params.get(2).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		String no = params.get(3).getStringWithTokenReplacement(ruleId, dialog, memoryMap);
		dialog.getOptionPanel().addOptionConfirmation(optionId, text, yes, no);
		return true;
	}
}
