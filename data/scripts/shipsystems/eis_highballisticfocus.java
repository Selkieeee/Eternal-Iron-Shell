package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import java.awt.Color;
import java.util.EnumSet;

public class eis_highballisticfocus extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS_PERCENT = 40f;
        public static final float ROF_BONUS = 0.8f;
        public static final float FLUX_REDUCTION = .45f;
        public static final float TOP_SPEED_BONUS = 40f;
        public static final float ACCELERATION_BONUS_PERCENT = 80f;
        public static final float DECELERATION_BONUS_PERCENT = 80f;
        public static final float TURN_ACCELERATION_BONUS_FLAT = 12f;
        public static final float TURN_ACCELERATION_BONUS_PERCENT = 80f;
        public static final float MAX_TURN_RATE_BONUS_FLAT = 6f;
        public static final float MAX_TURN_RATE_BONUS_PERCENT = 40f;
        private CombatEngineAPI engine =  Global.getCombatEngine();
        private static final Color ballisticGlowColor = new Color (255,200,0,155);
        private static final Color energyGlowColor = new Color (255,0,255,255);
        public static String eis_highballisticfocus1 = Global.getSettings().getString("eis_ironshell", "eis_highballisticfocus1");
        public static String eis_highballisticfocus2 = Global.getSettings().getString("eis_ironshell", "eis_highballisticfocus2");
        public static String eis_highballisticfocus3 = Global.getSettings().getString("eis_ironshell", "eis_highballisticfocus3");
        public static String eis_highballisticfocus4 = Global.getSettings().getString("eis_ironshell", "eis_highballisticfocus4");
        
        @Override
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
                if (engine.isPaused()) return;
                ShipAPI ship = (ShipAPI) stats.getEntity();
                if (ship == null) {return;}
                float mult = 1f + ROF_BONUS * effectLevel;
                float bonusPercent = DAMAGE_BONUS_PERCENT * effectLevel;
                // The buffs and the weapon glow last the whole system duration and fade in and out with effectLevel (charge up and charge down).
                stats.getEnergyWeaponDamageMult().modifyPercent(id, bonusPercent);
                stats.getBallisticRoFMult().modifyMult(id, mult);
                stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION);
                ship.setWeaponGlow(effectLevel, energyGlowColor, EnumSet.of(WeaponType.ENERGY));
                ship.setWeaponGlow(effectLevel, ballisticGlowColor, EnumSet.of(WeaponType.BALLISTIC));
		if (state == ShipSystemStatsScript.State.OUT) {
                    // the speed and turn rate caps drop as soon as the charge down starts, like vanilla Maneuvering Jets
                    stats.getMaxSpeed().unmodify(id);
                    stats.getMaxTurnRate().unmodify(id);
		} else {
                    stats.getMaxSpeed().modifyFlat(id, TOP_SPEED_BONUS);
                    stats.getAcceleration().modifyPercent(id, ACCELERATION_BONUS_PERCENT * effectLevel);
                    stats.getDeceleration().modifyPercent(id, DECELERATION_BONUS_PERCENT * effectLevel);
                    stats.getTurnAcceleration().modifyFlat(id, TURN_ACCELERATION_BONUS_FLAT * effectLevel);
                    stats.getTurnAcceleration().modifyPercent(id, TURN_ACCELERATION_BONUS_PERCENT * effectLevel);
                    stats.getMaxTurnRate().modifyFlat(id, MAX_TURN_RATE_BONUS_FLAT);
                    stats.getMaxTurnRate().modifyPercent(id, MAX_TURN_RATE_BONUS_PERCENT);
		}
                
		/*if (stats.getEntity() instanceof ShipAPI) {
			ship.getEngineController().fadeToOtherColor(this, new Color(100,255,100,255), new Color(0,0,0,0), effectLevel, 0.67f);
			ship.getEngineController().extendFlame(this, 2f * effectLevel, 0f * effectLevel, 0f * effectLevel);
		}*/
	}
        @Override
	public void unapply(MutableShipStatsAPI stats, String id) {
                ShipAPI ship = (ShipAPI) stats.getEntity();
                if (ship == null) {return;}
		stats.getEnergyWeaponDamageMult().unmodify(id);
		stats.getBallisticRoFMult().unmodify(id);
                stats.getBallisticWeaponFluxCostMod().unmodify(id);
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
                ship.setWeaponGlow(0f, ballisticGlowColor, EnumSet.of(WeaponType.BALLISTIC));
                ship.setWeaponGlow(0f, energyGlowColor, EnumSet.of(WeaponType.ENERGY));
	}
	
        @Override
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("+" + Math.round(DAMAGE_BONUS_PERCENT * effectLevel) + "% " + eis_highballisticfocus1, false);
		} else if (index == 1) {
			return new StatusData("+" + Math.round(ROF_BONUS * 100f * effectLevel) + "% " + eis_highballisticfocus2, false);
		} else if (index == 2) {
			return new StatusData("-" + Math.round(FLUX_REDUCTION * 100f) + "% " + eis_highballisticfocus3, false);
		} else if (index == 3) {
			return new StatusData("+" + Math.round(TOP_SPEED_BONUS) + " " + eis_highballisticfocus4, false);
		}
		return null;
	}
}
