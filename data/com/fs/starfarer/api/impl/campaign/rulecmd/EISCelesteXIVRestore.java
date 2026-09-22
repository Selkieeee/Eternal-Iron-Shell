package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISCelesteXIVRestore PICK     - shows the fleet member picker for eligible unrestorable XIV hulls
 * EISCelesteXIVRestore REPAIR   - spends gamma cores and clears the picked ship's d-mods
 * EISCelesteXIVRestore CANCEL   - discards the pending pick without repairing it
 * EISCelesteXIVRestore hasCores - check used by the disable row to grey out Approve when short on cores
 *
 * Eligible ships are fleet members with the "fourteenth" built-in hullmod (Iron Shell's XIV-pattern
 * marker, also checked by eis_xiv.java) whose hull spec or variant carries Tags.HULL_UNRESTORABLE /
 * Tags.VARIANT_UNRESTORABLE (the vanilla tags that block a ship from having its d-mods repaired
 * through the normal Recondition/salvage-crew route - typically a "worn hull condition" derelict
 * salvage) and that have at least one non-built-in d-mod. Caeda's justification is that these are
 * recovered pieces of the automated XIV defender fleet from the vanilla "Recover a Planetkiller"
 * questline, restorable only through House Celeste's own reverse-engineering work.
 *
 * The gamma core cost equals the ship's d-mod count and is charged entirely here rather than via a
 * separate AddRemoveCommodity rules.csv line, since that count is only known once a specific ship
 * has been picked - there is no static number to put in rules.csv for it.
 */
public class EISCelesteXIVRestore extends BaseCommandPlugin {

    private static final String MEM_SHIP_KEY = "$eis_celesteRestoreShip";
    private static final String MEM_COST_KEY = "$eis_celesteRestoreCost";
    private static final String GAMMA_CORE = "gamma_core";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.size() == 0) return false;
        String action = params.get(0).getString(memoryMap);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        if ("PICK".equals(action)) {
            pickShip(dialog, mem);
            return true;
        }
        if ("REPAIR".equals(action)) {
            return repair(dialog, mem);
        }
        if ("CANCEL".equals(action)) {
            mem.unset(MEM_SHIP_KEY);
            mem.unset(MEM_COST_KEY);
            return true;
        }
        if ("hasCores".equals(action)) {
            Integer cost = (Integer) mem.get(MEM_COST_KEY);
            if (cost == null) return false;
            return Global.getSector().getPlayerFleet().getCargo().getCommodityQuantity(GAMMA_CORE) >= cost;
        }
        return false;
    }

    private boolean isEligible(FleetMemberAPI member) {
        ShipVariantAPI variant = member.getVariant();
        if (!variant.hasHullMod("fourteenth")) return false;
        boolean unrestorable = member.getHullSpec().hasTag(Tags.HULL_UNRESTORABLE) || variant.hasTag(Tags.VARIANT_UNRESTORABLE);
        if (!unrestorable) return false;
        return DModManager.getNumNonBuiltInDMods(variant) >= 1;
    }

    private void returnToCelesteMenu(InteractionDialogAPI dialog) {
        dialog.getOptionPanel().clearOptions();
        dialog.getOptionPanel().addOption("Continue", "EISCelesteXIVSalvageBack");
        dialog.getPlugin().optionSelected("Continue", "EISCelesteXIVSalvageBack");
    }

    private void pickShip(final InteractionDialogAPI dialog, final MemoryAPI mem) {
        List<FleetMemberAPI> pool = new ArrayList<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (isEligible(member)) pool.add(member);
        }

        if (pool.isEmpty()) {
            dialog.getTextPanel().addParagraph("No eligible recovered XIV hulls found.");
            returnToCelesteMenu(dialog);
            return;
        }

        dialog.showFleetMemberPickerDialog(
                "Select ship to restore",
                "Confirm", "Cancel",
                4, 6, 58f,
                false, false,
                pool,
                new FleetMemberPickerListener() {
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                if (members == null || members.isEmpty() || members.get(0) == null) return;

                FleetMemberAPI picked = members.get(0);
                int cost = DModManager.getNumNonBuiltInDMods(picked.getVariant());
                if (cost < 1) return;

                mem.set(MEM_SHIP_KEY, picked);
                mem.set(MEM_COST_KEY, cost);

                dialog.getOptionPanel().clearOptions();
                dialog.getOptionPanel().addOption("Continue", "EISCelesteXIVSalvageConfirm");
                dialog.getPlugin().optionSelected("Continue", "EISCelesteXIVSalvageConfirm");
            }

            @Override
            public void cancelledFleetMemberPicking() {
                returnToCelesteMenu(dialog);
            }
        }
        );
    }

    private boolean repair(InteractionDialogAPI dialog, MemoryAPI mem) {
        if (dialog == null) return false;
        FleetMemberAPI member = (FleetMemberAPI) mem.get(MEM_SHIP_KEY);
        Integer cost = (Integer) mem.get(MEM_COST_KEY);
        if (member == null || cost == null) return false;

        ShipVariantAPI variant = member.getVariant();

        Global.getSector().getPlayerFleet().getCargo().removeCommodity(GAMMA_CORE, cost);
        AddRemoveCommodity.addCommodityLossText(GAMMA_CORE, cost, dialog.getTextPanel());

        List<String> dmodIds = new ArrayList<>();
        for (String id : variant.getHullMods()) {
            if (variant.getHullSpec().getBuiltInMods().contains(id)) continue;
            if (DModManager.getMod(id).hasTag(Tags.HULLMOD_DMOD)) dmodIds.add(id);
        }
        for (String id : dmodIds) {
            DModManager.removeDMod(variant, id);
        }

        TextPanelAPI text = dialog.getTextPanel();
        text.setFontSmallInsignia();
        text.addParagraph(member.getShipName() + "'s hull has been restored", Misc.getPositiveHighlightColor());
        text.highlightInLastPara(Misc.getHighlightColor(), "hull has been restored");
        text.setFontInsignia();

        mem.unset(MEM_SHIP_KEY);
        mem.unset(MEM_COST_KEY);

        return true;
    }
}
