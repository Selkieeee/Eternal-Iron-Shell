package com.fs.starfarer.api.impl.campaign.econ.impl;

//import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SpecialItemData;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.InstallableIndustryItemPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class eis_SpecialItemEffectsRepo {
    
    public static void addItemEffectsToVanillaRepo() {
        ItemEffectsRepo.ITEM_EFFECTS.putAll(ITEM_EFFECTS);
    }

    public static Map<String, InstallableItemEffect> ITEM_EFFECTS = new HashMap<String, InstallableItemEffect>() {
        {put("eis_bbp", new BaseInstallableItemEffect("eis_bbp") {
            @Override
            public void apply(final Industry industry) {
                if (industry.getMarket().hasCondition("eis_aquaculture"))  industry.getMarket().removeCondition("eis_aquaculture");
            }
            @Override
            public void unapply(final Industry industry) {
                if (!industry.getMarket().hasCondition("eis_aquaculture")) industry.getMarket().addCondition("eis_aquaculture");
            }                  
            @Override
            protected void addItemDescriptionImpl(final Industry industry, final TooltipMakerAPI text, final SpecialItemData data, final InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode, final String pre, final float pad) {
                if (industry != null) {if (industry.getMarket() != null) {if ("player".equals(industry.getMarket().getFactionId()) && industry.getSpecialItem() != null) {
                        text.addPara(pre + "%s.",
                        pad, Misc.getNegativeHighlightColor(), 
                        "" + Global.getSettings().getString("eis_ironshell", "eis_bbp_warning"));} else text.addPara(pre + Global.getSettings().getString("eis_ironshell", "eis_bbp"),
                        pad, Misc.getHighlightColor(), 
                        "" + (int)200 + "%");} else text.addPara(pre + Global.getSettings().getString("eis_ironshell", "eis_bbp"),
                        pad, Misc.getHighlightColor(), 
                        "" + (int)200 + "%");}    else text.addPara(pre + Global.getSettings().getString("eis_ironshell", "eis_bbp"),
                        pad, Misc.getHighlightColor(), 
                        "" + (int)200 + "%");
            }});
        // Atmospheric Regulator Core (Aquaculture, needs Volatile-rich Atmosphere): cancels that condition's -2 food and
        // +25% hazard, and adds +2 volatiles at Mining on top of the condition's +2
        put("eis_regulator_core", new BaseInstallableItemEffect("eis_regulator_core") {
            private final String CONDITION_ID = "eis_volatile_atmosphere";
            private final String requirement = Global.getSettings().getString("eis_ironshell", "eis_regulator_core_req");

            @Override
            public void apply(final Industry industry) {
                if (industry instanceof BaseIndustry) {
                    BaseIndustry aquaculture = (BaseIndustry) industry;
                    MarketAPI market = industry.getMarket();
                    String name = spec.getName();
                    // The condition only takes food while Aquaculture is functional, so only give it back then
                    aquaculture.supply(spec.getId(), Commodities.FOOD, aquaculture.isFunctional() ? 2 : 0, name);
                    market.getHazard().modifyFlat(spec.getId(), -0.25f, name);
                    Industry mining = market.getIndustry(Industries.MINING);
                    if (mining instanceof BaseIndustry) {
                        ((BaseIndustry) mining).supply(spec.getId(), Commodities.VOLATILES, mining.isFunctional() ? 2 : 0, name);
                    }
                }
            }
            @Override
            public void unapply(final Industry industry) {
                if (industry instanceof BaseIndustry) {
                    ((BaseIndustry) industry).supply(spec.getId(), Commodities.FOOD, 0, null);
                    MarketAPI market = industry.getMarket();
                    market.getHazard().unmodifyFlat(spec.getId());
                    Industry mining = market.getIndustry(Industries.MINING);
                    if (mining instanceof BaseIndustry) {
                        ((BaseIndustry) mining).supply(spec.getId(), Commodities.VOLATILES, 0, null);
                    }
                }
            }
            @Override
            protected void addItemDescriptionImpl(final Industry industry, final TooltipMakerAPI text, final SpecialItemData data, final InstallableIndustryItemPlugin.InstallableItemDescriptionMode mode, final String pre, final float pad) {
                text.addPara(pre + Global.getSettings().getString("eis_ironshell", "eis_regulator_core"),
                        pad, Misc.getHighlightColor(), "" + 2);
            }
            @Override
            public List<String> getRequirements(final Industry industry) {
                List<String> reqs = super.getRequirements(industry);
                reqs.add(requirement);
                return reqs;
            }
            @Override
            public List<String> getUnmetRequirements(final Industry industry, final boolean checkSurveyed) {
                List<String> unmet = super.getUnmetRequirements(industry, checkSurveyed);
                if (industry != null && !industry.getMarket().hasCondition(CONDITION_ID)) {
                    unmet.add(requirement);
                }
                return unmet;
            }
            @Override
            public Set<String> getConditionsRelatedToRequirements(final Industry industry) {
                Set<String> conditions = super.getConditionsRelatedToRequirements(industry);
                conditions.add(CONDITION_ID);
                return conditions;
            }});
        }
    };
	
	
}







