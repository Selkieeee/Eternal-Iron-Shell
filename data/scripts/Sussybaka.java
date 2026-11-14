package data.scripts;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RuleBasedDialog;
import com.fs.starfarer.api.impl.campaign.RuleBasedInteractionDialogPluginImpl;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;

// Opens the EIS_Tip dialog shortly after the campaign is back on screen. The waiting logic is what
// Nexerelin's DelayedDialogScreenScript does, kept here so this class works without Nexerelin.
public class Sussybaka implements EveryFrameScript {
    protected static final float DAYS_TO_WAIT = 0.1f;
    protected boolean isDone = false;
    protected float timer = 0;

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        // Don't do anything while in a menu/dialog
        if (Global.getSector().isInNewGameAdvance() || Global.getSector().getCampaignUI().isShowingDialog() || Global.getCurrentState() == GameState.TITLE) {
            return;
        }
        timer += Global.getSector().getClock().convertToDays(amount);
        if (timer < DAYS_TO_WAIT || isDone) return;
        showDialog();
        isDone = true;
    }

    protected void showDialog() {
	if (Global.getSector().getCampaignUI().showInteractionDialog(new RuleBasedInteractionDialogPluginImpl(), Global.getSector().getPlayerFleet())) {
            FireAll.fire(null, Global.getSector().getCampaignUI().getCurrentInteractionDialog(), ((RuleBasedDialog) Global.getSector().getCampaignUI().getCurrentInteractionDialog().getPlugin()).getMemoryMap(), "EIS_Tip");
	}
        Global.getSector().removeTransientScriptsOfClass(Sussybaka.class);
    }
}
