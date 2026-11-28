package data.scripts.campaign.intel;

/**
 * Hartley's conversation after the Askonia civil war, while he serves in the player's fleet.
 *
 * Starts one in-game day after vanilla sets $global.askoniaStandoffCompleted (SDResolution, when the standoff ends).
 * Initial trigger EISHoomPostWarTalkStart. Stands down if the player already settled the matter with him
 * ($global.eis_hoom_postWarResolved).
 */
public class EISHellOfOurOwnMakingPostWarTalk extends EISHellOfOurOwnMakingTalkTrigger {

    public static final String WAR_OVER_KEY = "$askoniaStandoffCompleted";
    public static final String SHOWN_KEY = "$eis_hoom_postWarTalkShown";
    public static final String RESOLVED_KEY = "$eis_hoom_postWarResolved";
    public static final String INITIAL_TRIGGER = "EISHoomPostWarTalkStart";

    @Override
    protected String getStartKey() {
        return WAR_OVER_KEY;
    }

    @Override
    protected String[] getStopKeys() {
        return new String[] {SHOWN_KEY, RESOLVED_KEY};
    }

    @Override
    protected String getShownKey() {
        return SHOWN_KEY;
    }

    @Override
    protected String getInitialTrigger() {
        return INITIAL_TRIGGER;
    }
}
