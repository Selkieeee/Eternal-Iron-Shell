package data.scripts;

import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.listeners.ColonyCrisesSetupListener;
import com.fs.starfarer.api.impl.campaign.intel.events.BaseHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.EISHegemonyInspectionNegationCause;
import com.fs.starfarer.api.impl.campaign.intel.events.HegemonyHostileActivityFactor;
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel;

// Transient (register every load, never saved). Installs EISHegemonyInspectionNegationCause on the Hegemony crisis factor,
// and averts an already-rolled Hegemony AI inspection while the player is commissioned to Iron Shell.
public class EISHegemonyInspectionGuard extends BaseCampaignEventListener implements ColonyCrisesSetupListener {

    public EISHegemonyInspectionGuard() {
        super(false);
    }

    @Override
    public void finishedAddingCrisisFactors(HostileActivityEventIntel intel) {
        install(intel);
    }

    @Override
    public void reportEconomyTick(int iterIndex) {
        HostileActivityEventIntel intel = HostileActivityEventIntel.get();
        if (intel == null) return;
        install(intel);
        if (EISHegemonyInspectionNegationCause.isIronShellOnlyCommission()) {
            HegemonyHostileActivityFactor.avertInspectionIfNotInProgress();
        }
    }

    public void ensureInstalled() {
        HostileActivityEventIntel intel = HostileActivityEventIntel.get();
        if (intel != null) install(intel);
    }

    private static void install(HostileActivityEventIntel intel) {
        BaseHostileActivityFactor hegemony = intel.getActivityOfClass(HegemonyHostileActivityFactor.class);
        if (hegemony == null) return;
        if (hegemony.getCauseOfClass(EISHegemonyInspectionNegationCause.class) != null) return;
        hegemony.addCause(new EISHegemonyInspectionNegationCause(intel));
    }
}
