package data.scripts.campaign.intel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.EISHellOfOurOwnMaking;
import com.fs.starfarer.api.util.Misc;

/**
 * Hell of Our Own Making: opens a rules conversation with Hartley while he serves in the player's fleet, one in-game
 * day after a vanilla questline moment (a global memory flag) appears. If Hartley is still an officer in the player
 * fleet when the day is up, it opens a rules dialog on the player fleet with the subclass's initial trigger, as soon as
 * no other dialog, menu or battle is in the way. Runs once; stands down if one of the subclass's stop flags is set.
 * Subclasses: EISHellOfOurOwnMakingWarTalk (both heirs have claimed), EISHellOfOurOwnMakingPostWarTalk (war over).
 */
public abstract class EISHellOfOurOwnMakingTalkTrigger implements EveryFrameScript {

    public static final float DELAY_DAYS = 1f;

    protected boolean startSeen = false;
    protected long startSeenTimestamp = 0L;
    protected boolean done = false;

    /** Global memory flag (without "$global.") that starts the one-day wait. */
    protected abstract String getStartKey();

    /** Global memory flags that make this conversation unnecessary. Includes the "shown" key. */
    protected abstract String[] getStopKeys();

    /** Global memory flag set once the conversation has been opened. */
    protected abstract String getShownKey();

    /** Initial rules trigger of the conversation. */
    protected abstract String getInitialTrigger();

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }

    @Override
    public void advance(float amount) {
        if (done) return;
        MemoryAPI mem = Global.getSector().getMemoryWithoutUpdate();
        for (String key : getStopKeys()) {
            if (mem.getBoolean(key)) {
                done = true;
                return;
            }
        }
        if (!mem.getBoolean(getStartKey())) return;

        if (!startSeen) {
            startSeen = true;
            startSeenTimestamp = Global.getSector().getClock().getTimestamp();
            return;
        }
        if (Global.getSector().getClock().getElapsedDaysSince(startSeenTimestamp) < DELAY_DAYS) return;

        // only while he is in the fleet when the day is up
        if (!EISHellOfOurOwnMaking.isHartleyInPlayerFleet()) {
            done = true;
            return;
        }
        if (!canShowDialog()) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (Misc.showRuleDialog(playerFleet, getInitialTrigger())) {
            mem.set(getShownKey(), true);
            done = true;
        }
    }

    protected boolean canShowDialog() {
        if (Global.getCurrentState() != GameState.CAMPAIGN) return false;
        if (Global.getSector().isInNewGameAdvance() || Global.getSector().isInFastAdvance()) return false;
        CampaignUIAPI ui = Global.getSector().getCampaignUI();
        if (ui == null || ui.isShowingDialog() || ui.isShowingMenu() || ui.getCurrentInteractionDialog() != null) return false;
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null || playerFleet.isInHyperspaceTransition() || playerFleet.getBattle() != null) return false;
        return true;
    }

    /** Adds both conversation triggers if they aren't running yet. Called when Hartley joins the player fleet. */
    public static void addAllIfMissing() {
        if (!Global.getSector().hasScript(EISHellOfOurOwnMakingWarTalk.class)) {
            Global.getSector().addScript(new EISHellOfOurOwnMakingWarTalk());
        }
        if (!Global.getSector().hasScript(EISHellOfOurOwnMakingPostWarTalk.class)) {
            Global.getSector().addScript(new EISHellOfOurOwnMakingPostWarTalk());
        }
    }
}
