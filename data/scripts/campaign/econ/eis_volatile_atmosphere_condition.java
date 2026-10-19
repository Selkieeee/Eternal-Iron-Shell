package data.scripts.campaign.econ;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.econ.BaseHazardCondition;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

// Volatile-rich Atmosphere: -2 food (Aquaculture), +2 volatiles (Mining), +25% hazard
public class eis_volatile_atmosphere_condition extends BaseHazardCondition {
    private static String foodPenaltyText = Global.getSettings().getString("eis_ironshell", "eis_volatile_atmosphere_condition1");
    private static String volatilesBonusText = Global.getSettings().getString("eis_ironshell", "eis_volatile_atmosphere_condition2");
    private static String hazardIncreaseText = Global.getSettings().getString("eis_ironshell", "eis_volatile_atmosphere_condition3");

    @Override
    public void apply(String id) {
        Industry aquaculture = market.getIndustry(Industries.AQUACULTURE);
        if (aquaculture != null) {
            if (aquaculture.isFunctional()) {
                aquaculture.supply(id + "_0", Commodities.FOOD, -2, condition.getName());
            }
        }
        Industry mining = market.getIndustry(Industries.MINING);
        if (mining != null) {
            if (mining.isFunctional()) {
                mining.supply(id + "_1", Commodities.VOLATILES, 2, condition.getName());
            }
        }
        market.getHazard().modifyFlat(id, 0.25f, condition.getName());
    }

    @Override
    public void unapply(String id) {
        Industry aquaculture = market.getIndustry(Industries.AQUACULTURE);
        if (aquaculture != null) {
            aquaculture.getSupply(Commodities.FOOD).getQuantity().unmodifyFlat(id + "_0");
        }
        Industry mining = market.getIndustry(Industries.MINING);
        if (mining != null) {
            mining.getSupply(Commodities.VOLATILES).getQuantity().unmodifyFlat(id + "_1");
        }
        market.getHazard().unmodifyFlat(id);
    }

    @Override
    protected void createTooltipAfterDescription(TooltipMakerAPI tooltip, boolean expanded) {
        float pad = 10f;
        tooltip.addPara(foodPenaltyText, pad, Misc.getHighlightColor(), "-2");
        tooltip.addPara(volatilesBonusText, pad, Misc.getHighlightColor(), "+2");
        tooltip.addPara(hazardIncreaseText, pad, Misc.getHighlightColor(), "+25%");
    }
}
