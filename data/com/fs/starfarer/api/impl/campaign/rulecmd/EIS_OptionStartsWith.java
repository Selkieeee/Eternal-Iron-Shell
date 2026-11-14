package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Map;

/**
 * True if the selected option's id starts with the given text. Does the same as Nexerelin's
 * Nex_OptionStartsWith, without needing Nexerelin installed.
 * EIS_OptionStartsWith <prefix>
 */
public class EIS_OptionStartsWith extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		String arg = params.get(0).getString(memoryMap);
		String option = memoryMap.get(MemKeys.LOCAL).getString("$option");
		if (option == null) return false;
		return option.startsWith(arg);
	}
}
