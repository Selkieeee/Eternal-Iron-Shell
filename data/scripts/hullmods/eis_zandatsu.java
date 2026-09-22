package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

public class eis_zandatsu extends BaseHullMod {
    
    private static final float ROF_BONUS = 1.5f;
    private static final float FLUX_REDUCTION = .33f;
    private static final float RAF_ROF_BUFF = 50f;
    private static final float RAF_FLUX_BUFF = 25f;
    private static final float LUNGE_AI_ASSIST_TIME = 1f;
    private static final float PARRY_RADIUS = 300f;
    private static final float PARRY_SHIELD_EFF_CHANGE = 30f;
    //private static final float PARRY_ROF_BUFF = 50f;
    //private static final float PARRY_FLUX_BUFF = 25f;
    private static final float PARRY_BUFF_DURATION = 12f;
    //private static final float fluxStatsMult = 1.15f;
    //private static final float mobilityBonusPercent = 15f;
    //private static final float durabilityBonusFlat = 100f;
    //private static final float systemUsesBonus = 2f;
    
    private static String RAFIcon = "graphics/icons/hullsys/ammo_feeder.png";
    private static String RAFTitle = Global.getSettings().getString("eis_ironshell", "eis_RAFTitle");
    private static String RAFText1 = Global.getSettings().getString("eis_ironshell", "eis_RAFText1");
    private static String RAFText2 = Global.getSettings().getString("eis_ironshell", "eis_RAFText2");
    //private static String RAFText3 = Global.getSettings().getString("eis_ironshell", "eis_RAFText3");
    private static String RAFText4 = Global.getSettings().getString("eis_ironshell", "eis_RAFText4");
    
    private static String LungeIcon = Global.getSettings().getSpriteName("misc", "eis_zandatsulunge");
    private static String LungeIcon2 = Global.getSettings().getSpriteName("misc", "eis_zandatsulunge2");
    private static String LungeTitle = Global.getSettings().getString("eis_ironshell", "eis_lungeTitle");
    private static String LungeText1 = Global.getSettings().getString("eis_ironshell", "eis_lungeText1"); 
    private static String LungeText2 = Global.getSettings().getString("eis_ironshell", "eis_lungeText2"); 
    private static String LungeText3 = Global.getSettings().getString("eis_ironshell", "eis_lungeText3");
    private static String LungeText4 = Global.getSettings().getString("eis_ironshell", "eis_lungeText4");
    
    private static String ParryIcon = "graphics/icons/hullsys/fortress_shield.png";
    private static String ParryTitle = Global.getSettings().getString("eis_ironshell", "eis_parryTitle");
    private static String ParryText1 = Global.getSettings().getString("eis_ironshell", "eis_parryText1");
    private static String ParryText2 = Global.getSettings().getString("eis_ironshell", "eis_parryText2");
    private static String ParryText3b = Global.getSettings().getString("eis_ironshell", "eis_parryText3b");
    //public static String ParryText3 = Global.getSettings().getString("eis_ironshell", "eis_parryText3");
    //private static String ParryText4 = Global.getSettings().getString("eis_ironshell", "eis_parryText4");
    
    //private static String CFBIcon = "graphics/icons/hullsys/interdictor_array.png";
    private static final String BULLET = "\u2022";
    //private static String zandatsuTitle = Global.getSettings().getString("eis_ironshell", "eis_zandatsuTitle");
    //private static String zandatsuText1 = Global.getSettings().getString("eis_ironshell", "eis_zandatsuText1");
    //private static String zandatsuText4 = Global.getSettings().getString("eis_ironshell", "eis_zandatsuText4");
    //private static String zandatsuText2 = Global.getSettings().getString("eis_ironshell", "eis_zandatsuText2");
    //private static String zandatsuText3 = Global.getSettings().getString("eis_ironshell", "eis_zandatsuText3");
    
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 50f;
        float PAD = 10f;
        Color YELLOW = new Color(241, 199, 0);
        
        TooltipMakerAPI rapidAmmoFeeder = tooltip.beginImageWithText(RAFIcon, HEIGHT);
        rapidAmmoFeeder.addPara(RAFTitle, 0f, YELLOW, RAFTitle);
        rapidAmmoFeeder.addPara(RAFText1, 0f, Misc.getPositiveHighlightColor(), BULLET, Math.round(RAF_ROF_BUFF)+"%");
        rapidAmmoFeeder.addPara(RAFText2, 0f, Misc.getPositiveHighlightColor(), BULLET, Math.round(RAF_FLUX_BUFF)+"%");
        rapidAmmoFeeder.addPara(RAFText4, 0f, Misc.getPositiveHighlightColor(), BULLET);
        tooltip.addImageWithText(PAD);

