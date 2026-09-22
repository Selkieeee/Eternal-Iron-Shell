package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

public class eis_vengeancecore extends BaseHullMod {
    private int storedHashCode;

    // Resonance buff: same effect as eis_audaciousrelay's Vengeance-proximity buff (minus its AI escort-tasking),
    // but triggered by a nearby eis_perfect_vengeance / eis_vengeance_dauntless ship instead. Not described in the tooltip.
    private static final float RESONANCE_RANGE = 1000f;
    private static final float RESONANCE_SPEED_INCREASE_PERCENT = 25f;
    private static final float RESONANCE_SHIELD_EFF_PERCENT = 10f;
    private static final float RESONANCE_PPT_INCREASE = 15f;
    private static final float RESONANCE_BUFF_REFRESH_TIME = 30f;
    private static final String RESONANCE_DATA_KEY = "eis_vengeancecore_relay_data";
    private static final Color RESONANCE_CORE_COLOR = new Color(79,187,255,70);
    private static final Color RESONANCE_AQUILA_CORE_COLOR = new Color(255, 135, 240, 200);
    private static final Color RESONANCE_RING_COLOR = new Color(205,235,255,175);
    private static final Color RESONANCE_INITIAL_SHIELD_RING_COLOR = new Color(255,255,255,255);
    private static final Color RESONANCE_INITIAL_SHIELD_CORE_COLOR = new Color(255,125,125,75);
    private static final String RESONANCE_STATUS_TITLE = "Vengeance Resonance";
    private static final String RESONANCE_STATUS_TEXT = "Speed/Shield Efficiency";
    private static final String RESONANCE_STATUS_TEXT2 = "Speed";
    private static final String RESONANCE_STATUS_TEXT3 = "Increased PPT ";

    private static final int rangeRadius = 400;
    private static final int rangeRelay = 1000;
    private static final int rofBuff = 50;
    private static final int fluxReductionBuff = 33;
    //private static final int shieldArcDebuff = 120;
    //private static final int shieldUnfoldRate = 50;
    private static final int buffDur = 4;
    //private static final int debuffDur = 3;
    //private static final float fluxCapacityBonus = 2000f;
    //private static String hullmodname = Global.getSettings().getHullModSpec("neural_interface").getDisplayName();

    private static String successIcon = "graphics/icons/hullsys/fortress_shield.png";
    //private static String failIcon = Global.getSettings().getSpriteName("misc", "eis_parryfail");
    private static String relayIcon = "graphics/icons/campaign/sensor_strength.png";

    private static String Paragraph = Global.getSettings().getString("eis_ironshell", "eis_vengeanceParagraph");
    private static String ParagraphShunted = Global.getSettings().getString("eis_ironshell", "eis_vengeanceParagraphShunted");
    private static String successTitle = Global.getSettings().getString("eis_ironshell", "eis_vengeanceSuccessTitle");
    private static String successText1 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceSuccessText1");
    private static String successText2 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceSuccessText2");
    private static String successText3 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceSuccessText3");
    //private static String failTitle = Global.getSettings().getString("eis_ironshell", "eis_vengeanceFailTitle");
    //private static String failText1 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceFailText1");
    //private static String failText2 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceFailText2");
    //private static String failText3 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceFailText3");
    private static String relayTitle = Global.getSettings().getString("eis_ironshell", "eis_vengeanceRelayTitle");
    private static String relayText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceRelayText1");
    //private static String relayText2 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceRelayText2");
    private static final String BULLET = "\u2022";
    
