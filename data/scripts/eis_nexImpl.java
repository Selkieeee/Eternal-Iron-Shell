package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.intel.BaseMissionIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.Nex_BuyColony;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.AllianceManager;
import exerelin.campaign.CovertOpsManager;
import exerelin.campaign.DiplomacyManager;
import exerelin.campaign.ExerelinSetupData;
import exerelin.campaign.PlayerFactionStore;
import exerelin.campaign.SectorManager;
import exerelin.campaign.alliances.Alliance;
import exerelin.campaign.fleets.InvasionFleetManager;
import exerelin.campaign.intel.BuyColonyIntel;
import exerelin.campaign.intel.agents.AgentIntel;
import exerelin.campaign.intel.colony.ColonyExpeditionIntel;
import exerelin.campaign.intel.invasion.InvasionIntel;
import exerelin.campaign.intel.missions.ConquestMissionIntel;
import exerelin.campaign.intel.rebellion.RebellionIntel;
import exerelin.utilities.NexConfig;
import exerelin.utilities.NexFactionConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything Iron Shell does through Nexerelin. Only eis_nex may call this class, and only after checking
 * eis_nex.HAVE_NEX: loading it without Nexerelin installed throws NoClassDefFoundError.
 */
class eis_nexImpl {
    private static final String IRON = "ironshell";

    static boolean isCorvusMode() {
        return SectorManager.getCorvusMode();
    }

    static boolean isHardMode() {
        return SectorManager.getManager().isHardMode();
    }

    static boolean isRandomRelationships() {
        return DiplomacyManager.isRandomFactionRelationships();
    }

    static String getPlayerFactionIdNGC() {
        return PlayerFactionStore.getPlayerFactionIdNGC();
    }

    static String getPlayerFactionId() {
        return PlayerFactionStore.getPlayerFactionId();
    }

    static boolean isSuperStart() {
        return NexFactionConfig.StartFleetType.SUPER == ExerelinSetupData.getInstance().startFleetType;
    }

    // Adds the fleet unless a super ship start already leads with the same variant.
    static void unlockSuperStartFleet(String... variants) {
        NexFactionConfig.StartFleetSet starts = NexConfig.getFactionConfig(IRON).getStartFleetSet(NexFactionConfig.StartFleetType.SUPER.name());
        int count = starts.getNumFleets();
        if (count <= 0) return;
        for (int i = 1; i <= count; i++) {
            if (variants[0].equals(starts.getFleet(i).get(0))) return;
        }
        starts.addFleet(new ArrayList<String>(Arrays.asList(variants)));
    }

    static boolean isLoyalistRebellion(MarketAPI market) {
        if (!market.hasCondition("nex_rebellion_condition")) return false;
        String rebels = RebellionIntel.getOngoingEvent(market).getRebelFaction().getId();
        return "hegemony".equals(rebels) || IRON.equals(rebels);
    }

    static boolean hasAlliance(String factionId) {
        return AllianceManager.getFactionAlliance(factionId) != null;
    }

    static boolean playerSharesAlliance(String factionId) {
        return AllianceManager.getPlayerAlliance(true) == AllianceManager.getFactionAlliance(factionId);
    }

    static boolean sameAlliance(String factionId, String otherId) {
        return AllianceManager.getFactionAlliance(factionId) == AllianceManager.getFactionAlliance(otherId);
    }

    static void createGreaterHegemony(boolean permanentMembers) {
        Alliance hegemonyAlliance = AllianceManager.createAlliance(IRON, Factions.HEGEMONY, AllianceManager.getBestAlignment(IRON, Factions.HEGEMONY));
        hegemonyAlliance.setName(Global.getSettings().getString("eis_ironshell", "eis_greaterhegemony"));
        if (permanentMembers) {
            hegemonyAlliance.addPermaMember(IRON);
            hegemonyAlliance.addPermaMember("hegemony");
        }
    }

