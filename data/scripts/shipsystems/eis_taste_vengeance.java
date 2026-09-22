package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;

import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class eis_taste_vengeance extends BaseShipSystemScript {
    private static final float BUFF_DURATION = 4.0f;
    private static final float REFLECT_RANGE = 400f; // added onto ship collision radius
    private static final float ROTATION_SPEED = 420f; // how fast missiles get rotated in degrees per second
    public static float PIERCE_MULT = 0.5f;

    // Parry buff - was a shield-efficiency buff with a shield color change; now a ballistic/energy
    // rate-of-fire/reload/flux buff instead, matching eis_zandatsu's own parry-buff pattern (including its
    // weapon glow visual).
    private static final float ROF_BONUS = 1.5f; // +50%
    private static final float FLUX_REDUCTION = 0.33f; // -33%
    private static final float GLOW_FADE_DURATION = 0.5f; // linear 100->0 ramp on the weapon glow after the buff ends
    private static final Color weaponGlowColor = new Color(255, 200, 0, 155); // matches eis_zandatsu's weaponGlowColor

    private static String buffActiveText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceCoreBuffActiveText");
    private static String buffTimeRemainingText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceCoreBuffRemainingText");

    private static final Color PARRY_JITTER_RING_COLOR = new Color (255,255,255,80);

    private boolean reset = true;
    private boolean reflectSuccess = false;
    private boolean soundOnce = false;

    private int doEffects = 0;

    private float activeTime = 0f;

    private CombatEngineAPI engine =  Global.getCombatEngine();

    private ShipAPI ship;

    private Map<MissileAPI,MissileTracker> missileMap = new HashMap<>();

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine.isPaused() || stats.getEntity() == null) return;

        // eis_taste_vengeance.system now sets runScriptWhileIdle, so apply() keeps getting called through
        // COOLDOWN/IDLE (needed for the jitter/click and shield buff below, which now live in COOLDOWN) -
        // but that means unapply() never fires anymore. Detect the return to IDLE here instead and do the
        // same cleanup unapply() used to, then arm reset for the next activation cycle.
        if (state == State.IDLE) {
            if (!reset) {
                if (reflectSuccess) {
                    stats.getBallisticRoFMult().unmodify(id);
                    stats.getBallisticWeaponFluxCostMod().unmodify(id);
                    stats.getBallisticAmmoRegenMult().unmodify(id);
                    stats.getEnergyRoFMult().unmodify(id);
                    stats.getEnergyWeaponFluxCostMod().unmodify(id);
                    stats.getEnergyAmmoRegenMult().unmodify(id);
                    ship.setWeaponGlow(0f, weaponGlowColor, EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY));
                }
                reset = true;
            }
            return;
        }

        if (reset) {
            reset = false;
            reflectSuccess = false;
            doEffects = 0;
            activeTime = 0f;
            ship = (ShipAPI)stats.getEntity();
        }

        // Loop sound - was handled automatically by the engine via the .system spec's "loopSound" field, but
        // that's driven by ShipSystemAPI.isActive() (IN/ACTIVE/OUT only) with no way to extend it into COOLDOWN,
        // so it always cut off the instant OUT ended regardless of down's duration. Played manually here instead,
        // same pattern as eis_vengeance_dauntless_subsystem.advance()'s isOn() check.
        if (ship.getSystem().isActive() || ship.getSystem().isCoolingDown()) {
            Global.getSoundPlayer().playLoop("system_vengeance_loop", ship, 1f, 1f, ship.getLocation(), ship.getVelocity());
        }

        Color jitterColor = PARRY_JITTER_RING_COLOR;
        Color jitterUnderColor = PARRY_JITTER_RING_COLOR;
        if (state == State.ACTIVE) {
            soundOnce = false;
        } else if (state == State.COOLDOWN) {
            // Ready-click + jitter pulse in the last 0.5s of cooldown - mirrors
            // eis_vengeance_dauntless_subsystem's isCooldown() branch. "down" no longer represents the
            // wait-until-ready (down=0 now, see ship_systems.csv) - cooldown does, and it's stat-scaled via
            // getSystemCooldownBonus(), so this naturally re-times itself against a shortened/lengthened cooldown.
            if (ship.getSystem().getCooldownRemaining() <= 0.5f) {
                ship.setJitter(this, jitterColor, 1f, 1, 0, 0f);
                ship.setJitterUnder(this, jitterUnderColor, 1f, 10, 0f, 7f);
                if (!soundOnce) {Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 0.6f, ship.getLocation(), ship.getVelocity());soundOnce = true;}
            }
        }
        float ADJUSTED_RANGE = ship.getMutableStats().getSystemRangeBonus().computeEffective(REFLECT_RANGE);
        float amount = engine.getElapsedInLastFrame();
        if (state == State.ACTIVE) {
            if (doEffects < 10) {
                if (doEffects == 0) { // use doEffects % num == 0 to do it a few times if you want
                    RippleDistortion ripple = new RippleDistortion(ship.getLocation(), ship.getVelocity());
                    ripple.setSize(ADJUSTED_RANGE + ship.getCollisionRadius() * 1.75f);
                    ripple.setIntensity(ship.getCollisionRadius());
                    ripple.setFrameRate(60f);
                    ripple.fadeInSize(0.75f);
                    ripple.fadeOutIntensity(0.5f);
                    DistortionShader.addDistortion(ripple);
                }
                for (int i = 0; i < 4; i++) {
                    Vector2f particleLoc = ship.getLocation();
                    Vector2f particleVel = MathUtils.getRandomPointInCircle(null, (ship.getCollisionRadius() + ADJUSTED_RANGE) * 2f);
                    engine.addSmoothParticle(particleLoc, particleVel, (float)Math.random() * 15f + 5f, 0.5f, 0.75f, Color.white);
                }
                doEffects++;
            }
            //I see the coom in my sight.
            List<MissileAPI> missilesInRange = CombatUtils.getMissilesWithinRange(ship.getLocation(), ADJUSTED_RANGE + ship.getCollisionRadius());
            for (MissileAPI missile : missilesInRange) {
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher")) continue;
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher_hf")) continue;
                if (!missileMap.keySet().contains(missile) && missile.getOwner() != ship.getOwner()) missileMap.put(missile, new MissileTracker(missile));
            }
            List<MissileAPI> toRemove = new ArrayList<>();
            for (MissileAPI missile : missileMap.keySet()) {
                if (missile == null || missile.isFading()) {
                    toRemove.add(missile);
                    continue;
                }
                if (missile.isMine()) { //Just ignore, I don't want to have sex with Doom Mines. It just makes them time out. :^)
                    toRemove.add(missile);
                    missile.fadeOutThenIn(amount);
                    continue;
                }
                try {if (missile.getBehaviorSpecParams().get("behavior").equals("PROXIMITY_FUSE")) {toRemove.add(missile);continue;}} catch (Exception e) {}
                MissileTracker tracker = missileMap.get(missile);
                if (!tracker.isFacingOrigin()) {
                    if (tracker.shouldTurnLeft()) {
                        VectorUtils.rotate(missile.getVelocity(), ROTATION_SPEED * amount);
                        missile.setFacing(missile.getFacing() + ROTATION_SPEED * amount);
                    } else {
                        VectorUtils.rotate(missile.getVelocity(), -ROTATION_SPEED * amount);
                        missile.setFacing(missile.getFacing() - ROTATION_SPEED * amount);
                    }
                }
                if (tracker.getTotalRotation() >= 45f && missile.getOwner() != ship.getOwner()) {
                    if (missile.getMissileAI() instanceof GuidedMissileAI) {((GuidedMissileAI) missile.getMissileAI()).setTarget(null);} //Basic guidance missiles.
                    if (missile.isGuided()) {missile.setMissileAI(new eis_dummyMissileAI(missile, missile.getSource()));} //Salamander sourced missiles
                    missile.setOwner(ship.getOwner());
                    missile.setSource(ship);
                    reflectSuccess = true;
                }
                if (!missilesInRange.contains(missile)) toRemove.add(missile);
            }
            for (MissileAPI missile : toRemove) {
                missileMap.remove(missile);
            }
        }
        // Was State.OUT - moved here since "down" is now 0 (see ship_systems.csv) and no longer where this
        // buff's window lives; State.COOLDOWN now holds the real, stat-scaled wait time.
        if (state == State.COOLDOWN) {
            activeTime += amount;
            if (reflectSuccess && activeTime <= BUFF_DURATION) {
                stats.getBallisticRoFMult().modifyMult(id, ROF_BONUS);
                stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION);
                stats.getBallisticAmmoRegenMult().modifyMult(id, ROF_BONUS);
                stats.getEnergyRoFMult().modifyMult(id, ROF_BONUS);
                stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION);
                stats.getEnergyAmmoRegenMult().modifyMult(id, ROF_BONUS);
                ship.setWeaponGlow(1f, weaponGlowColor, EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY));
            }
            if (reflectSuccess && activeTime > BUFF_DURATION) {
                stats.getBallisticRoFMult().unmodify(id);
                stats.getBallisticWeaponFluxCostMod().unmodify(id);
                stats.getBallisticAmmoRegenMult().unmodify(id);
                stats.getEnergyRoFMult().unmodify(id);
                stats.getEnergyWeaponFluxCostMod().unmodify(id);
                stats.getEnergyAmmoRegenMult().unmodify(id);
                // Stat buff ends immediately, but ramp the weapon glow down linearly over GLOW_FADE_DURATION
                // instead of cutting it instantly.
                float fadeElapsed = activeTime - BUFF_DURATION;
                float glowLevel = Math.max(0f, 1f - fadeElapsed / GLOW_FADE_DURATION);
                ship.setWeaponGlow(glowLevel, weaponGlowColor, EnumSet.of(WeaponType.BALLISTIC, WeaponType.ENERGY));
            }
        }
    }

    // No unapply() override - eis_taste_vengeance.system now sets runScriptWhileIdle so apply() keeps running
    // through COOLDOWN/IDLE, and unapply() never gets called as a result (confirmed vanilla behavior for that
    // flag). The equivalent cleanup (unmodify the buff if still active, arm reset) now happens in
    // apply()'s State.IDLE branch instead.

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index == 0 && state == State.COOLDOWN && reflectSuccess && activeTime < BUFF_DURATION)
            return new StatusData(buffActiveText, false);
        if (index == 1 && state == State.COOLDOWN && reflectSuccess && activeTime < BUFF_DURATION)
            return new StatusData(buffTimeRemainingText + Misc.getRoundedValueMaxOneAfterDecimal(BUFF_DURATION - activeTime) + "", false);
        return null;
    }

    private class MissileTracker  {
        private MissileAPI missile;
        private Vector2f source;
        private float initialFacing;

        public MissileTracker(MissileAPI missile) {
            if (missile.getSource() instanceof ShipAPI) {
                this.source = missile.getSource().getLocation();
            } else {
                this.source = MathUtils.getRandomPointInCone(missile.getLocation(), 800f, missile.getFacing() - 150, missile.getFacing() - 210);
            }
            this.missile = missile;
            this.initialFacing = missile.getFacing();
        }

        public boolean isFacingOrigin() {
            if (source == null)
                return false;
            return Math.abs(MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), source))) < 10;
        }

        public boolean shouldTurnLeft() {
            if (source == null)
                return false;
            float delta = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), source));
            return delta > 0;
        }
        
        public float getTotalRotation() {
            return Math.abs((missile.getFacing() - initialFacing) % 360);
        }
    }
}