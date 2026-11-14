package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.eventide.DuelPanel;
import com.fs.starfarer.api.util.Misc;
import data.scripts.EISDuelDialogDelegate;
import java.util.List;
import java.util.Map;

/**
 * Runs the base game's sword duel (the one from the Eventide story mission). Works without Nexerelin.
 *
 * EISFencingDuel start <player skilled: boolean> <opponent skilled: boolean>
 *     When the duel ends, sets $eis_playerWonDuel or $eis_playerLostDuel (local memory, this rule only)
 *     and fires the EISFencingDuelFinished trigger.
 * EISFencingDuel tutorial <player skilled: boolean>
 *     Practice duel. Fires the EISFencingTutorialFinished trigger when it ends.
 */
public class EISFencingDuel extends BaseCommandPlugin {
	@Override
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		String arg = params.get(0).getString(memoryMap);
		boolean playerSkilled = params.size() >= 2 && params.get(1).getBoolean(memoryMap);
		switch (arg)
		{
			case "start":
			{
				boolean enemySkilled = params.size() >= 3 && params.get(2).getBoolean(memoryMap);
				DuelPanel duelPanel = DuelPanel.createDefault(playerSkilled, enemySkilled, "soe_ambience");
				dialog.showCustomVisualDialog(1024, 700, new EISDuelDialogDelegate(duelPanel, dialog, memoryMap, false));
				return true;
			}
			case "tutorial":
			{
				DuelPanel duelPanel = DuelPanel.createTutorial(playerSkilled, "soe_ambience");
				dialog.showCustomVisualDialog(1024, 700, new EISDuelDialogDelegate(duelPanel, dialog, memoryMap, true));
				return true;
			}
		}
		return false;
	}
}
