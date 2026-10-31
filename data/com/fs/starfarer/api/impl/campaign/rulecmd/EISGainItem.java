package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

// EISGainItem <special item id> <quantity>
// Adds special items to the player's cargo and announces it in the same small highlighted style as EISLearn
// ("You have acquired [item name]"), instead of vanilla AddRemoveAnyItem's gain line.
public class EISGainItem extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, final Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		String itemId = params.get(0).getString(memoryMap);
		int quantity = params.size() > 1 ? params.get(1).getInt(memoryMap) : 1;
		if (quantity <= 0) return false;
		Global.getSector().getPlayerFleet().getCargo().addSpecial(new SpecialItemData(itemId, null), quantity);
		String name = Global.getSettings().getSpecialItemSpec(itemId).getName();
		if (quantity > 1) name = quantity + "x " + name;
		dialog.getTextPanel().setFontSmallInsignia();
		dialog.getTextPanel().addPara(Global.getSettings().getString("eis_ironshell", "EISGainItem") + name, Misc.getHighlightColor());
		dialog.getTextPanel().setFontInsignia();
		return true;
	}
}