    static void createGreaterHegemonyIfNone() {
        if (AllianceManager.getFactionAlliance(IRON) == null && AllianceManager.getFactionAlliance("hegemony") == null) {
            createGreaterHegemony(false);
        }
    }

    static void addGreaterHegemonyPermaMembers() {
        Alliance hegemonyAlliance = AllianceManager.getFactionAlliance(IRON);
        if (hegemonyAlliance == null) return;
        hegemonyAlliance.addPermaMember(IRON);
        hegemonyAlliance.addPermaMember("hegemony");
    }

    static void applyStartRelationships(FactionAPI iron) {
        NexFactionConfig factionConfig = NexConfig.getFactionConfig(IRON);
        if (!DiplomacyManager.isRandomFactionRelationships()) {
            factionConfig.minRelationships.clear();
            factionConfig.minRelationships.put("hegemony", eis_modPlugin.HEGEMONY_MIN_REL);
        }
        for (Map.Entry<String, Float> entry : factionConfig.startRelationships.entrySet()) {
            if (IRON.equals(entry.getKey())) continue;
            iron.setRelationship(entry.getKey(), entry.getValue());
        }
        if (IRON.equals(PlayerFactionStore.getPlayerFactionIdNGC()) || "hegemony".equals(PlayerFactionStore.getPlayerFactionIdNGC())) {
            iron.setRelationship(Factions.HEGEMONY, 0.75f); //Prevents you from instantly gaining 1 story point repping up to 100...
        }
    }

    static void registerMarketTransferListener() {
        Global.getSector().getListenerManager().addListener(new MarketshipChange(), true);
    }

    static void transferMarket(MarketAPI market, FactionAPI newOwner, FactionAPI oldOwner) {
        SectorManager.transferMarket(market, newOwner, oldOwner, false, false, null, 0);
    }

    static void createDiplomacyEvent(FactionAPI faction, FactionAPI other, String eventId) {
        DiplomacyManager.createDiplomacyEvent(faction, other, eventId, null);
    }

    static boolean agentsAtCap() {
        return CovertOpsManager.getManager().getAgents().size() >= CovertOpsManager.getManager().getMaxAgents().getModifiedValue();
    }

    static int getAgentSalary(int level) {
        return AgentIntel.getSalary(level);
    }

    static void addAgent(int level, String specialization, InteractionDialogAPI dialog) {
        PersonAPI agent = Global.getSector().getFaction(IRON).createRandomPerson();
        agent.setRankId(Ranks.AGENT);
        agent.setPostId(Ranks.POST_AGENT);
        AgentIntel intel = new AgentIntel(agent, Global.getSector().getPlayerFaction(), level);
        intel.addSpecialization(AgentIntel.Specialization.valueOf(specialization.toUpperCase(Locale.ENGLISH)));
        intel.setMarket(dialog.getInteractionTarget().getMarket());
        intel.init();
        Global.getSector().getIntelManager().addIntelToTextPanel(intel, dialog.getTextPanel());
    }

    static boolean conquestCompleted(String marketId) {
        List<IntelInfoPlugin> intels = Global.getSector().getIntelManager().getIntel(ConquestMissionIntel.class);
        for (IntelInfoPlugin intel : intels) {
            ConquestMissionIntel conquestIntel = (ConquestMissionIntel) intel;
            if (conquestIntel.getPostingLocation() != null && conquestIntel.getFactionForUIColors() != null) {
                if (marketId.equals(conquestIntel.getPostingLocation().getId())
                        && IRON.equals(conquestIntel.getFactionForUIColors().getId())
                        && conquestIntel.isCompleted())
                    return true;
            }
        }
        return false;
    }

