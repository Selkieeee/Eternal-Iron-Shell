package data.scripts;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.eventide.DuelDialogDelegate;
import com.fs.starfarer.api.impl.campaign.eventide.DuelPanel;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import java.util.Map;

/**
 * The base game's duel dialog, reporting the result through Iron Shell's own memory keys and triggers
 * instead of the Eventide mission's. Used by the EISFencingDuel rule command.
 */
public class EISDuelDialogDelegate extends DuelDialogDelegate {

	public EISDuelDialogDelegate(DuelPanel duelPanel, InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, boolean tutorialMode) {
		super(null, duelPanel, dialog, memoryMap, tutorialMode);
	}

	@Override
	public void reportDismissed(int option) {
		if (memoryMap == null) return;
		if (tutorialMode) {
			FireBest.fire(null, dialog, memoryMap, "EISFencingTutorialFinished");
			return;
		}
		if (duelPanel.getPlayer().health > 0) {
			memoryMap.get(MemKeys.LOCAL).set("$eis_playerWonDuel", true, 0);
		} else {
			memoryMap.get(MemKeys.LOCAL).set("$eis_playerLostDuel", true, 0);
		}
		FireBest.fire(null, dialog, memoryMap, "EISFencingDuelFinished");
	}
}
