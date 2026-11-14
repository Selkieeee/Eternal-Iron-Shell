package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/**
 * The only door to Nexerelin. Nexerelin is optional, so nothing outside eis_nexImpl (and the classes only
 * eis_nexImpl touches: eis_NexMusicDirectory, MarketshipChange, the eis_ironshell save stub) may import
 * exerelin.* or a Nex_* rule command.
 *
 * This class must never reference a Nexerelin class itself, not even in a method body: rules.csv instantiates
 * every rule command while it loads, and a class that can't link without Nexerelin would stop the game starting.
 * Every method here checks HAVE_NEX and only then calls eis_nexImpl, so eis_nexImpl is never loaded without Nexerelin.
 * Each method documents what Iron Shell does when Nexerelin is absent.
 */
public class eis_nex {
    public static final boolean HAVE_NEX = Global.getSettings().getModManager().isModEnabled("nexerelin");

    private static final String IRON = "ironshell";
    private static final String MOD_ID = "selkie_eis";
    private static final String FACTION_CONFIG = "data/config/exerelinFactionConfig/ironshell.json";

    /** Without Nexerelin there is no random sector, so the core worlds (and Naraka) always exist. */
    public static boolean isCorvusMode() {
        return !HAVE_NEX || eis_nexImpl.isCorvusMode();
    }

    /** Nexerelin's "hard mode" start option. Never on without Nexerelin. */
    public static boolean isHardMode() {
        return HAVE_NEX && eis_nexImpl.isHardMode();
    }

    /** Nexerelin's random starting relationships option. Never on without Nexerelin. */
    public static boolean isRandomRelationships() {
        return HAVE_NEX && eis_nexImpl.isRandomRelationships();
    }

    /** Faction picked in Nexerelin's new game dialog; null without Nexerelin (there is no faction start). */
    public static String getPlayerFactionIdNGC() {
        return HAVE_NEX ? eis_nexImpl.getPlayerFactionIdNGC() : null;
    }

    /** Faction the player is aligned with. Without Nexerelin: the commissioning faction, else "player". */
    public static String getPlayerFactionId() {
        if (HAVE_NEX) return eis_nexImpl.getPlayerFactionId();
        String commission = Misc.getCommissionFactionId();
        return commission != null ? commission : "player";
    }

    /** True if the campaign was started with Nexerelin's "super ship" starting fleet. */
    public static boolean isSuperStart() {
        return HAVE_NEX && eis_nexImpl.isSuperStart();
    }

    /** Adds a mission-unlocked fleet to Iron Shell's Nexerelin super ship starts (once). No-op without Nexerelin. */
    public static void unlockSuperStartFleet(String... variants) {
        if (HAVE_NEX) eis_nexImpl.unlockSuperStartFleet(variants);
    }

    /** True if the market has a Nexerelin rebellion whose rebels are the Hegemony or Iron Shell. */
    public static boolean isLoyalistRebellion(MarketAPI market) {
        return HAVE_NEX && eis_nexImpl.isLoyalistRebellion(market);
    }

    /**
     * True if the player counts as an ally of the Hegemony and Iron Shell (a commission is checked separately by callers).
     * See playerIsAllyOf.
     */
    public static boolean playerIsAlly() {
        return playerIsAllyOf(IRON);
    }

    /**
     * True if the player counts as an ally of the given faction.
     * With Nexerelin, when that faction is in an alliance: the player is in the same alliance.
     * Otherwise (no Nexerelin, or the faction is in no alliance, e.g. the Greater Hegemony was never created or
     * has dissolved) standing takes its place: Cooperative (75 or better) with both the Hegemony and Iron Shell.
     * A faction without an alliance never passes just because the player has none either.
     */
    public static boolean playerIsAllyOf(String factionId) {
        if (HAVE_NEX && eis_nexImpl.hasAlliance(factionId)) return eis_nexImpl.playerSharesAlliance(factionId);
        return Global.getSector().getFaction(IRON).isAtWorst(Factions.PLAYER, RepLevel.COOPERATIVE)
                && Global.getSector().getFaction(Factions.HEGEMONY).isAtWorst(Factions.PLAYER, RepLevel.COOPERATIVE);
    }

    /**
     * True if the Hegemony and Iron Shell are in the same Nexerelin alliance. Without Nexerelin there are no
     * alliances, so the two count as allied whenever the "Greater Hegemony" setting is on.
     */
    public static boolean hegemonyAndIronShellAllied() {
        if (HAVE_NEX) return eis_nexImpl.sameAlliance(IRON, "hegemony");
        return eis_modPlugin.getEISBooleanSetting("GreaterHegemony", true);
    }