    static boolean hananPachaColonized() {
        List<IntelInfoPlugin> intels = Global.getSector().getIntelManager().getIntel(ColonyExpeditionIntel.class);
        for (IntelInfoPlugin intel : intels) {
            ColonyExpeditionIntel colonyIntel = (ColonyExpeditionIntel) intel;
            if ((colonyIntel.getTarget().getId().equals("market_hanan_pacha") && colonyIntel.getTarget().getFactionId().equals(IRON))
                    || (Global.getSector().getEntityById("hanan_pacha").getMarket().getFactionId().equals(IRON)))
                return true;
        }
        return false;
    }

    static void buyColony(MarketAPI market, InteractionDialogAPI dialog) {
        int value = Nex_BuyColony.getValue(market, false, true).getModifiedInt();
        if (value < 0) value = 0;
        if (value > 0) {
            dialog.getTextPanel().addParagraph(Global.getSettings().getString("eis_ironshell", "EISGained") + Misc.getWithDGS(value) + com.fs.starfarer.api.impl.campaign.ids.Strings.C + "", Misc.getPositiveHighlightColor());
            dialog.getTextPanel().setFontSmallInsignia();
            dialog.getTextPanel().highlightInLastPara(Misc.getHighlightColor(), Misc.getWithDGS(value) + com.fs.starfarer.api.impl.campaign.ids.Strings.C);
            Global.getSector().getPlayerFleet().getCargo().getCredits().add(value);
            dialog.getTextPanel().setFontSmallInsignia();
        }
        Nex_BuyColony.setColonyPlayerOwned(market, true, dialog);
        BuyColonyIntel intel = new BuyColonyIntel(market.getFactionId(), market);
        intel.init();
        if (dialog != null) Global.getSector().getIntelManager().addIntelToTextPanel(intel, dialog.getTextPanel());
    }

    static void colonizeOrInvade(String entityId, InteractionDialogAPI dialog) {
        if (Global.getSector().getEntityById(entityId) == null) return;
        MarketAPI target = Global.getSector().getEntityById(entityId).getMarket();
        FactionAPI iron = Global.getSector().getFaction(IRON);
        MarketAPI source = Global.getSector().getEconomy().getMarket("eis_chitagupta");
        if (target.getFactionId().equals(Factions.NEUTRAL)) {
            ColonyExpeditionIntel intel = new ColonyExpeditionIntel(iron, source, target, 500, 1f);
            intel.init();
            Global.getSector().getIntelManager().addIntelToTextPanel(intel, dialog.getTextPanel());
        } else if (iron.getRelationship(target.getFactionId()) >= -0.5f || !target.getFactionId().equals(Factions.HEGEMONY)) {
            iron.setRelationship(target.getFactionId(), -0.75f);
            float fp = InvasionFleetManager.getWantedFleetSize(iron, target, 0.2f, false);
            fp *= 1.1f;
            InvasionIntel intel2 = new InvasionIntel(iron, source, target, fp, 1);
            intel2.init();
            Global.getSector().getIntelManager().addIntelToTextPanel(intel2, dialog.getTextPanel());
        }
    }

    static void issueInvasion(String entityId, float days, InteractionDialogAPI dialog) {
        if (Global.getSector().getEntityById(entityId) == null) return;
        MarketAPI target = Global.getSector().getEntityById(entityId).getMarket();
        if (target.getFactionId().equals(IRON)) return;
        DiplomacyManager.createDiplomacyEvent(Global.getSector().getFaction(IRON), target.getFaction(), "declare_war", null);
        DiplomacyManager.createDiplomacyEvent(Global.getSector().getFaction("hegemony"), target.getFaction(), "declare_war", null);
        ConquestMissionIntel intel = new ConquestMissionIntel(target, Global.getSector().getFaction(IRON), days);
        Global.getSector().addScript(intel);
        intel.init();
        intel.setMissionState(BaseMissionIntel.MissionState.ACCEPTED);
        Global.getSector().getIntelManager().addIntelToTextPanel(intel, dialog.getTextPanel());
    }

    static boolean musicDirectory(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        return eis_NexMusicDirectory.execute(ruleId, dialog, params, memoryMap);
    }
}
