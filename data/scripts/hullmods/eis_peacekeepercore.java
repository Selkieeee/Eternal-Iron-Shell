package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.util.IntervalUtil;

public class eis_peacekeepercore extends BaseHullMod {
    private static final float HAVE_SEX_BONUS = 50f;
    private static final float HAVE_SEX_BONUS2 = 0.66f;
    // One hullmod object is shared by every ship that has it, so each ship's overload-check timer lives in the combat engine's custom data instead of in a field here.
    private static final String DATA_KEY = "eis_peacekeepercore_data";
    
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDamageToMissiles().modifyPercent(id, HAVE_SEX_BONUS);
        stats.getDamageToFighters().modifyPercent(id, HAVE_SEX_BONUS);
        //stats.getBallisticRoFMult().modifyMult(id, HAVE_SEX_BONUS2);
        stats.getEngineDamageTakenMult().modifyMult(id, 0f);
        //stats.getDamageToFrigates().modifyMult(id, HAVE_SEX_BONUS2);
        //stats.getDamageToDestroyers().modifyMult(id, HAVE_SEX_BONUS2);
        //stats.getDamageToCruisers().modifyMult(id, HAVE_SEX_BONUS2);
        //stats.getDamageToCapital().modifyMult(id, HAVE_SEX_BONUS2);
    }
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) return "" + (int) HAVE_SEX_BONUS + "%";
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine =  Global.getCombatEngine();
        if (engine == null || engine.isPaused() || !ship.isAlive()) {return;}
        String key = DATA_KEY + "_" + ship.getId();
        PeacekeeperData data = (PeacekeeperData) engine.getCustomData().get(key);
        if (data == null) {
            data = new PeacekeeperData();
            engine.getCustomData().put(key, data);
        }
        data.tracker.advance(amount);
        if (data.tracker.intervalElapsed()) {
                FluxTrackerAPI flux = ship.getFluxTracker();
                if (flux.isOverloadedOrVenting() && ship.getSystem().getAmmo() > 0) {
                    flux.stopOverload();
                    flux.setCurrFlux(0f);
                    flux.setHardFlux(0f);
                    ship.useSystem();
                    flux.setCurrFlux(0f);
                    flux.setHardFlux(0f);
                    //ship.getMutableStats().getCombatEngineRepairTimeMult().modifyMult("eis_peacekeepercore", 0f);
                    //engine.addFloatingText(ship.getLocation(), "My Life For Ava!", 15f, Color.WHITE, ship, 1f, 0.5f);
                } //else {ship.getMutableStats().getCombatEngineRepairTimeMult().unmodifyMult("eis_peacekeepercore");}
        }
    }

    private static class PeacekeeperData {
        IntervalUtil tracker = new IntervalUtil(0.5f, 0.5f);
    }
}


