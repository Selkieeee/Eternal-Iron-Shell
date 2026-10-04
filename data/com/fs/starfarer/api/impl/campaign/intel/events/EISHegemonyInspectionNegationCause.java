package com.fs.starfarer.api.impl.campaign.intel.events;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI.TooltipCreator;
import com.fs.starfarer.api.util.Misc;

// Added to the Hegemony colony-crisis factor; exactly cancels HegemonyAICoresActivityCause while the player is commissioned to Iron Shell.
public class EISHegemonyInspectionNegationCause extends BaseHostileActivityCause2 {

    private static final String IRON_SHELL_ID = "ironshell";

    public EISHegemonyInspectionNegationCause(HostileActivityEventIntel intel) {
        super(intel);
    }

    public static boolean isIronShellOnlyCommission() {
        return IRON_SHELL_ID.equals(Misc.getCommissionFactionId());
    }

    private HegemonyAICoresActivityCause getAICoresCause() {
        BaseHostileActivityFactor hegemony = intel.getActivityOfClass(HegemonyHostileActivityFactor.class);
        if (hegemony == null) return null;
        return (HegemonyAICoresActivityCause) hegemony.getCauseOfClass(HegemonyAICoresActivityCause.class);
    }

    @Override
    public int getProgress() {
        if (!isIronShellOnlyCommission()) return 0;
        HegemonyAICoresActivityCause aiCores = getAICoresCause();
        if (aiCores == null) return 0;
        return -Math.max(0, aiCores.getProgress());
    }

    @Override
    public float getMagnitudeContribution(StarSystemAPI system) {
        if (!isIronShellOnlyCommission()) return 0f;
        HegemonyAICoresActivityCause aiCores = getAICoresCause();
        if (aiCores == null) return 0f;
        return -aiCores.getMagnitudeContribution(system);
    }

    @Override
    public String getDesc() {
        return Global.getSettings().getString("eis_ironshell", "eis_hegInspNegatedDesc");
    }

    @Override
    public TooltipCreator getTooltip() {
        return new BaseFactorTooltip() {
            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                tooltip.addPara(Global.getSettings().getString("eis_ironshell", "eis_hegInspNegatedTooltip"), 0f);
            }
        };
    }
}
