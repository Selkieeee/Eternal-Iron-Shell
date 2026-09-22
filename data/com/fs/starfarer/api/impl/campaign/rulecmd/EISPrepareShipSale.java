package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISPrepareShipSale <ship variant id>   - creates a standalone ship and stashes it for sale
 * EISPrepareShipSale SHOW                - previews the pending ship in the dialog's visual panel
 * EISPrepareShipSale BUY                 - adds the pending ship to the player fleet
 * EISPrepareShipSale CANCEL              - discards the pending ship without adding it
 *
 * (START_QUEST/SHOW_SHIP/MEMORY_CLEANUP), simplified for a single pending sale at a time: the
 * ship is stashed in sector memory (Global.getSector().getMemoryWithoutUpdate()) rather than the
 * per-invocation memoryMap parameter, since a sale conversation needs it to persist across several
 * separate DialogOptionSelected rows (offer -> show ship & price -> buy/decline). The temporary
 * empty-fleet add/remove and CR/crew setup below matches UAF's own approach - a freshly created
 * FleetMemberAPI that was never part of any real fleet can otherwise preview with uninitialized
 * state (0 CR, no crew) in the visual panel.
 *
 * This does not track price/name in memory - callers already know
 * those values at design time and can reference them directly in the option text and any
 * insufficient-credits disable check.
 */
public class EISPrepareShipSale extends BaseCommandPlugin {

    private static final String MEM_KEY = "$eis_shipSalePending";

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.size() == 0) return false;

        String action = params.get(0).getString(memoryMap);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        if ("SHOW".equals(action)) {
            if (dialog == null) return false;
            FleetMemberAPI member = (FleetMemberAPI) mem.get(MEM_KEY);
            if (member == null) return false;
            dialog.getVisualPanel().showFleetMemberInfo(member, true);
        } else if ("BUY".equals(action)) {
            if (dialog == null) return false;
            FleetMemberAPI member = (FleetMemberAPI) mem.get(MEM_KEY);
            if (member == null) return false;
            Global.getSector().getPlayerFleet().getFleetData().addFleetMember(member);
            AddRemoveCommodity.addFleetMemberGainText(member, dialog.getTextPanel());
            mem.unset(MEM_KEY);
        } else if ("CANCEL".equals(action)) {
            mem.unset(MEM_KEY);
        } else {
            String variantId = action;
            ShipVariantAPI variant = Global.getSettings().getVariant(variantId).clone();
            FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);
            member.setShipName(Global.getSector().getFaction("ironshell").pickRandomShipName());
            CampaignFleetAPI tempFleet = Global.getFactory().createEmptyFleet("ironshell", null, true);
            tempFleet.getFleetData().addFleetMember(member);
            tempFleet.getFleetData().removeFleetMember(member);
            member.getCrewComposition().setCrew(100000f);
            member.getRepairTracker().setCR(0.7f);
            mem.set(MEM_KEY, member);
        }

        return true;
    }
}
