package data.scripts.hullmods;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.hullmods.BaseLogisticsHullMod;

public class eis_aegissuite extends BaseLogisticsHullMod {
//This is supposedly a... Terminator Core hullmod duplicate.... somewhat.
    private static float DAMAGE_MISSILES_PERCENT = 50f;
    private static float DAMAGE_FIGHTERS_PERCENT = 50f;
    private static final float PROJECTILE_SPEED_MULT = 1.2f;
    private static final float BALLISTIC_RANGE_BONUS = 100f;
    private static final float PD_RANGE_BONUS = 100f;
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDamageToMissiles().modifyPercent(id, DAMAGE_MISSILES_PERCENT);
        stats.getDamageToFighters().modifyPercent(id, DAMAGE_FIGHTERS_PERCENT);
	stats.getProjectileSpeedMult().modifyMult(id, PROJECTILE_SPEED_MULT);
        stats.getBallisticWeaponRangeBonus().modifyFlat(id, BALLISTIC_RANGE_BONUS);
        //Yes I know Vulcan Cannon gets +100 more range this is intentional with both range bonus.
	stats.getNonBeamPDWeaponRangeBonus().modifyFlat(id, PD_RANGE_BONUS);
	stats.getAutofireAimAccuracy().modifyFlat(id, 1f);
	stats.getEngineDamageTakenMult().modifyMult(id, 0f);
	stats.getDynamic().getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f);
    }
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);   
    }
    
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
	if (index == 0) return "" + (int) DAMAGE_MISSILES_PERCENT + "%";
	if (index == 1) return "" + Math.round((PROJECTILE_SPEED_MULT - 1f) * 100f) + "%";
	if (index == 2) return "" + (int) BALLISTIC_RANGE_BONUS;
	if (index == 3) return "" + (int) PD_RANGE_BONUS;
	return null;
    }
}







