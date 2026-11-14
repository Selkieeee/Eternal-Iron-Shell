package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

/**
 * The player faction music picker on Nexerelin's market menu. The work is done in data.scripts.eis_NexMusicDirectory;
 * without Nexerelin this returns false and its rules never fire (they hang off Nexerelin's own triggers).
 * Nexual_Connection setVariables | listGroups | listFactions <group number> | printMusic | setMusic
 */
public class Nexual_Connection extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		return eis_nex.musicDirectory(ruleId, dialog, params, memoryMap);
	}
}
