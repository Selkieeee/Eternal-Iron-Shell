package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/**
 * EISPortraitReplace <spritePath>
 * Sets the currently active person's portrait to the given sprite.
 */
public class EISPortraitReplace extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null && dialog.getInteractionTarget() != null && dialog.getInteractionTarget().getActivePerson() != null) {
			return false;
		} else {
			dialog.getInteractionTarget().getActivePerson().setPortraitSprite(params.get(0).getString(memoryMap));
			return true;
		}
	}

}
