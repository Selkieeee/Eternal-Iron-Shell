package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.FleetMemberPickerListener;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISXIVConversion PICK   - shows the fleet member picker for eligible source hulls
 * EISXIVConversion SHOW   - previews the resulting XIV hull in the dialog's visual panel
 * EISXIVConversion BUY    - performs the conversion (credits are deducted separately via
 *                           AddRemoveCommodity in rules.csv; the Story Point cost of the one-time
 *                           conversions is charged by the game itself, via SetStoryOption on the
 *                           Authorize option in the preview rows - this command never spends SP)
 * EISXIVConversion CANCEL - discards the pending picked ship without converting it
 * EISXIVConversion hasSP <n> - check used by disable rows to grey out Authorize when short on SP
 *
 * Modeled on Arma Armatura's armaa_CoreUnitPickerCMD (showFleetMemberPickerDialog usage, and its
 * pattern of driving the dialog's option panel directly from a picker callback) and
 * armaa_valkazardCMD (the hullmod-transfer logic for swapping a ship onto a same-slot hull),
 * adapted for Hartley's "Request XIV hull conversion" reward. The new ship is built from a bare
 * hull rather than any specific pre-built variant - only the old ship's earned hullmods (S-mods,
 * story mods, built-ins) carry over, same fields armaa_valkazardCMD transfers. The player refits
 * the result themselves afterward, same as a real hull swap would require.
 */
public class EISXIVConversion extends BaseCommandPlugin {

    private static final String MEM_OLD_KEY = "$eis_xivConversionOld";
    private static final String MEM_TYPE_KEY = "$eis_xivConversionType";

    private static class ConversionInfo {
        final String sourceHullId;
        final String targetHullId;
        final boolean oneTime;
        final String doneFlagKey;
        final String previewOptionId;
        /** Non-null if this conversion requires a hull spec from another mod to be loaded
         *  (e.g. Tahlan Shipworks' Castigator (XIV)) - checked the same way eis_modPlugin does
         *  for the Castigator (XIV) blueprint elsewhere, so this offer can't appear, be picked,
         *  previewed, or completed for players who don't have that mod installed. */
        final String requiredHullSpecId;

        ConversionInfo(String sourceHullId, String targetHullId, boolean oneTime, String doneFlagKey, String previewOptionId, String requiredHullSpecId) {
            this.sourceHullId = sourceHullId;
            this.targetHullId = targetHullId;
            this.oneTime = oneTime;
            this.doneFlagKey = doneFlagKey;
            this.previewOptionId = previewOptionId;
            this.requiredHullSpecId = requiredHullSpecId;
        }

        boolean isAvailable() {
            return requiredHullSpecId == null || Global.getSettings().getHullSpec(requiredHullSpecId) != null;
        }
    }

    private static final ConversionInfo[] ALL = {
        new ConversionInfo("tahlan_Castigator", "tahlan_Castigator_xiv", true, "$EISHartleyXIVCastigatorDone", "EISHartleyXIVCastigatorPreview", "tahlan_Castigator_xiv"),
        new ConversionInfo("legion", "legion_xiv", true, "$EISHartleyXIVLegionDone", "EISHartleyXIVLegionPreview", null),
        new ConversionInfo("champion", "eis_champion", false, null, "EISHartleyXIVChampionPreview", null),
        new ConversionInfo("eradicator", "eis_eradicator", false, null, "EISHartleyXIVEradicatorPreview", null),
    };