    /** Creates the Greater Hegemony alliance. No-op without Nexerelin. */
    public static void createGreaterHegemony(boolean permanentMembers) {
        if (HAVE_NEX) eis_nexImpl.createGreaterHegemony(permanentMembers);
    }

    /** Creates the Greater Hegemony alliance if neither faction is in one. No-op without Nexerelin. */
    public static void createGreaterHegemonyIfNone() {
        if (HAVE_NEX) eis_nexImpl.createGreaterHegemonyIfNone();
    }

    /** Makes both factions permanent members of Iron Shell's alliance. No-op without Nexerelin. */
    public static void addGreaterHegemonyPermaMembers() {
        if (HAVE_NEX) eis_nexImpl.addGreaterHegemonyPermaMembers();
    }

    /**
     * Iron Shell's starting relationships. With Nexerelin they come from its faction config (plus the minimum
     * relationship with the Hegemony and the faction-start adjustment). Without it, the same startRelationships
     * table is read straight from Iron Shell's own copy of that config file, so both setups start out the same.
     */
    public static void applyStartRelationships(FactionAPI iron) {
        if (HAVE_NEX) {
            eis_nexImpl.applyStartRelationships(iron);
            return;
        }
        try {
            JSONObject relationships = Global.getSettings().loadJSON(FACTION_CONFIG, MOD_ID).optJSONObject("startRelationships");
            if (relationships == null) return;
            // Most of the table is other mods' factions, so walk the factions that exist in this game instead.
            for (FactionAPI faction : Global.getSector().getAllFactions()) {
                String factionId = faction.getId();
                if (IRON.equals(factionId) || !relationships.has(factionId)) continue;
                iron.setRelationship(factionId, (float) relationships.getDouble(factionId));
            }
        } catch (Exception e) {
            Global.getLogger(eis_nex.class).warn("Could not read Iron Shell's starting relationships from " + FACTION_CONFIG, e);
        }
    }

    /** Hands captured markets back between the Hegemony and Iron Shell. Nexerelin invasions only. */
    public static void registerMarketTransferListener() {
        if (HAVE_NEX) eis_nexImpl.registerMarketTransferListener();
    }

    /** Nexerelin market transfer. Returns false (nothing happens) without Nexerelin. */
    public static boolean transferMarket(MarketAPI market, FactionAPI newOwner, FactionAPI oldOwner) {
        if (!HAVE_NEX) return false;
        eis_nexImpl.transferMarket(market, newOwner, oldOwner);
        return true;
    }

    /** Nexerelin diplomacy event. No-op without Nexerelin. */
    public static void createDiplomacyEvent(FactionAPI faction, FactionAPI other, String eventId) {
        if (HAVE_NEX) eis_nexImpl.createDiplomacyEvent(faction, other, eventId);
    }

    /** True if the player can't take another Nexerelin agent. Without Nexerelin there are no agents: always true. */
    public static boolean agentsAtCap() {
        return !HAVE_NEX || eis_nexImpl.agentsAtCap();
    }

    public static int getAgentSalary(int level) {
        return HAVE_NEX ? eis_nexImpl.getAgentSalary(level) : 0;
    }

    public static void addAgent(int level, String specialization, InteractionDialogAPI dialog) {
        if (HAVE_NEX) eis_nexImpl.addAgent(level, specialization, dialog);
    }

    /** True if an Iron Shell conquest mission posted at the market was completed. Nexerelin only. */
    public static boolean conquestCompleted(String marketId) {
        return HAVE_NEX && eis_nexImpl.conquestCompleted(marketId);
    }

    /** True once Iron Shell's Nexerelin colony expedition has taken Hanan Pacha. */
    public static boolean hananPachaColonized() {
        return HAVE_NEX && eis_nexImpl.hananPachaColonized();
    }

    /** Gives the player governorship of the market through Nexerelin. No-op without Nexerelin. */
    public static void buyColony(MarketAPI market, InteractionDialogAPI dialog) {
        if (HAVE_NEX) eis_nexImpl.buyColony(market, dialog);
    }

    /** Sends an Iron Shell colony expedition (unowned target) or invasion (owned target). Nexerelin only. */
    public static void colonizeOrInvade(String entityId, InteractionDialogAPI dialog) {
        if (HAVE_NEX) eis_nexImpl.colonizeOrInvade(entityId, dialog);
    }

    /** Declares war on the market's owner and gives the player a conquest mission. Nexerelin only. */
    public static void issueInvasion(String entityId, float days, InteractionDialogAPI dialog) {
        if (HAVE_NEX) eis_nexImpl.issueInvasion(entityId, days, dialog);
    }

    /** The player faction music picker hung off Nexerelin's market menu. */
    public static boolean musicDirectory(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
        return HAVE_NEX && eis_nexImpl.musicDirectory(ruleId, dialog, params, memoryMap);
    }
}
