package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterLaunchBayAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class eis_xiv {
	public static final float CAPACITY_MULT = 1.05f;
	public static final float DISSIPATION_MULT = 1.05f;
	public static final float HANDLING_MULT = 1.10f;
	public static final float ARMOR_BONUS = 100f;
	public static final int WING_SIZE_MULT = 2;
	private static final Set<String> DOUBLED_WING_IDS = new HashSet<>(Arrays.asList("eis_gunshield_wing", "eis_gunblade_wing"));

        public static String level1EffectText = Global.getSettings().getString("eis_ironshell", "eis_xiv1");
        public static String level2EffectText = Global.getSettings().getString("eis_ironshell", "eis_xiv2");
        public static String level3EffectText = Global.getSettings().getString("eis_ironshell", "eis_xiv3");
        public static String level4EffectText = Global.getSettings().getString("eis_ironshell", "eis_xiv4");

	private static boolean isXIVShip(ShipAPI ship) {
		if (ship == null || ship.getVariant() == null) return false;
		return isXIVShip(ship.getVariant().hasHullMod("fourteenth"), ship.getVariant().getHullSpec().hasTag("heg_aux_bp"), ship.getVariant().getHullSpec().getManufacturer());
	}

	private static boolean isXIVShip(MutableShipStatsAPI stats) {
		if (stats.getVariant() == null) return false;
		return isXIVShip(stats.getVariant().hasHullMod("fourteenth"), stats.getVariant().getHullSpec().hasTag("heg_aux_bp"), stats.getVariant().getHullSpec().getManufacturer());
	}

	private static boolean isXIVShip(boolean hasFourteenth, boolean hasHegAuxBP, String manufacturer) {
		return hasFourteenth || (hasHegAuxBP && "Hegemony".equals(manufacturer));
	}

	// Non-elite: +5% flux capacity/dissipation when piloting XIV ships.
	public static class Level1 implements ShipSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (isXIVShip(stats)) {
				stats.getFluxCapacity().modifyMult(id, CAPACITY_MULT);
				stats.getFluxDissipation().modifyMult(id, DISSIPATION_MULT);
			}
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getFluxCapacity().unmodifyMult(id);
			stats.getFluxDissipation().unmodifyMult(id);
		}

		public String getEffectDescription(float level) {
			return level1EffectText;
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	// Non-elite: +10% speed and maneuverability when piloting XIV ships.
	public static class Level2 implements ShipSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (isXIVShip(stats)) {
				stats.getMaxSpeed().modifyMult(id, HANDLING_MULT);
				stats.getAcceleration().modifyMult(id, HANDLING_MULT);
				stats.getDeceleration().modifyMult(id, HANDLING_MULT);
				stats.getMaxTurnRate().modifyMult(id, HANDLING_MULT);
				stats.getTurnAcceleration().modifyMult(id, HANDLING_MULT);
			}
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getMaxSpeed().unmodifyMult(id);
			stats.getAcceleration().unmodifyMult(id);
			stats.getDeceleration().unmodifyMult(id);
			stats.getMaxTurnRate().unmodifyMult(id);
			stats.getTurnAcceleration().unmodifyMult(id);
		}

		public String getEffectDescription(float level) {
			return level2EffectText;
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	// Elite: +100 armor when piloting XIV ships.
	public static class Level3 implements ShipSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			if (isXIVShip(stats)) {
				stats.getArmorBonus().modifyFlat(id, ARMOR_BONUS);
			}
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getArmorBonus().unmodifyFlat(id);
		}

		public String getEffectDescription(float level) {
			return level3EffectText;
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	// Elite: doubles the effective wing size of Gunshield/Gunblade drone wings launched from the piloted XIV
	// ship's own bays. ShipSkillEffect has no per-frame hook, so this is driven via an AfterShipCreationSkillEffect
	// attaching an AdvanceableListener (the same pattern vanilla's Combat Endurance Level4 uses), rather than a
	// hullmod's advanceInCombat like Diable Avionics' DiableAvionicsUniversalDecksUpgrade2 does it.
	public static class Level4 implements ShipSkillEffect, AfterShipCreationSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
		}

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			if (isXIVShip(ship)) {
				ship.addListener(new WingDoublerListener(ship));
			}
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(WingDoublerListener.class);
		}

		public String getEffectDescription(float level) {
			return level4EffectText;
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	public static class WingDoublerListener implements AdvanceableListener {
		private final ShipAPI ship;

		public WingDoublerListener(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (ship == null || !ship.isAlive() || ship.getOriginalOwner() == -1) {
				return;
			}
			CombatEngineAPI engine = Global.getCombatEngine();
			if (engine == null || engine.isPaused()) {
				return;
			}
			for (FighterLaunchBayAPI bay : ship.getLaunchBaysCopy()) {
				if (bay.getWing() == null) {
					continue;
				}
				FighterWingSpecAPI wingSpec = bay.getWing().getSpec();
				if (!DOUBLED_WING_IDS.contains(wingSpec.getId())) {
					continue;
				}
				int deployed = bay.getWing().getWingMembers().size();
				int maxTotal = wingSpec.getNumFighters() * WING_SIZE_MULT;
				int actualAdd = maxTotal - deployed;
				if (actualAdd > 0) {
					bay.setExtraDeployments(actualAdd);
					bay.setExtraDeploymentLimit(maxTotal);
					bay.setExtraDuration(9999999f);
					bay.setFastReplacements(actualAdd);
				}
			}
		}
	}
}
