package data.scripts.campaign.intel;

import java.awt.Color;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.missions.askonia.TheUsurpers;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

/**
 * Intel for Hartley's Sindrian Diktat mini-quest, "Hell of Our Own Making".
 *
 * Started and advanced from rules.csv through the EISHellOfOurOwnMaking command. The quest state the rules check lives
 * in global memory ($global.eis_hoom_accepted, $global.eis_hoom_intelDelivered); this class only displays it.
 * While gathering intel, this intel sends one update when vanilla The Usurpers reaches DELIVER_NEWS (the player has
 * searched the debris field and found the ANDRADA files), telling the player to return to Hartley.
 */
public class EISHellOfOurOwnMakingIntel extends BaseIntelPlugin {

    public static final String HARTLEY_ID = "eisdarren";
    public static final String MARKET_ID = "eis_chitagupta";
    public static final String FACTION_ID = "ironshell";
    public static final String USURPERS_COMPLETED_KEY = "$sdtu_missionCompleted";
    public static final String USURPERS_REF_KEY = "$sdtu_ref";
    // the debris field with the ANDRADA files has been searched
    public static final String DELIVER_STAGE = "DELIVER_NEWS";
    public static final String HARTLEY_JOINED_KEY = "$eis_hoom_hartleyJoined";

    public static final String UPDATE_INTEL_READY = "intelReady";
    public static final String UPDATE_COMPLETED = "completed";

    public enum Stage {
        GATHER_INTEL,
        INTEL_DELIVERED,
        COMPLETED,
    }

    protected Stage stage = Stage.GATHER_INTEL;
    protected boolean sentIntelReadyUpdate = false;

    public static EISHellOfOurOwnMakingIntel get() {
        IntelInfoPlugin intel = Global.getSector().getIntelManager().getFirstIntel(EISHellOfOurOwnMakingIntel.class);
        return (EISHellOfOurOwnMakingIntel) intel;
    }

    /** Adds the intel if the player doesn't have it yet. Returns the intel either way. */
    public static EISHellOfOurOwnMakingIntel start(TextPanelAPI text) {
        EISHellOfOurOwnMakingIntel intel = get();
        if (intel != null) return intel;
        intel = new EISHellOfOurOwnMakingIntel();
        // already done when accepted: no update later, the bullet shows "return to Hartley" straight away
        intel.sentIntelReadyUpdate = isIntelReady();
        Global.getSector().getIntelManager().addIntel(intel, false, text);
        Global.getSector().addScript(intel);
        return intel;
    }

    public static boolean isUsurpersCompleted() {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(USURPERS_COMPLETED_KEY);
    }

