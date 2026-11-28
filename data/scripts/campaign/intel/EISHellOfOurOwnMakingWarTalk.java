package data.scripts.campaign.intel;

/**
 * Hartley's conversation about the Askonia civil war while he serves in the player's fleet.
 *
 * Starts one in-game day after both Andrada heirs have made their claim: vanilla sets $global.sdse_achillesDeclared
 * when Achilles declares (after Hector) and starts the standoff in the same conversation. Initial trigger
 * EISHoomWarTalkStart. Stands down if the player already settled the matter with him ($global.eis_hoom_warResolved)
 * or the war is already over.
 */
public class EISHellOfOurOwnMakingWarTalk extends EISHellOfOurOwnMakingTalkTrigger {

    public static final String WAR_STARTED_KEY = "$sdse_achillesDeclared";
    public static final String WAR_OVER_KEY = "$askoniaStandoffCompleted";
    public static final String SHOWN_KEY = "$eis_hoom_warTalkShown";
    public static final String RESOLVED_KEY = "$eis_hoom_warResolved";
    public static final String INITIAL_TRIGGER = "EISHoomWarTalkStart";

    @Override
    protected String getStartKey() {
        return WAR_STARTED_KEY;
    }

    @Override
    protected String[] getStopKeys() {
        return new String[] {SHOWN_KEY, RESOLVED_KEY, WAR_OVER_KEY};
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