    private static ConversionInfo infoForTarget(String targetHullId) {
        for (ConversionInfo info : ALL) {
            if (info.targetHullId.equals(targetHullId) && info.isAvailable()) return info;
        }
        return null;
    }

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.size() == 0) return false;
        String action = params.get(0).getString(memoryMap);
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();

        if ("PICK".equals(action)) {
            pickShip(dialog, mem);
            return true;
        }
        if ("SHOW".equals(action)) {
            return showPreview(dialog, mem);
        }
        if ("BUY".equals(action)) {
            return performConversion(dialog, mem);
        }
        if ("CANCEL".equals(action)) {
            mem.unset(MEM_OLD_KEY);
            mem.unset(MEM_TYPE_KEY);
            return true;
        }
        if ("hasSP".equals(action)) {
            int needed = params.get(1).getInt(memoryMap);
            return Global.getSector().getPlayerStats().getStoryPoints() >= needed;
        }
        return false;
    }

    private void pickShip(final InteractionDialogAPI dialog, final MemoryAPI mem) {
        List<FleetMemberAPI> pool = new ArrayList<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            String hullId = member.getHullSpec().getHullId();
            for (ConversionInfo info : ALL) {
                if (!info.sourceHullId.equals(hullId)) continue;
                if (!info.isAvailable()) break;
                if (info.oneTime && mem.getBoolean(info.doneFlagKey)) break;
                pool.add(member);
                break;
            }
        }

        if (pool.isEmpty()) {
            dialog.getTextPanel().addParagraph("No eligible ships found for XIV conversion.");
            return;
        }

        dialog.showFleetMemberPickerDialog(
                "Select ship for XIV conversion",
                "Confirm", "Cancel",
                4, 6, 58f,
                false, false,
                pool,
                new FleetMemberPickerListener() {
            @Override
            public void pickedFleetMembers(List<FleetMemberAPI> members) {
                if (members == null || members.isEmpty() || members.get(0) == null) return;

                FleetMemberAPI picked = members.get(0);
                String hullId = picked.getHullSpec().getHullId();
                ConversionInfo info = null;
                for (ConversionInfo candidate : ALL) {
                    if (candidate.sourceHullId.equals(hullId) && candidate.isAvailable()) {
                        info = candidate;
                        break;
                    }
                }
                if (info == null) return;

                mem.set(MEM_OLD_KEY, picked);
                mem.set(MEM_TYPE_KEY, info.targetHullId);

                dialog.getOptionPanel().clearOptions();
                dialog.getOptionPanel().addOption("Continue", info.previewOptionId);
                dialog.getPlugin().optionSelected("Continue", info.previewOptionId);
            }

            @Override
            public void cancelledFleetMemberPicking() {
                dialog.getOptionPanel().clearOptions();
                dialog.getOptionPanel().addOption("Select ship for XIV conversion", "EISHartleyXIVConversionPick");
                dialog.getOptionPanel().addOption("Back", "EISHartleyXIVConversionBack");
            }
        }
        );
    }

    private boolean showPreview(InteractionDialogAPI dialog, MemoryAPI mem) {
        if (dialog == null) return false;
        String targetHullId = (String) mem.get(MEM_TYPE_KEY);
        if (targetHullId == null) return false;

        ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(targetHullId);
        if (hullSpec == null) return false;

        ShipVariantAPI variant = Global.getSettings().createEmptyVariant(targetHullId + "_xivConversionPreview", hullSpec);
        FleetMemberAPI preview = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variant);

        CampaignFleetAPI tempFleet = Global.getFactory().createEmptyFleet("ironshell", null, true);
        tempFleet.getFleetData().addFleetMember(preview);
        tempFleet.getFleetData().removeFleetMember(preview);
        preview.getCrewComposition().setCrew(100000f);
        preview.getRepairTracker().setCR(0.7f);

        dialog.getVisualPanel().showFleetMemberInfo(preview, true);
        return true;
    }

    private boolean performConversion(InteractionDialogAPI dialog, MemoryAPI mem) {
        if (dialog == null) return false;
        FleetMemberAPI oldMember = (FleetMemberAPI) mem.get(MEM_OLD_KEY);
        String targetHullId = (String) mem.get(MEM_TYPE_KEY);
        if (oldMember == null || targetHullId == null) return false;

        ConversionInfo info = infoForTarget(targetHullId);
        if (info == null) return false;

        ShipHullSpecAPI hullSpec = Global.getSettings().getHullSpec(targetHullId);
        if (hullSpec == null) return false;

        ShipVariantAPI oldVariant = oldMember.getVariant().clone();
        ShipVariantAPI newVariant = Global.getSettings().createEmptyVariant(targetHullId + "_xivConversion", hullSpec);
        newVariant.setSource(VariantSource.REFIT);

        for (String hullmod : oldVariant.getPermaMods()) {
            boolean sMod = oldVariant.getSModdedBuiltIns().contains(hullmod);
            if (sMod) {
                newVariant.removePermaMod(hullmod);
                newVariant.addPermaMod(hullmod, true);
            }
        }
        for (String hullmod : oldVariant.getSMods()) {
            newVariant.removePermaMod(hullmod);
            newVariant.addPermaMod(hullmod, true);
        }
        for (String hullmod : oldVariant.getSModdedBuiltIns()) {
            newVariant.getHullMods().remove(hullmod);
            newVariant.removePermaMod(hullmod);
            newVariant.addPermaMod(hullmod, true);
        }
        for (String hullmod : oldVariant.getNonBuiltInHullmods()) {
            boolean sMod = oldVariant.getSMods().contains(hullmod);
            if (sMod) {
                newVariant.addPermaMod(hullmod, true);
            } else {
                newVariant.addMod(hullmod);
            }
        }
        for (String hullmod : oldVariant.getSuppressedMods()) {
            if (!newVariant.getSuppressedMods().contains(hullmod)) {
                boolean sMod = oldVariant.getSMods().contains(hullmod);
                if (sMod) {
                    newVariant.addPermaMod(hullmod, true);
                } else {
                    newVariant.addMod(hullmod);
                }
            }
        }

        FleetMemberAPI newMember = Global.getFactory().createFleetMember(FleetMemberType.SHIP, newVariant);
        newMember.setShipName(oldMember.getShipName());
        newMember.getCrewComposition().setCrew(100000f);
        newMember.getRepairTracker().setCR(0.7f);

        PersonAPI captain = oldMember.getCaptain();
        String id = oldMember.getId();

        // Deliberately no "lost ship" / "gained ship" text: the conversion is presented as one refit, not a swap.
        FleetDataAPI fleetData = oldMember.getFleetData();
        fleetData.scuttle(oldMember);

        newMember.setId(id);
        if (captain != null) {
            newMember.setCaptain(captain);
        }
        fleetData.addFleetMember(newMember);

        if (info.oneTime) {
            // The Story Point itself is spent by the game: the Authorize option is registered with SetStoryOption
            // in its preview row (rules.csv), so nothing is spent here.
            Global.getSector().getMemoryWithoutUpdate().set(info.doneFlagKey, true);
        }

        TextPanelAPI text = dialog.getTextPanel();
        text.setFontSmallInsignia();
        text.addParagraph("XIV battlegroup conversion complete", Misc.getPositiveHighlightColor());
        text.highlightInLastPara(Misc.getHighlightColor(), "XIV battlegroup conversion complete");
        text.setFontInsignia();

        mem.unset(MEM_OLD_KEY);
        mem.unset(MEM_TYPE_KEY);

        return true;
    }
}
