package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.Misc;

import data.scripts.campaign.intel.EISHellOfOurOwnMakingIntel;
import data.scripts.campaign.intel.EISHellOfOurOwnMakingTalkTrigger;

/**
 * Drives Hartley's "Hell of Our Own Making" mini-quest from rules.csv.
 *
 * EISHellOfOurOwnMaking start      adds the intel (does nothing if the player already has it)
 * EISHellOfOurOwnMaking deliver    marks the Usurpers intel as handed to Hartley (adds the intel first if needed)
 * EISHellOfOurOwnMaking complete   ends the quest: final intel update, then the intel ends (nothing if the player never
 *                                  had the intel, e.g. a first meeting during the civil war)
 * EISHellOfOurOwnMaking sdtuReached <STAGE>
 *                                  condition: vanilla The Usurpers is at that TheUsurpers.Stage or later, or completed
 * EISHellOfOurOwnMaking join       Hartley (eisdarren) joins the player fleet as an officer, without a ship, and is taken
 *                                  off any Iron Shell patrol he was commanding
 * EISHellOfOurOwnMaking showAcheron
 *                                  shows the Acheron variant in the dialog's visual panel
 * EISHellOfOurOwnMaking acheronCosts
 *                                  turns the two "bring the flagship" options into story point options (see below)
 * EISHellOfOurOwnMaking hartleyInFleet
 *                                  condition: Hartley is an officer in the player fleet
 * EISHellOfOurOwnMaking leave      Hartley leaves the player fleet and takes the EIS Acheron with him (unless the player
 *                                  is flying it as the flagship); sets $global.eis_hoom_hartleyLeft
 * EISHellOfOurOwnMaking giveAcheron
 *                                  adds the EIS Acheron to the player fleet with Hartley as its captain
 *
 * The rules track the quest with global flags ($global.eis_hoom_accepted, $global.eis_hoom_intelDelivered, ...);
 * this command keeps the intel in step with them and does what rules.csv cannot.
 */
public class EISHellOfOurOwnMaking extends BaseCommandPlugin {

    public static final String HARTLEY_ID = EISHellOfOurOwnMakingIntel.HARTLEY_ID;
    public static final String ACHERON_VARIANT = "eis_acheron_Vengeance";
    // a unique ship in-universe: always this name (EIS prefix as on Kim's EIS Hoan Kiem in IronShellHQ)
    public static final String ACHERON_SHIP_NAME = "EIS Acheron";

    public static final String OPT_CONNECTIONS = "eis_hoom_acheronConnections";
    public static final String OPT_MANAGE = "eis_hoom_acheronManage";
    // bonus XP fractions, from the bonusXP block in data/config/settings.json
    public static final String BONUS_XP_CONNECTIONS = "eis_hoomAcheronConnections";
    public static final String BONUS_XP_MANAGE = "eis_hoomAcheronManage";

    // survival (extended) mode: the story point cost of recovering a capital ship that wasn't yours, [frigate, destroyer, cruiser, capital]
    public static final String EXT_RECOVERY_SP_COST_KEY = "ext_recoveryNonOwnSPCost";
    public static final int DEFAULT_CAPITAL_SP_COST = 4;

