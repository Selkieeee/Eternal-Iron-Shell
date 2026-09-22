package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class eis_damper extends BaseShipSystemScript {
	private static final Object KEY_JITTER = new Object();
	private static final float DAMAGE_REDUCTION_PERCENT = 40f;

	private static final Color JITTER_UNDER_COLOR = new Color(255,237,171,135);
	private static final int JITTER_UNDER_COPIES = 25;
	private static final float JITTER_UNDER_MIN_RANGE = 0f;
	private static final float JITTER_UNDER_RANGE = 6f;
	private static final Color JITTER_COLOR = new Color(255,165,90,55);
	private static final int JITTER_COPIES = 2;
	private static final float JITTER_MIN_RANGE = 0f;
	private static final float JITTER_RANGE = 5f;

        private static String Icon = "graphics/icons/hullsys/damper_field.png";
        private static String StatusTitle = Global.getSettings().getString("eis_ironshell", "eis_damper1"); // Damper Subfield
        private static String StatusText = Global.getSettings().getString("eis_ironshell", "eis_damper2"); //% less damage taken

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
                float mult = 1f - (DAMAGE_REDUCTION_PERCENT / 100f * effectLevel);
                stats.getHullDamageTakenMult().modifyMult(id, mult);
                stats.getArmorDamageTakenMult().modifyMult(id, mult);
                stats.getShieldDamageTakenMult().modifyMult(id, mult);

		if (effectLevel > 0) {
			ship.setJitterUnder(KEY_JITTER, JITTER_UNDER_COLOR, effectLevel, JITTER_UNDER_COPIES, JITTER_UNDER_MIN_RANGE, JITTER_UNDER_RANGE);
			ship.setJitter(KEY_JITTER, JITTER_COLOR, effectLevel, JITTER_COPIES, JITTER_MIN_RANGE, JITTER_RANGE);
			for (ShipAPI fighter : getFighters(ship)) {
				if (fighter.isHulk()) continue;
				MutableShipStatsAPI fStats = fighter.getMutableStats();
                                fStats.getHullDamageTakenMult().modifyMult(id, mult);
                                fStats.getArmorDamageTakenMult().modifyMult(id, mult);
                                fStats.getShieldDamageTakenMult().modifyMult(id, mult);
				fighter.setJitterUnder(KEY_JITTER, JITTER_UNDER_COLOR, effectLevel, JITTER_UNDER_COPIES, JITTER_UNDER_MIN_RANGE, JITTER_UNDER_RANGE);
				fighter.setJitter(KEY_JITTER, JITTER_COLOR, effectLevel, JITTER_COPIES, JITTER_MIN_RANGE, JITTER_RANGE);
			}
                    if (Global.getCombatEngine().getPlayerShip() == ship) {
                        Global.getCombatEngine().maintainStatusForPlayerShip("eis_damper", Icon, StatusTitle, Misc.getRoundedValueMaxOneAfterDecimal(DAMAGE_REDUCTION_PERCENT * effectLevel) + StatusText, false);
                    }
		}
	}

	private List<ShipAPI> getFighters(ShipAPI carrier) {
		List<ShipAPI> result = new ArrayList<ShipAPI>();
		for (ShipAPI ship : Global.getCombatEngine().getShips()) {
			if (!ship.isFighter()) continue;
			if (ship.getWing() == null) continue;
			if (ship.getWing().getSourceShip() == carrier) {
				result.add(ship);
			}
		}
		return result;
	}


        @Override
	public void unapply(MutableShipStatsAPI stats, String id) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}
                stats.getHullDamageTakenMult().unmodify(id);
                stats.getArmorDamageTakenMult().unmodify(id);
                stats.getShieldDamageTakenMult().unmodify(id);
		for (ShipAPI fighter : getFighters(ship)) {
			if (fighter.isHulk()) continue;
			MutableShipStatsAPI fStats = fighter.getMutableStats();
                        fStats.getHullDamageTakenMult().unmodify(id);
                        fStats.getArmorDamageTakenMult().unmodify(id);
                        fStats.getShieldDamageTakenMult().unmodify(id);
		}
	}


	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0 && effectLevel > 0) {
			return new StatusData(Misc.getRoundedValueMaxOneAfterDecimal(DAMAGE_REDUCTION_PERCENT * effectLevel) + StatusText, false);
		}
		return null;
	}
}