    /*@Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        /*if (ship.getVariant().hasHullMod("shield_shunt")) {
            ship.getVariant().removeMod("shield_shunt");
            Global.getSoundPlayer().playUISound("cr_allied_critical", 1f, 1f);
        }
    }*/
    
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        //if (stats.getVariant().hasHullMod("neural_interface")) {
        //    stats.getFluxCapacity().modifyFlat(id, fluxCapacityBonus);
        //}
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !engine.isEntityInPlay(ship) || !ship.isAlive()) {return;}
        String key = RESONANCE_DATA_KEY + "_" + ship.getId();
        eis_vengeancecoreRelayData data = (eis_vengeancecoreRelayData) engine.getCustomData().get(key);
        if (data == null) {
            data = new eis_vengeancecoreRelayData();
            engine.getCustomData().put(key, data);
        }
        if (!data.runOnce) {
            data.runOnce = true;
            data.buffId = this.getClass().getName() + "_" + ship.getId();
        }

        if (engine.hashCode() != storedHashCode) {
            storedHashCode = engine.hashCode();
            ship.getMutableStats().getPeakCRDuration().unmodifyFlat(data.buffId);
        }

        if (data.activeTime <= 0f) {
            data.tracker.advance(amount);
            if (data.tracker.intervalElapsed()) {
                List<ShipAPI> shipsInRange = CombatUtils.getShipsWithinRange(ship.getLocation(), RESONANCE_RANGE * 2.5f);
                ShipAPI sourceShip = null;
                for (ShipAPI ship2 : shipsInRange) {
                    if (MathUtils.isWithinRange(ship2, ship, RESONANCE_RANGE) && ship2.getOwner() == ship.getOwner() && (ship2.getVariant().hasHullMod("eis_perfect_vengeance") || ship2.getVariant().hasHullMod("eis_vengeance_dauntless"))) {
                        data.hasResonanceTarget = true;
                        sourceShip = ship2;
                        break;
                    }
                }
                if (data.hasResonanceTarget) {
                    data.activeTime = RESONANCE_BUFF_REFRESH_TIME;
                    data.howManyTimes += 1;
                    MutableShipStatsAPI stats = ship.getMutableStats();
                    stats.getMaxSpeed().modifyPercent(data.buffId, RESONANCE_SPEED_INCREASE_PERCENT);
                    stats.getAcceleration().modifyPercent(data.buffId, RESONANCE_SPEED_INCREASE_PERCENT * 2f);
                    stats.getDeceleration().modifyPercent(data.buffId, RESONANCE_SPEED_INCREASE_PERCENT * 2f);
                    stats.getShieldDamageTakenMult().modifyMult(data.buffId, 1f - RESONANCE_SHIELD_EFF_PERCENT / 100f);
                    if (stats.getPeakCRDuration().getMult() != 1) {
                        stats.getPeakCRDuration().modifyFlat(data.buffId, data.howManyTimes * RESONANCE_PPT_INCREASE / stats.getPeakCRDuration().getMult());
                    } else if (stats.getPeakCRDuration().getMult() == 0) {
                        // do nothing, same as eis_audaciousrelay
                    } else {
                        stats.getPeakCRDuration().modifyFlat(data.buffId, data.howManyTimes * RESONANCE_PPT_INCREASE);
                    }
                    if (sourceShip != null) {
                        if (ship.getVariant().hasHullMod("eis_aquila")) {
                            engine.spawnEmpArcVisual(ship.getLocation(), ship, sourceShip.getLocation(), sourceShip, 12f, RESONANCE_AQUILA_CORE_COLOR, RESONANCE_RING_COLOR);
                        } else {
                            engine.spawnEmpArcVisual(ship.getLocation(), ship, sourceShip.getLocation(), sourceShip, 12f, RESONANCE_CORE_COLOR, RESONANCE_RING_COLOR);
                        }
                    }
                }
            }
        }
        if (data.activeTime > 0f) {
            if (ship == engine.getPlayerShip()) {
                if (ship.getShield() != null) {
                    engine.maintainStatusForPlayerShip(data.buffId, "graphics/icons/hullsys/burn_drive.png",
                            RESONANCE_STATUS_TITLE, "+"+Math.round(RESONANCE_SHIELD_EFF_PERCENT)+"% "+RESONANCE_STATUS_TEXT, false);
                }
                else {
                    engine.maintainStatusForPlayerShip(data.buffId, "graphics/icons/hullsys/burn_drive.png",
                            RESONANCE_STATUS_TITLE, "+"+Math.round(RESONANCE_SHIELD_EFF_PERCENT)+"% "+RESONANCE_STATUS_TEXT2, false);
                }
                engine.maintainStatusForPlayerShip(data.buffId+"2", "graphics/icons/hullsys/burn_drive.png",
                        RESONANCE_STATUS_TITLE, RESONANCE_STATUS_TEXT3 + Misc.getRoundedValue(data.activeTime), false);
            }
            if (ship.getShield() != null) {
                ship.getShield().setRingColor(RESONANCE_RING_COLOR);
                ship.getShield().setInnerColor(RESONANCE_CORE_COLOR);
            }
            data.activeTime -= amount;
            if (data.activeTime <= 0f) {
                data.hasResonanceTarget = false;
                MutableShipStatsAPI stats = ship.getMutableStats();
                stats.getMaxSpeed().unmodifyPercent(data.buffId);
                stats.getAcceleration().unmodifyPercent(data.buffId);
                stats.getDeceleration().unmodifyPercent(data.buffId);
                stats.getShieldDamageTakenMult().unmodifyMult(data.buffId);
                if (ship.getShield() != null) {
                    ship.getShield().setRingColor(RESONANCE_INITIAL_SHIELD_RING_COLOR);
                    ship.getShield().setInnerColor(RESONANCE_INITIAL_SHIELD_CORE_COLOR);
                }
            }
        }
    }

    private static class eis_vengeancecoreRelayData {
        int howManyTimes = 0;
        String buffId = "";
        boolean hasResonanceTarget = false;
        boolean runOnce = false;
        float activeTime = 0f;
        IntervalUtil tracker = new IntervalUtil(1f, 2f);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 50f;
        float PAD = 10f;
        Color YELLOW = new Color(241,199,0);
        if (ship.getShield() != null) {tooltip.addPara(Paragraph, PAD);} else {tooltip.addPara(ParagraphShunted, PAD);}
        TooltipMakerAPI success = tooltip.beginImageWithText(successIcon, HEIGHT);
        success.addPara(successTitle, 0f, YELLOW, successTitle);
        success.addPara(successText1, 0f, YELLOW, BULLET, Integer.toString(Math.round(ship.getMutableStats().getSystemRangeBonus().computeEffective(rangeRadius))));
        success.addPara(successText2, 0f, Misc.getPositiveHighlightColor(), BULLET, Integer.toString(rofBuff)+"%", Integer.toString(buffDur));
        success.addPara(successText3, 0f, Misc.getPositiveHighlightColor(), BULLET, Integer.toString(fluxReductionBuff)+"%", Integer.toString(buffDur));
        tooltip.addImageWithText(PAD);

        /*if (ship.getShield() != null) {
            TooltipMakerAPI fail = tooltip.beginImageWithText(failIcon, HEIGHT);
            fail.addPara(failTitle, 0f, YELLOW, failTitle);
            fail.addPara(failText1, 0f, Misc.getNegativeHighlightColor(), BULLET, Integer.toString(shieldEffBuff)+"%", Integer.toString(debuffDur));
            fail.addPara(failText2, 0f, Misc.getNegativeHighlightColor(), BULLET, Integer.toString(shieldArcDebuff), Integer.toString(debuffDur));
            fail.addPara(failText3, 0f, Misc.getNegativeHighlightColor(), BULLET, Integer.toString(shieldUnfoldRate)+"%", Integer.toString(debuffDur));
            tooltip.addImageWithText(PAD);
        }*/

        TooltipMakerAPI relay = tooltip.beginImageWithText(relayIcon, HEIGHT);
        relay.addPara(relayTitle, 0f, YELLOW, relayTitle);
        relay.addPara(relayText, 0f, YELLOW, "Audacious", Integer.toString(rangeRelay));
        //relay.addPara(relayText2, 0f, Misc.getPositiveHighlightColor(), BULLET, hullmodname, Misc.getRoundedValue(fluxCapacityBonus));
        tooltip.addImageWithText(PAD);
    }
}


