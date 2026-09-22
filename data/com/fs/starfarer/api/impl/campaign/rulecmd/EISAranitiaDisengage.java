package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.OptionId;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

// EISAranitiaDisengage
// Invokes FleetInteractionDialogPluginImpl's own OptionId.CLEAN_DISENGAGE handling directly, so the fleet
// escapes exactly as if the player had picked vanilla's "Disengage by executing a series of special
// maneuvers" option - but for free, without the Story Point cost/confirmation popup, since the plan's cost
// was already paid via the sacrificed nanoforge.
public class EISAranitiaDisengage extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
            if (dialog == null || dialog.getPlugin() == null) return false;
            if (dialog.getPlugin() instanceof FleetInteractionDialogPluginImpl) {
                ((FleetInteractionDialogPluginImpl) dialog.getPlugin()).optionSelected(null, OptionId.CLEAN_DISENGAGE);
            }
            return true;
	}
}
