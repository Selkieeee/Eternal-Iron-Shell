package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.combat.eis_ProjectileCreatedListener;
import java.awt.Color;

// Replaces eis_justatip.java as the script for the "Gula Tandem Warheads" hullmod (id unchanged: eis_justatip,
// since that id is already referenced by faction known-hullmod grants, tier/rarity toggles in eis_modPlugin.java,
// the EISAddSpecial rules.csv command, and is fitted/built-in on several existing ship variants). Only the
// hull_mods.csv "script" column is repointed to this class; the mechanic and all description text below is new.
public class eis_gula extends BaseHullMod {

    private static final float REGEN_BONUS = 25f; // % faster missile ammo regeneration
    private static final float ROF_BONUS = 25f; // % faster missile rate of fire
    private static final float SPEED_BONUS = 25f; // % bonus missile projectile speed
    private static final float MICROFORGE_BONUS = 25f; // % faster eis_microforge reload cycle, s-mod only

    private static String Icon = "graphics/icons/hullsys/missile_racks.png";
    private static String Title = Global.getSettings().getString("eis_ironshell", "eis_gula_title");
    private static String Text0 = Global.getSettings().getString("eis_ironshell", "eis_gula_text0");
    private static String Text1 = Global.getSettings().getString("eis_ironshell", "eis_gula_text1");
    private static String Text2 = Global.getSettings().getString("eis_ironshell", "eis_gula_text2");
    private static String Text3 = Global.getSettings().getString("eis_ironshell", "eis_gula_text3");
    // Text4/Text5 now render as two separate lines in the vanilla "S-mod bonus" section instead of as
    // bullets in the main list - see addSModEffectSection() below.
    private static String SModText1 = Global.getSettings().getString("eis_ironshell", "eis_gula_text4");
    private static String SModText2 = Global.getSettings().getString("eis_ironshell", "eis_gula_text5");
    private static String FighterMissileWeapons = Global.getSettings().getString("eis_ironshell", "eis_gula_fighter_missile_weapons");
    private static final String BULLET = "•";

    private static boolean isNonGuidedWeapon(WeaponAPI weapon) {
        String tracking = weapon.getSpec().getTrackingStr();
        return tracking == null || "None".equals(tracking);
    }

    public static boolean isGulaSMod(ShipAPI ship) {
        return ship != null && ship.getVariant() != null
                && (ship.getHullSpec().isBuiltInMod("eis_justatip") || ship.getVariant().getSMods().contains("eis_justatip"));
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new GulaListener(ship, false));
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        // S-mod bonus: fighter missile weapons get the bonuses regardless of guidance.
        fighter.addListener(new GulaListener(fighter, isGulaSMod(ship)));
    }

    public static class GulaListener implements AdvanceableListener, eis_ProjectileCreatedListener {
        private final ShipAPI ship;
        private final boolean allowGuided;

        public GulaListener(ShipAPI ship, boolean allowGuided) {
            this.ship = ship;
            this.allowGuided = allowGuided;
        }

        public void advance(float amount) {
            if (ship == null || !ship.isAlive() || Global.getCombatEngine() == null) {
                return;
            }

            for (WeaponAPI weapon : ship.getAllWeapons()) {
                if (weapon.getType() != WeaponType.MISSILE) {
                    continue;
                }
                if (!allowGuided && !isNonGuidedWeapon(weapon)) {
                    continue;
                }

                // +25% ammo regeneration speed - recomputed fresh from the weapon's unmodified base value each
                // frame so this can't compound with itself; composes correctly with the ship-wide
                // getMissileAmmoRegenMult() stat, which is applied as a separate multiplier on top of this.
                if (weapon.usesAmmo() && weapon.getSpec().getAmmoPerSecond() > 0f) {
                    weapon.getAmmoTracker().setAmmoPerSecond(weapon.getSpec().getAmmoPerSecond() * (1f + REGEN_BONUS / 100f));
                }

                // +25% rate of fire - shaves an extra slice off the current cooldown each frame, on top of
                // whatever the engine's own advance() already took off this frame (which itself already
                // accounts for getMissileRoFMult() and any other stacked RoF bonuses). Skipped while
                // isInBurst(): that state uses getCooldownRemaining() for the short delay BETWEEN shots
                // within a single burst (e.g. eis_celestials_blossom's 0.5s), not the delay between firing
                // cycles - shaving that down the same way collapsed it to near-zero, firing the whole burst
                // back to back instead of just refiring the burst itself faster.
                if (!weapon.isInBurst() && weapon.getCooldownRemaining() > 0f) {
                    float extra = amount * (ROF_BONUS / 100f);
                    weapon.setRemainingCooldownTo(Math.max(0f, weapon.getCooldownRemaining() - extra));
                }
            }
        }

        // +25% missile speed - applied directly to the missile's own engine stats once, right as it's
        // created, rather than through the ship-wide getMissileMaxSpeedBonus() stat (which the engine bakes
        // into every missile a ship fires, with no way to scope it to specific weapons). Fired once per
        // missile by eis_ProjectileTracker instead of scanning the whole battle's missile list every frame.
        @Override
        public void reportProjectileCreated(DamagingProjectileAPI proj) {
            if (!(proj instanceof MissileAPI)) {
                return;
            }
            MissileAPI missile = (MissileAPI) proj;
            if (!allowGuided && missile.isGuided()) {
                return;
            }
            missile.getEngineStats().getMaxSpeed().modifyPercent("eis_gula", SPEED_BONUS);
            missile.updateMaxSpeed();
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 50f;
        float PAD = 10f;
        Color YELLOW = new Color(241, 199, 0);

        TooltipMakerAPI Gula = tooltip.beginImageWithText(Icon, HEIGHT);
        Gula.addPara(Title, 0f, YELLOW, Title);
        Gula.addPara(Text0, 0f);
        Gula.addPara(Text1, 0f, Misc.getPositiveHighlightColor(), BULLET, "+" + Misc.getRoundedValue(REGEN_BONUS) + "%");
        Gula.addPara(Text2, 0f, Misc.getPositiveHighlightColor(), BULLET, "+" + Misc.getRoundedValue(ROF_BONUS) + "%");
        Gula.addPara(Text3, 0f, Misc.getPositiveHighlightColor(), BULLET, "+" + Misc.getRoundedValue(SPEED_BONUS) + "%");
        tooltip.addImageWithText(PAD);
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
        return null;
    }

    // hull_mods.csv's sModDesc column only needs to be non-empty for BaseHullMod.hasSModEffect() to detect
    // this hullmod has an s-mod effect at all (gating whether the "S-mod bonus" section heading shows up) -
    // the actual displayed text is built from SModText1/SModText2 below instead, since it's presented as two
    // separate lines rather than the single combined sentence the CSV column holds.
    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) {
            return "+" + Misc.getRoundedValue(MICROFORGE_BONUS) + "%";
        }
        return null;
    }

    // Two separate lines instead of vanilla's single-paragraph default: gray-until-smodded /
    // positive-highlight-once-smodded for the sentence text, normal yellow highlight for "fighter missile
    // weapons" and the microforge percentage.
    @Override
    public void addSModEffectSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec, boolean isForBuildInList) {
        float opad = 10f;
        Color h = (!isForModSpec && isGulaSMod(ship)) ? Misc.getPositiveHighlightColor() : Misc.getGrayColor();
        tooltip.addPara(SModText1, opad, h, Misc.getHighlightColor(), FighterMissileWeapons);
        tooltip.addPara(SModText2, 0f, h, Misc.getHighlightColor(), getSModDescriptionParam(0, hullSize, ship));
    }
}