    /**
     * True once vanilla The Usurpers has reached the named stage (TheUsurpers.Stage, e.g. MEET_RAM) or any later one,
     * or has been completed. Reads the running mission's current stage: the global flags the mission's rules set per
     * stage ($global.sdtu_meetHyder and so on) are unset again as it moves on, so they can't tell "already passed".
     */
    public static boolean isUsurpersStageReached(String stageName) {
        TheUsurpers.Stage target;
        try {
            target = TheUsurpers.Stage.valueOf(stageName);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (isUsurpersCompleted()) return true;
        Object ref = Global.getSector().getMemoryWithoutUpdate().get(USURPERS_REF_KEY);
        if (!(ref instanceof TheUsurpers)) return false;
        Object current = ((TheUsurpers) ref).getCurrentStage();
        if (!(current instanceof TheUsurpers.Stage)) return false;
        return ((TheUsurpers.Stage) current).ordinal() >= target.ordinal();
    }

    /** The point where the player can hand Hartley the intel: the ANDRADA files from the debris field. */
    public static boolean isIntelReady() {
        return isUsurpersStageReached(DELIVER_STAGE);
    }

    /** Hartley accepted into the player's fleet in this quest ($global.eis_hoom_hartleyJoined, set by rules.csv). */
    public static boolean isHartleyJoined() {
        return Global.getSector().getMemoryWithoutUpdate().getBoolean(HARTLEY_JOINED_KEY);
    }

    public Stage getStage() {
        return stage;
    }

    /**
     * INTEL_DELIVERED changes silently: the handover and Hartley's offer happen in the same conversation, which ends
     * with COMPLETED. COMPLETED sends the final update and ends the intel.
     */
    public void setStage(Stage stage, TextPanelAPI text) {
        if (this.stage == stage) return;
        this.stage = stage;
        if (stage == Stage.COMPLETED) {
            sendUpdate(UPDATE_COMPLETED, text);
            endAfterDelay();
        }
    }

    protected PersonAPI getHartley() {
        return Global.getSector().getImportantPeople().getPerson(HARTLEY_ID);
    }

    @Override
    protected void advanceImpl(float amount) {
        if (stage == Stage.GATHER_INTEL && !sentIntelReadyUpdate && isIntelReady()) {
            sentIntelReadyUpdate = true;
            sendUpdateIfPlayerHasIntel(UPDATE_INTEL_READY, false);
        }
    }

    @Override
    protected String getName() {
        if (stage == Stage.COMPLETED) return "Hell of Our Own Making - Completed";
        return "Hell of Our Own Making";
    }

    @Override
    protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
        if (stage == Stage.COMPLETED) {
            if (isHartleyJoined()) {
                info.addPara("Hartley has joined your fleet.", tc, initPad);
            } else {
                info.addPara("You have declined Hartley's offer to join your fleet.", tc, initPad);
            }
        } else if (stage == Stage.INTEL_DELIVERED) {
            info.addPara("[todo: hear Hartley's offer]", tc, initPad);
        } else if (isIntelReady()) {
            info.addPara("Report to Hartley with what you found at the Askonian jump point.", tc, initPad);
        } else {
            info.addPara("Gather intel on the Sindrian Diktat", tc, initPad);
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        float opad = 10f;
        PersonAPI hartley = getHartley();
        FactionAPI faction = Global.getSector().getFaction(FACTION_ID);
        if (hartley != null && faction != null) {
            info.addImages(width, 128, opad, opad, hartley.getPortraitSprite(), faction.getCrest());
        }

        info.addPara("Commissioner Hartley of Iron Shell has asked you to gather intel of the current state of the "
                + "Sindrian Diktat. You may run into some loose-lipped Diktat officers around the Askonia System.", opad);

        if (stage == Stage.COMPLETED) {
            if (isHartleyJoined()) {
                info.addPara("Commissioner Hartley of Iron Shell has joined your fleet at his request. He wants to see "
                        + "the end of Andrada and the Sindrian Diktat from a front-row seat within your fleet. He likely "
                        + "will oppose any attempt to preserve the current regime.", opad);
            } else {
                info.addPara("Commissioner Hartley offered to join your fleet in order to fight the Sindrian Diktat. He "
                        + "may still be found on Chitagupta if you want to accept his offer.", opad);
            }
        } else if (stage == Stage.INTEL_DELIVERED) {
            info.addPara("[todo: hear Hartley's offer description]", opad);
        } else if (isIntelReady()) {
            info.addPara("You found some strange intel at a jump point within Askonia. Did Macario set this up, or was "
                    + "he truly careless? Regardless, it may be wise to decrypt the data before returning for your "
                    + "report.", opad);
        }

        addBulletPoints(info, ListInfoMode.IN_DESC);
    }

    @Override
    public String getIcon() {
        PersonAPI hartley = getHartley();
        if (hartley != null) return hartley.getPortraitSprite();
        return Global.getSettings().getSpriteName("intel", "fleet_log");
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add(Tags.INTEL_ACCEPTED);
        return tags;
    }

    @Override
    public FactionAPI getFactionForUIColors() {
        FactionAPI faction = Global.getSector().getFaction(FACTION_ID);
        if (faction != null) return faction;
        return super.getFactionForUIColors();
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        MarketAPI market = Global.getSector().getEconomy().getMarket(MARKET_ID);
        if (market != null) return market.getPrimaryEntity();
        return null;
    }

    @Override
    public String getSortString() {
        return getName();
    }
}
