package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

// Particle burst on each DURANDAL shot, with the same values as the Tahlan Shipworks Heavy Thermal Blast Cannon:
// two bursts venting backward to each side of the muzzle.
public class eis_DurandalMuzzleBurst implements EveryFrameWeaponEffectPlugin {

    private static final int PARTICLES_PER_BURST = 15;
    private static final float[] BURST_FACINGS = {135f, -135f};
    private static final float BURST_ARC = 30f;
    private static final Color COLOR = new Color(255, 80, 40, 165);
    private static final float SIZE_MIN = 15f;
    private static final float SIZE_MAX = 20f;
    private static final float VELOCITY_MIN = 10f;
    private static final float VELOCITY_MAX = 50f;
    private static final float DURATION_MIN = 0.8f;
    private static final float DURATION_MAX = 1f;
    private static final float OFFSET_MIN = 5f;
    private static final float OFFSET_MAX = 10f;
    private static final float CULL_DISTANCE = 600f;
    // Distances forward from the weapon's center, each spawning its own left/right pair of bursts.
    // The turret value keeps the UTPC's spacing behind the muzzle (15) against the DURANDAL's turret fire offset (49).
    private static final float[] SPAWN_FORWARD_HARDPOINT = {49f, 58f};
    private static final float[] SPAWN_FORWARD_TURRET = {34f};

    private boolean hasFiredThisCharge = false;
    private boolean hasFiredOnceWithoutAmmo = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused() || weapon == null || amount <= 0f) return;

        float chargeLevel = weapon.getChargeLevel();
        if (weapon.getAmmo() >= 1) hasFiredOnceWithoutAmmo = false;

        if (chargeLevel >= 1f && !hasFiredThisCharge) {
            // The last round reads 0 ammo on the frame it fires, so one shot is still allowed at 0
            boolean outOfAmmo = weapon.getAmmo() <= 0 && hasFiredOnceWithoutAmmo;
            if (weapon.getAmmo() <= 0) hasFiredOnceWithoutAmmo = true;
            if (!outOfAmmo && engine.getViewport().isNearViewport(weapon.getLocation(), CULL_DISTANCE)) {
                float[] spawnPoints = weapon.getSlot().isHardpoint() ? SPAWN_FORWARD_HARDPOINT : SPAWN_FORWARD_TURRET;
                for (float forward : spawnPoints) {
                    for (float facing : BURST_FACINGS) {
                        spawnBurst(engine, weapon, forward, facing);
                    }
                }
            }
            hasFiredThisCharge = true;
        } else if (hasFiredThisCharge && (chargeLevel <= 0f || !weapon.isFiring())) {
            hasFiredThisCharge = false;
        }
    }

    private void spawnBurst(CombatEngineAPI engine, WeaponAPI weapon, float forward, float facing) {
        Vector2f center = VectorUtils.rotate(new Vector2f(forward, 0f), weapon.getCurrAngle(), new Vector2f());
        Vector2f.add(center, weapon.getLocation(), center);
        float arcFacing = facing + weapon.getCurrAngle();

        for (int i = 0; i < PARTICLES_PER_BURST; i++) {
            float arcPoint = MathUtils.getRandomNumberInRange(arcFacing - BURST_ARC / 2f, arcFacing + BURST_ARC / 2f);
            Vector2f velocity = MathUtils.getPointOnCircumference(weapon.getShip().getVelocity(), MathUtils.getRandomNumberInRange(VELOCITY_MIN, VELOCITY_MAX), arcPoint);
            Vector2f location = MathUtils.getPointOnCircumference(center, MathUtils.getRandomNumberInRange(OFFSET_MIN, OFFSET_MAX), arcPoint);
            float size = MathUtils.getRandomNumberInRange(SIZE_MIN, SIZE_MAX);
            float duration = MathUtils.getRandomNumberInRange(DURATION_MIN, DURATION_MAX);
            engine.addSmoothParticle(location, velocity, size, 1f, duration, COLOR);
        }
    }
}
