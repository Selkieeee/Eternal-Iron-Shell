package data.scripts;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

// Ported from vanilla's com.fs.starfarer.api.impl.campaign.skills.EnergyWeaponMastery, broadened to cover both
// ballistic and energy weapons instead of energy only. MutableShipStatsAPI/ShipAPI have no ballistic equivalent
// of getEnergyWeaponFluxBasedBonusDamageMagnitude()/MinLevel()/getFluxBasedEnergyWeaponDamageMultiplier() - that
// mechanic is energy-only in vanilla - so the magnitude/minLevel/multiplier math is replicated locally instead
// of delegating to a ship-level getter, for both weapon types alike.
public class eis_weaponmastery {
	public static float FLUX_COST_MULT = 0.9f;
	public static float PROJ_SPEED_BONUS = 33f;
	public static float MIN_RANGE = 600f;
	public static float MAX_RANGE = 1000f;
	public static float WEAPON_DAMAGE_PERCENT = 30f;
	public static float WEAPON_DAMAGE_MIN_FLUX_LEVEL = 0f;
	public static Object DAM_BONUS_STATUS_KEY = new Object();

	public static String level1EffectText = Global.getSettings().getString("eis_ironshell", "eis_weaponmastery1");
	public static String level1EffectTextDetail = Global.getSettings().getString("eis_ironshell", "eis_weaponmastery1b");
	public static String level2EffectText = Global.getSettings().getString("eis_ironshell", "eis_weaponmastery2");
	public static String level2EffectTextSpeed = Global.getSettings().getString("eis_ironshell", "eis_weaponmastery2b");

	// Mirrors vanilla's ship.getFluxBasedEnergyWeaponDamageMultiplier(): no bonus at/below minLevel flux,
	// ramping linearly up to the full magnitude bonus at 100% flux.
	private static float getMultiplier(ShipAPI ship) {
		if (WEAPON_DAMAGE_PERCENT <= 0f) {
			return 1f;
		}
		float fluxLevel = ship.getFluxTracker().getFluxLevel();
		if (fluxLevel <= WEAPON_DAMAGE_MIN_FLUX_LEVEL) {
			return 1f;
		}
		return 1f + (WEAPON_DAMAGE_PERCENT * 0.01f) * (fluxLevel - WEAPON_DAMAGE_MIN_FLUX_LEVEL) / (1f - WEAPON_DAMAGE_MIN_FLUX_LEVEL);
	}

	private static boolean isAffectedWeaponType(WeaponType type) {
		return type == WeaponType.BALLISTIC || type == WeaponType.ENERGY;
	}

	public static class WMDamageDealtMod implements DamageDealtModifier, AdvanceableListener {
		protected ShipAPI ship;

		public WMDamageDealtMod(ShipAPI ship) {
			this.ship = ship;
		}

		public void advance(float amount) {
			if (Global.getCurrentState() == GameState.COMBAT && Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {
				int damageBonus = Math.round(getMultiplier(ship) * 100f - 100f);
				if (damageBonus > 0) {
					Global.getCombatEngine().maintainStatusForPlayerShip(
							DAM_BONUS_STATUS_KEY,
							"graphics/icons/skills/ballistic_mastery.png",
							"Weapon mastery",
							"+" + damageBonus + "% weapon damage",
							false);
				}
			}
		}

		public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
			Vector2f from;
			WeaponAPI weapon;
			if (param instanceof DamagingProjectileAPI) {
				from = ((DamagingProjectileAPI) param).getSpawnLocation();
				weapon = ((DamagingProjectileAPI) param).getWeapon();
			} else if (param instanceof BeamAPI) {
				from = ((BeamAPI) param).getFrom();
				weapon = ((BeamAPI) param).getWeapon();
			} else {
				return null;
			}

			if (weapon == null || ship == null) {
				return null;
			}
			if (!isAffectedWeaponType(weapon.getSpec().getType())) {
				return null;
			}

			float mag = getMultiplier(ship) - 1f;
			if (mag <= 0f) {
				return null;
			}

			float dist = Misc.getDistance(from, point);
			float f = 1f;
			if (dist > MAX_RANGE) {
				f = 0f;
			} else if (dist > MIN_RANGE) {
				f = 1f - (dist - MIN_RANGE) / (MAX_RANGE - MIN_RANGE);
			}
			f = Math.max(0f, Math.min(1f, f));

			String id = "eis_wm_dam_mod";
			damage.getModifier().modifyPercent(id, mag * f * 100f);
			return id;
		}
	}

	// Non-elite: up to +30% ballistic and energy weapon damage at close range, scaling with the firing ship's
	// flux level. Uses BaseSkillEffectDescription/createCustomDescription (same as vanilla EnergyWeaponMastery.
	// Level1) instead of a plain getEffectDescription() string, so the numbers get properly highlighted and the
	// range-falloff detail line gets its own properly-indented/spaced paragraph, matching vanilla's formatting.
	public static class Level1 extends BaseSkillEffectDescription implements ShipSkillEffect, AfterShipCreationSkillEffect {

		public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.addListener(new WMDamageDealtMod(ship));
		}

		public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
			ship.removeListenerOfClass(WMDamageDealtMod.class);
		}

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
			init(stats, skill);
			info.addPara(level1EffectText, 0f, hc, hc, "+" + (int) WEAPON_DAMAGE_PERCENT + "%");
			info.addPara(indent + level1EffectTextDetail, 0f, tc, hc, "" + (int) MIN_RANGE, "" + (int) MAX_RANGE);
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}

	// Elite: -10% flux generated by ballistic and energy weapons, +33% projectile speed for ballistic and
	// energy projectiles specifically (getBallisticProjectileSpeedMult()/getEnergyProjectileSpeedMult(), not the
	// generic getProjectileSpeedMult(), which would also affect missiles). Also uses createCustomDescription so
	// the two bonuses render as separate, properly-highlighted lines instead of one run-on sentence.
	public static class Level2 extends BaseSkillEffectDescription implements ShipSkillEffect {

		public void apply(MutableShipStatsAPI stats, HullSize hullSize, String id, float level) {
			stats.getBallisticWeaponFluxCostMod().modifyMult(id, FLUX_COST_MULT);
			stats.getEnergyWeaponFluxCostMod().modifyMult(id, FLUX_COST_MULT);
			stats.getBallisticProjectileSpeedMult().modifyPercent(id, PROJ_SPEED_BONUS);
			stats.getEnergyProjectileSpeedMult().modifyPercent(id, PROJ_SPEED_BONUS);
		}

		public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
			stats.getBallisticWeaponFluxCostMod().unmodify(id);
			stats.getEnergyWeaponFluxCostMod().unmodify(id);
			stats.getBallisticProjectileSpeedMult().unmodify(id);
			stats.getEnergyProjectileSpeedMult().unmodify(id);
		}

		public String getEffectDescription(float level) {
			return null;
		}

		public void createCustomDescription(MutableCharacterStatsAPI stats, SkillSpecAPI skill, TooltipMakerAPI info, float width) {
			initElite(stats, skill);
			info.addPara(level2EffectText, 0f, hc, hc, "-" + Math.round((1f - FLUX_COST_MULT) * 100f) + "%");
			info.addPara(level2EffectTextSpeed, 0f, hc, hc, "+" + (int) PROJ_SPEED_BONUS + "%");
		}

		public String getEffectPerLevelDescription() {
			return null;
		}

		public ScopeDescription getScopeDescription() {
			return ScopeDescription.PILOTED_SHIP;
		}
	}
}