        if (hullSize == ShipAPI.HullSize.FRIGATE) {
        TooltipMakerAPI lunge = tooltip.beginImageWithText(LungeIcon, HEIGHT);
        lunge.addPara(LungeTitle, 0f, YELLOW, LungeTitle);
        lunge.addPara(LungeText1, 0f);
        lunge.addPara(LungeText2, 0f, Misc.getPositiveHighlightColor(), BULLET);
        lunge.addPara(LungeText3, 0f, Misc.getPositiveHighlightColor(), BULLET, Integer.toString(Math.round(LUNGE_AI_ASSIST_TIME)));
        lunge.addPara(LungeText4, 0f, Misc.getPositiveHighlightColor(), BULLET, Integer.toString(Math.round(RAF_ROF_BUFF))+"%", Integer.toString(Math.round(LUNGE_AI_ASSIST_TIME)));
        tooltip.addImageWithText(PAD);
        } else {
        TooltipMakerAPI lunge = tooltip.beginImageWithText(LungeIcon2, HEIGHT);
        lunge.addPara(LungeTitle, 0f, YELLOW, LungeTitle);
        lunge.addPara(LungeText1, 0f);
        lunge.addPara(LungeText2, 0f, Misc.getPositiveHighlightColor(), BULLET);
        lunge.addPara(LungeText3, 0f, Misc.getPositiveHighlightColor(), BULLET, Integer.toString(Math.round(LUNGE_AI_ASSIST_TIME)));
        tooltip.addImageWithText(PAD);}

        TooltipMakerAPI parry = tooltip.beginImageWithText(ParryIcon, HEIGHT);
        parry.addPara(ParryTitle, 0f, YELLOW, ParryTitle);
        parry.addPara(ParryText1, 0f, YELLOW, BULLET, Integer.toString(Math.round(ship.getMutableStats().getSystemRangeBonus().computeEffective(PARRY_RADIUS))));
        if (hullSize == ShipAPI.HullSize.FRIGATE) { parry.addPara(ParryText2, 0f, Misc.getPositiveHighlightColor(), BULLET, Math.round(PARRY_SHIELD_EFF_CHANGE)+"%", Integer.toString(Math.round(PARRY_BUFF_DURATION)));}
        parry.addPara(ParryText3b, 0f, Misc.getPositiveHighlightColor(), BULLET, RAFTitle, Integer.toString(Math.round(PARRY_BUFF_DURATION)));
        //parry.addPara(ParryText4, 0f, Misc.getNegativeHighlightColor(), BULLET, Math.round(PARRY_ROF_BUFF)+"%");
        //parry.addPara(ParryText3, 0f, Misc.getPositiveHighlightColor(), Math.round(PARRY_FLUX_BUFF)+"%", Integer.toString(Math.round(PARRY_BUFF_DURATION)));
        tooltip.addImageWithText(PAD);

        /*TooltipMakerAPI CFB = tooltip.beginImageWithText(CFBIcon, HEIGHT);
        CFB.addPara(zandatsuTitle, 0f, YELLOW, zandatsuTitle);
        CFB.addPara(zandatsuText1, 0f, Misc.getPositiveHighlightColor(), BULLET, RAFTitle);
        CFB.addPara(zandatsuText4, 0f, Misc.getPositiveHighlightColor(), BULLET, "+"+Misc.getRoundedValue(systemUsesBonus));
        CFB.addPara(zandatsuText2, 0f, Misc.getPositiveHighlightColor(), BULLET, "+"+Misc.getRoundedValue(mobilityBonusPercent)+"%");
        CFB.addPara(zandatsuText3, 0f, Misc.getPositiveHighlightColor(), BULLET, "+"+Misc.getRoundedValue(durabilityBonusFlat));
        tooltip.addImageWithText(PAD);*/
    }
    
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        /*if (stats.getVariant().hasHullMod("converted_fighterbay")) {
            stats.getFluxCapacity().modifyMult(id, fluxStatsMult);
            stats.getFluxDissipation().modifyMult(id, fluxStatsMult);
            stats.getMaxSpeed().modifyPercent(id, mobilityBonusPercent);
            stats.getAcceleration().modifyPercent(id, mobilityBonusPercent * 2f);
            stats.getDeceleration().modifyPercent(id, mobilityBonusPercent);
            stats.getTurnAcceleration().modifyPercent(id, mobilityBonusPercent * 2f);
            stats.getMaxTurnRate().modifyPercent(id, mobilityBonusPercent);
            stats.getPeakCRDuration().modifyFlat(id, durabilityBonusFlat);
            stats.getArmorBonus().modifyFlat(id, durabilityBonusFlat);
            stats.getSystemUsesBonus().modifyFlat(id, systemUsesBonus);
            stats.getCargoMod().modifyFlat(id, -50f);
        }*/
	stats.getBallisticRoFMult().modifyMult(id, ROF_BONUS);
	stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION);
	stats.getBallisticAmmoRegenMult().modifyMult(id, ROF_BONUS);
	stats.getEnergyRoFMult().modifyMult(id, ROF_BONUS);
	stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION);
	stats.getEnergyAmmoRegenMult().modifyMult(id, ROF_BONUS);
    }
    
    /*@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getVariant().hasHullMod("shield_shunt")) {
            ship.getVariant().removeMod("shield_shunt");
            Global.getSoundPlayer().playUISound("cr_allied_critical", 1f, 1f);
        }
    }/*
    
    /*@Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize, ShipAPI ship) {
        if (index == 0) return "50%";
        if (index == 1) return "" + (int) (((Float) FLUX_REDUCTION) * 100f) + "%";
        return null;
    }*/
}