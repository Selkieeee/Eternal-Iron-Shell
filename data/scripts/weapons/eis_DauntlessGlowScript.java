package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.hullmods.eis_vengeance_dauntless;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

// Ported from eis_VengeanceGlowScript - just the heat glow/smoke effect, keyed off our
// eis_vengeance_dauntless_subsystem (a MagicLib subsystem) instead of ship.getSystem() (the ship's real
// equipped system slot), since the Dauntless's ability isn't in that slot at all. The ring glow ("Revengeance
// section") lives back on the subsystem itself (eis_vengeance_dauntless_subsystem.renderWorld/updateVengeanceGlow).
// The Audacious relay "wow" ring from the original isn't ported - that's specific to
// eis_vengeance2/eis_audaciousrelay_data, neither of which apply to the Dauntless.
public class eis_DauntlessGlowScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLOR_NORMAL = {255f / 255f, 100f / 255f, 20f / 255f};
    private static final float MAX_JITTER_DISTANCE = 0.2f;
    private static final float MAX_OPACITY = 1f;

    private IntervalUtil interval = new IntervalUtil(0.05f, 0.1f);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || !engine.isUIShowingHUD() || engine.isUIShowingDialog() || engine.getCombatUI().isShowingCommandUI()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }
        eis_vengeance_dauntless.eis_vengeance_dauntless_subsystem subsystem = getOurSubsystem(ship);

        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //----------------------
        // HEAT GLOW SECTION
        //----------------------

        float currentBrightness = subsystem == null ? 0f : subsystem.getEffectLevel();

        //No glows on wrecks or in refit screen
        if (ship.isHulk() || ship.isPiece() || !ship.isAlive() || ship.getOriginalOwner() == -1 || ship.getFluxTracker().isOverloadedOrVenting()) {
            currentBrightness = 0;
        }

        Color colorToUse = new Color(COLOR_NORMAL[0], COLOR_NORMAL[1], COLOR_NORMAL[2], currentBrightness * MAX_OPACITY);

        //Switches to the proper sprite
        if (currentBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        //Spawn some smoke
        // subsystem.isOn() (IN/ACTIVE/OUT) matches vanilla ship.getSystem().isActive()'s span - MagicSubsystem's
        // own isActive() covers only the brief ACTIVE burst, which cut smoke off right after activation.
        if (subsystem != null && subsystem.isOn()) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                for (int i = 0; i < 3; i++) {
                    engine.addSmokeParticle(weapon.getLocation(),
                            MathUtils.getRandomPointInCircle(new Vector2f(0f, 0f), 15f),
                            MathUtils.getRandomNumberInRange(10f, 30f),
                            1f,
                            MathUtils.getRandomNumberInRange(0.5f, 1.5f),
                            new Color(200f / 255f, 200f / 255f, 200f / 255f, MathUtils.getRandomNumberInRange(0.4f, 0.7f) * currentBrightness));
                }
            }
        }

        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);

        //Jitter! Jitter based on our maximum jitter distance and our flux level
        if (currentBrightness > 0) {
            Vector2f randomOffset = MathUtils.getRandomPointInCircle(new Vector2f(weapon.getSprite().getWidth() / 2f, weapon.getSprite().getHeight() / 2f), currentBrightness * MAX_JITTER_DISTANCE);
            weapon.getSprite().setCenter(randomOffset.x, randomOffset.y);
        }
    }

    private static eis_vengeance_dauntless.eis_vengeance_dauntless_subsystem getOurSubsystem(ShipAPI ship) {
        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems == null) {
            return null;
        }
        for (MagicSubsystem s : subsystems) {
            if (s instanceof eis_vengeance_dauntless.eis_vengeance_dauntless_subsystem) {
                return (eis_vengeance_dauntless.eis_vengeance_dauntless_subsystem) s;
            }
        }
        return null;
    }
}