    private static final String JOIN_SOUND = "ui_contact_developed";

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);
        TextPanelAPI text = dialog != null ? dialog.getTextPanel() : null;

        if ("start".equals(action)) {
            EISHellOfOurOwnMakingIntel.start(text);
            return true;
        }
        if ("sdtuReached".equals(action)) {
            if (params.size() < 2) return false;
            return EISHellOfOurOwnMakingIntel.isUsurpersStageReached(params.get(1).getString(memoryMap));
        }
        if ("deliver".equals(action)) {
            EISHellOfOurOwnMakingIntel intel = EISHellOfOurOwnMakingIntel.start(text);
            intel.setStage(EISHellOfOurOwnMakingIntel.Stage.INTEL_DELIVERED, text);
            return true;
        }
        if ("complete".equals(action)) {
            EISHellOfOurOwnMakingIntel intel = EISHellOfOurOwnMakingIntel.get();
            if (intel != null) intel.setStage(EISHellOfOurOwnMakingIntel.Stage.COMPLETED, text);
            return true;
        }
        if ("hartleyInFleet".equals(action)) {
            return isHartleyInPlayerFleet();
        }
        if ("leave".equals(action)) {
            return leave(text);
        }
        if ("join".equals(action)) {
            return join(text);
        }
        if ("showAcheron".equals(action)) {
            if (dialog == null) return false;
            dialog.getVisualPanel().showFleetMemberInfo(createAcheron());
            return true;
        }
        if ("acheronCosts".equals(action)) {
            if (dialog == null) return false;
            setAcheronCosts(dialog);
            return true;
        }
        if ("giveAcheron".equals(action)) {
            return giveAcheron(text);
        }
        return false;
    }

    protected static PersonAPI getHartley() {
        return Global.getSector().getImportantPeople().getPerson(HARTLEY_ID);
    }

    /** True while Hartley is an officer in the player's fleet. IronShellHQ checks this before giving him a combat patrol. */
    public static boolean isHartleyInPlayerFleet() {
        PersonAPI hartley = getHartley();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (hartley == null || fleet == null) return false;
        return fleet.getFleetData().getOfficerData(hartley) != null;
    }

    /**
     * Takes Hartley off any Iron Shell patrol already in space (IronShellHQ makes him the commander and flagship captain
     * of combat patrols). Each such fleet gets a generated Iron Shell commander in his place.
     */
    protected static void releaseHartleyFromOtherFleets() {
        PersonAPI hartley = getHartley();
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (hartley == null) return;
        for (LocationAPI loc : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI fleet : loc.getFleets()) {
                if (fleet == playerFleet || fleet.getFleetData() == null) continue;
                boolean commands = fleet.getCommander() == hartley;
                FleetMemberAPI captained = null;
                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    if (member.getCaptain() == hartley) captained = member;
                }
                if (!commands && captained == null) continue;

                PersonAPI replacement = fleet.getFaction().createRandomPerson();
                replacement.setRankId(Ranks.SPACE_COMMANDER);
                replacement.setPostId(Ranks.POST_FLEET_COMMANDER);
                if (captained != null) captained.setCaptain(replacement);
                if (commands) fleet.setCommander(replacement);
                fleet.forceSync();
            }
        }
    }

    /**
     * Same approach as EISHireOfficerWithShip (Charlotte): the important person himself goes into the officer roster,
     * and his comm directory entry and contact status are left alone.
     */
    protected boolean join(TextPanelAPI text) {
        PersonAPI hartley = getHartley();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (hartley == null || fleet == null) return false;
        if (fleet.getFleetData().getOfficerData(hartley) != null) return true;

        releaseHartleyFromOtherFleets();
        fleet.getFleetData().addOfficer(hartley);
        EISHellOfOurOwnMakingTalkTrigger.addAllIfMissing();
        Global.getSoundPlayer().playUISound(JOIN_SOUND, 1f, 1f);
        if (text != null) AddRemoveCommodity.addOfficerGainText(hartley, text);
        return true;
    }

    public static final String HARTLEY_LEFT_KEY = "$eis_hoom_hartleyLeft";
    // the Acheron goes with him when he leaves
    public static final boolean LEAVE_TAKES_ACHERON = true;

    protected static boolean isQuestAcheron(FleetMemberAPI member) {
        return member.getHullSpec() != null
                && "eis_acheron".equals(member.getHullSpec().getBaseHullId())
                && ACHERON_SHIP_NAME.equals(member.getShipName());
    }

    /**
     * Hartley leaves the player fleet. He is taken off any ship he captains and out of the officer roster; IronShellHQ
     * then gives him combat patrols again. The EIS Acheron leaves with him, unless the player is flying it as the
     * flagship (removing the player's own flagship isn't safe). Does nothing if he isn't in the fleet.
     */
    protected boolean leave(TextPanelAPI text) {
        PersonAPI hartley = getHartley();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (hartley == null || fleet == null) return true;
        if (fleet.getFleetData().getOfficerData(hartley) == null) return true;

        FleetMemberAPI acheron = null;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getCaptain() == hartley) member.setCaptain(null);
            if (LEAVE_TAKES_ACHERON && acheron == null && isQuestAcheron(member) && !member.isFlagship()) acheron = member;
        }
        fleet.getFleetData().removeOfficer(hartley);
        if (text != null) AddRemoveCommodity.addOfficerLossText(hartley, text);
        if (acheron != null) {
            fleet.getFleetData().removeFleetMember(acheron);
            if (text != null) AddRemoveCommodity.addFleetMemberLossText(acheron, text);
        }
        fleet.forceSync();
        Global.getSector().getMemoryWithoutUpdate().set(HARTLEY_LEFT_KEY, true);
        return true;
    }

    protected static FleetMemberAPI createAcheron() {
        FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, Global.getSettings().getVariant(ACHERON_VARIANT).clone());
        ship.setShipName(ACHERON_SHIP_NAME);
        return ship;
    }

    protected boolean giveAcheron(TextPanelAPI text) {
        PersonAPI hartley = getHartley();
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (hartley == null || fleet == null) return false;

        FleetMemberAPI ship = createAcheron();
        // he captains the Acheron, so take him off any ship the player put him on
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.getCaptain() == hartley) member.setCaptain(null);
        }
        if (fleet.getFleetData().getOfficerData(hartley) == null) {
            releaseHartleyFromOtherFleets();
            fleet.getFleetData().addOfficer(hartley);
            EISHellOfOurOwnMakingTalkTrigger.addAllIfMissing();
        }
        ship.setCaptain(hartley);
        fleet.getFleetData().addFleetMember(ship);

        Global.getSoundPlayer().playUISound(JOIN_SOUND, 1f, 1f);
        if (text != null) AddRemoveCommodity.addFleetMemberGainText(ship.getVariant(), text);
        return true;
    }

    /**
     * "Use your connections within the Hegemony." is free, except in survival mode, where it costs half the capital
     * ship story point cost (rounded down, at least 1).
     * "We can manage. If all else fails you can blame me." costs 1 story point, or in survival mode the full capital
     * ship story point cost (at least 1).
     * The capital ship cost is the last entry of ext_recoveryNonOwnSPCost (vanilla 4). Options that aren't on the
     * menu (no Hegemony or Iron Shell commission) are skipped.
     */
    protected void setAcheronCosts(InteractionDialogAPI dialog) {
        boolean survival = Misc.isExtended();
        int capitalCost = getCapitalSPCost();

        if (survival && dialog.getOptionPanel().hasOption(OPT_CONNECTIONS)) {
            SetStoryOption.set(dialog, Math.max(1, capitalCost / 2), OPT_CONNECTIONS, BONUS_XP_CONNECTIONS, "general", null);
        }
        if (dialog.getOptionPanel().hasOption(OPT_MANAGE)) {
            int cost = survival ? Math.max(1, capitalCost) : 1;
            SetStoryOption.set(dialog, cost, OPT_MANAGE, BONUS_XP_MANAGE, "general", null);
        }
    }

    protected static int getCapitalSPCost() {
        try {
            JSONArray costs = Global.getSettings().getJSONArray(EXT_RECOVERY_SP_COST_KEY);
            if (costs != null && costs.length() > 0) return costs.getInt(costs.length() - 1);
        } catch (Exception e) {
            // missing or malformed setting: fall back to vanilla's value
        }
        return DEFAULT_CAPITAL_SP_COST;
    }
}
