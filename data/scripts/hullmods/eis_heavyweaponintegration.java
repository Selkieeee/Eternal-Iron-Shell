package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

public class eis_heavyweaponintegration extends BaseHullMod {
    public static final float COST_REDUCTION = 10.0f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION);
        stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        return index == 0 ? "" + (int) COST_REDUCTION : null;
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }
}
