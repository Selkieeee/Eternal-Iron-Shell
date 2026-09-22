package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

// EISAranitiaPlan check <itemId> <optionId> - disables optionId (with an explanatory tooltip) if the
//     player's cargo doesn't have at least 1 of the given special item.
// EISAranitiaPlan checkSP <optionId> - disables optionId (with an explanatory tooltip) if the player
//     doesn't have at least 1 Story Point. Kept separate from "check" so the item and SP requirements
//     never interfere with each other's enabled/tooltip state on the same option.
// EISAranitiaPlan remove <itemId> - removes 1 of the given special item from player cargo and shows the
//     standard vanilla-style highlighted loss text for it.
// EISAranitiaPlan spendSP - spends 1 Story Point directly (no confirmation popup, no bonus XP).
// EISAranitiaPlan install <itemId> [override] - installs the given special item on chicomoztoc's Orbital
//     Works. If that industry already has a colony item installed, it's left alone unless "override" is
//     passed (used for Pristine, which is meant to take precedence; Corrupted is not).
// itemId is expected to be either "pristine_nanoforge" or "corrupted_nanoforge" (Items.PRISTINE_NANOFORGE /
// Items.CORRUPTED_NANOFORGE) - used by the Aranitia "consider your options" nanoforge-decoy plan.
//
// Matches special item stacks by id only (via CargoStackAPI.getSpecialDataIfSpecial().getId()) rather than
// via CargoAPI.getQuantity()/removeItems() with a fresh "new SpecialItemData(id, null)": SpecialItemData's
// equals() requires an EXACT match on both id and data, so a stack whose data isn't null (as can happen
// depending on how the item was acquired) would silently fail to match a null-data query even though the
// player clearly has the item.
public class EISAranitiaPlan extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
            if (dialog == null) return false;
            String mode = params.get(0).getString(memoryMap);
            if ("check".equals(mode)) {
                String itemId = params.get(1).getString(memoryMap);
                String optionId = params.get(2).getString(memoryMap);
                String displayName = "pristine_nanoforge".equals(itemId) ? "Pristine Nanoforge" : "Corrupted Nanoforge";
                if (findStack(itemId) == null) {
                    dialog.getOptionPanel().setEnabled(optionId, false);
                    dialog.getOptionPanel().setTooltip(optionId, "Requires a " + displayName + " in your cargo hold.");
                }
            } else if ("checkSP".equals(mode)) {
                String optionId = params.get(1).getString(memoryMap);
                if (Global.getSector().getPlayerStats().getStoryPoints() < 1) {
                    dialog.getOptionPanel().setEnabled(optionId, false);
                    dialog.getOptionPanel().setTooltip(optionId, "Requires 1 Story Point, which you don't currently have.");
                }
            } else if ("remove".equals(mode)) {
                String itemId = params.get(1).getString(memoryMap);
                CargoStackAPI stack = findStack(itemId);
                if (stack != null) {
                    SpecialItemData data = stack.getSpecialDataIfSpecial();
                    Global.getSector().getPlayerFleet().getCargo().removeItems(CargoAPI.CargoItemType.SPECIAL, data, 1f);
                    AddRemoveCommodity.addItemLossText(data, 1, dialog.getTextPanel());
                }
            } else if ("spendSP".equals(mode)) {
                Global.getSector().getPlayerStats().spendStoryPoints(1, true, dialog.getTextPanel(), false, "Aranitia nanoforge decoy plan");
            } else if ("install".equals(mode)) {
                String itemId = params.get(1).getString(memoryMap);
                boolean override = params.size() > 2 && "override".equals(params.get(2).getString(memoryMap));
                MarketAPI market = Global.getSector().getEconomy().getMarket("chicomoztoc");
                if (market != null) {
                    Industry orbitalWorks = market.getIndustry(Industries.ORBITALWORKS);
                    if (orbitalWorks != null && (orbitalWorks.getSpecialItem() == null || override)) {
                        orbitalWorks.setSpecialItem(new SpecialItemData(itemId, null));
                    }
                }
            }
            return true;
	}

        private static CargoStackAPI findStack(String itemId) {
            CargoAPI cargo = Global.getSector().getPlayerFleet().getCargo();
            for (CargoStackAPI stack : cargo.getStacksCopy()) {
                if (stack.isSpecialStack() && stack.getSpecialDataIfSpecial() != null
                        && itemId.equals(stack.getSpecialDataIfSpecial().getId())
                        && stack.getSize() >= 1f) {
                    return stack;
                }
            }
            return null;
        }
}
