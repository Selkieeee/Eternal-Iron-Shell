package com.fs.starfarer.api.impl.campaign.rulecmd;


import com.fs.starfarer.api.Global;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.OptionId;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISCheck
 */

public class EISCheck extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
            if (dialog == null) return false;
            // Light Aranitia fleets (see Amongus.UhohStinky) don't force engagement - leave the base
            // Story Point "Clean Disengage" option untouched and skip the custom tooltip entirely.
            if (dialog.getInteractionTarget() != null && dialog.getInteractionTarget().getMemoryWithoutUpdate().getBoolean("$EIS_YKWYD_light")) {
                return true;
            }
            dialog.getOptionPanel().setEnabled(OptionId.CLEAN_DISENGAGE, false);
            dialog.getOptionPanel().setTooltip(OptionId.CLEAN_DISENGAGE, Global.getSettings().getString("eis_ironshell", "EISStoryPointForceEngage"));
            dialog.getOptionPanel().setTooltipHighlightColors(OptionId.CLEAN_DISENGAGE, Misc.getStoryOptionColor());
            dialog.getOptionPanel().setTooltipHighlights(OptionId.CLEAN_DISENGAGE, Global.getSettings().getString("eis_ironshell", "EISStoryPointForceEngageHighlight"));
            return true;
        }
}