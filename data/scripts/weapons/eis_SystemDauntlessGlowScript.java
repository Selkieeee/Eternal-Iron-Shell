package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.hullmods.eis_vengeance_dauntless;
import java.awt.Color;
import java.util.List;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

// Duplicated from eis_DauntlessGlowScript - same heat glow/jitter effect, keyed off our
// eis_vengeance_dauntless_subsystem, but with the smoke-particle section removed for this second
// system-glow weapon (eis_system_dauntless_glow) so both weapons don't double up on smoke.
public class eis_SystemDauntlessGlowScript implements EveryFrameWeaponEffectPlugin {
    private static final float[] COLOR_NORMAL = {255f / 255f, 100f / 255f, 20f / 255f};
    private static final float MAX_JITTER_DISTANCE = 0.2f;
    private static final float MAX_OPACITY = 1f;

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
