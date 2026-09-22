package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;
import data.scripts.shipsystems.eis_dummyMissileAI;

public class eis_vengeance_dauntless extends BaseHullMod {
    private static final String BULLET = "•";
    private static final float RELAY_RANGE = 1000f; // matches eis_vengeancecore's rangeRelay / eis_audaciousrelay's RANGE_RELAY

    private static String subsystemParagraph = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessParagraph");
    // Same icon as Vengeance Core's "Parry Buff" section (eis_vengeancecore.successIcon).
    private static String subsystemIcon = "graphics/icons/hullsys/fortress_shield.png";
    private static String subsystemSectionTitle = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessSectionTitle");
    private static String subsystemActivatedText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessActivatedText");
    private static String subsystemCooldownText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessCooldownText");
    private static String subsystemRangeText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessRangeText");
    private static String subsystemReflectText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessReflectText");
    private static String subsystemChainText = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessChainText");

    // Own relay synergy blurb (dedicated strings, not shared with eis_vengeancecore.java's "Vengeance Relay"
    // section) since eis_audaciousrelay now also checks for this hullmod's id, but this hullmod boosts both
    // Audacious- and Vengeance-class ships, not just Audacious-class ones. The flux-capacity-on-neural_interface
    // bonus from Vengeance Core's relay section isn't copied - this hullmod doesn't grant that bonus.
    private static String relayIcon = "graphics/icons/campaign/sensor_strength.png";
    private static String relayTitle = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessRelayTitle");
    private static String relayText1 = Global.getSettings().getString("eis_ironshell", "eis_vengeanceDauntlessRelayText1");

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        MagicSubsystemsManager.addSubsystemToShip(ship, new eis_vengeance_dauntless_subsystem(ship));
    }

    @Override
    public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 50f;
        float PAD = 10f;
        Color YELLOW = new Color(241, 199, 0);
        Color HIGHLIGHT = Misc.getHighlightColor();

        tooltip.addPara(subsystemParagraph, PAD);

        float totalCycle = eis_vengeance_dauntless_subsystem.ACTIVE_DURATION
                + eis_vengeance_dauntless_subsystem.OUT_DURATION
                + eis_vengeance_dauntless_subsystem.COOLDOWN_DURATION;

        TooltipMakerAPI subsystem = tooltip.beginImageWithText(subsystemIcon, HEIGHT);
        subsystem.addPara(subsystemSectionTitle, 0f, YELLOW, subsystemSectionTitle);
        subsystem.addPara(subsystemActivatedText, 0f, HIGHLIGHT, BULLET, getSubsystemKeyText(ship));
        subsystem.addPara(subsystemCooldownText, 0f, HIGHLIGHT, BULLET, Integer.toString(Math.round(totalCycle)));
        subsystem.addPara(subsystemRangeText, 0f, HIGHLIGHT, BULLET, Integer.toString(Math.round(eis_vengeance_dauntless_subsystem.REFLECT_RANGE)));
        subsystem.addPara(subsystemReflectText, 0f, HIGHLIGHT, BULLET);
        subsystem.addPara(subsystemChainText, 0f, HIGHLIGHT, BULLET, "chains");
        tooltip.addImageWithText(PAD);

        TooltipMakerAPI relay = tooltip.beginImageWithText(relayIcon, HEIGHT);
        relay.addPara(relayTitle, 0f, YELLOW, relayTitle);
        relay.addPara(relayText1, 0f, YELLOW, "Audacious", "Vengeance", Integer.toString(Math.round(RELAY_RANGE)));
        tooltip.addImageWithText(PAD);
    }

    // The subsystem's key is assigned dynamically by MagicLib (defaults to Left Alt), not a fixed vanilla
    // control binding, so we look up the actual per-ship subsystem instance to display its current key.
    private static String getSubsystemKeyText(ShipAPI ship) {
        if (ship != null) {
            List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
            if (subsystems != null) {
                for (MagicSubsystem s : subsystems) {
                    if (s instanceof eis_vengeance_dauntless_subsystem) {
                        return s.getKeyText();
                    }
                }
            }
        }
        return "L-ALT";
    }

    /**
     * Full independent reimplementation of data.scripts.shipsystems.eis_taste_vengeance, ported to run as a
     * MagicLib subsystem instead of wrapping the vanilla ship system script directly. This avoids sharing the
     * single cached eis_taste_vengeance script instance (and its per-ship fields like ship/activeTime/missileMap)
     * across every ship that carries this ability - each ship now gets its own private instance.
     */
    public static class eis_vengeance_dauntless_subsystem extends MagicSubsystem {
        private static final float BUFF_DURATION = 9.0f;
        private static final float ACTIVE_DURATION = 0.5f;
        private static final float OUT_DURATION = 9.5f;
        private static final float COOLDOWN_DURATION = 20f;
        private static final float REFLECT_RANGE = 400f; // added onto ship collision radius
        private static final float ROTATION_SPEED = 420f; // how fast missiles get rotated in degrees per second
        //private static final float SHIELD_BONUS = .3f;
        private static final float AI_EFFECT_RANGE = 420f;

        //private static final Color PARRY_SUCCESS_CORE_COLOR = new Color(79, 187, 255, 70);
        //private static final Color PARRY_SUCCESS_RING_COLOR = new Color(205, 235, 255, 175);
        private static final Color PARRY_JITTER_RING_COLOR = new Color(255, 255, 255, 80);

        private static final Color VENGEANCE_GLOW_COLOR = new Color(215, 21, 16, 166);
        private static final float VENGEANCE_GLOW_RANGE = 380f; // matches eis_VengeanceGlowScript.EFFECT_RANGE
        private static final float VENGEANCE_GLOW_ROTATION_SPEED = 10f;

        // Chain activation: on activating, the Dauntless scans for nearby Vengeance-class ships, then scans
        // for nearby Audacious-class ships around itself AND around each found Vengeance. Every ship found
        // this way reflects missiles and plays the ripple/particle burst at the same time as the Dauntless,
        // each scaled to its own collision radius (see REFLECT_RANGE usage below - same flat range added to a
        // smaller hull's smaller collision radius naturally gives it a smaller effective bubble).
        private static final float CHAIN_SCAN_RANGE = 1000f;
        private static final String CHAIN_VENGEANCE_HULL_ID = "eis_vengeance";
        private static final String CHAIN_AUDACIOUS_HULL_ID = "eis_audacious";
        private static final Color CHAIN_ARC_CORE_COLOR = new Color(255, 40, 30, 220);
        private static final Color CHAIN_ARC_FRINGE_COLOR = new Color(150, 0, 0, 160);

        private static String buffActiveText = Global.getSettings().getString("eis_ironshell", "eis_taste_vengeance1");
        private static String buffTimeRemainingText = Global.getSettings().getString("eis_ironshell", "eis_taste_vengeance2");

        private boolean reflectSuccess = false;
        private boolean soundOnce = false;
        //private boolean shieldColorsCaptured = false;
        private int doEffects = 0;
        private float activeTime = 0f;
        //private Color initialShieldRingColor;
        //private Color initialShieldCoreColor;
        private final Map<MissileAPI, MissileTracker> missileMap = new HashMap<>();
        private final List<ShipAPI> chainedShips = new ArrayList<>();
        private final Map<ShipAPI, Map<MissileAPI, MissileTracker>> chainedMissileMaps = new HashMap<>();
        private final com.fs.starfarer.api.util.IntervalUtil aiTracker = new com.fs.starfarer.api.util.IntervalUtil(0.4f, 0.6f);

        // Handle to the activation sound, so we can fade it out manually during OUT - mirrors the vanilla
        // system's "fadeActivationSoundOnChargedown" flag, which MagicSubsystem has no equivalent for.
        private SoundAPI activationSound;

        private final SpriteAPI vengeanceGlowSprite = Global.getSettings().getSprite("misc", "sfxvengeance");
        private float glowOpacity = 0f;
        private float glowRotation = 0f;

        public eis_vengeance_dauntless_subsystem(ShipAPI ship) {
            super(ship);
        }

        @Override
        public int getOrder() {
            return ORDER_SHIP_UNIQUE;
        }

        // Disables MagicSubsystem's own internal scaling (used for charges/cooldown/regen via scaleSystemStat()),
        // so no subsystem parameter is affected by Systems Expertise or similar. Our direct range calculations
        // below are handled separately since they don't go through this hook at all.
        @Override
        public float getSystemStatsEffectMult() {
            return 0f;
        }

        @Override
        public String getDisplayText() {
            return "Revengeance";
        }

        @Override
        public float getBaseActiveDuration() {
            return ACTIVE_DURATION;
        }

        @Override
        public float getBaseOutDuration() {
            return OUT_DURATION;
        }

        @Override
        public float getBaseCooldownDuration() {
            return COOLDOWN_DURATION;
        }

        @Override
        public void onActivate() {
            reflectSuccess = false;
            soundOnce = false;
            doEffects = 0;
            activeTime = 0f;
            missileMap.clear();
            // Shield buff removed for this subsystem - see the commented-out block in advance() for the original effect.
            /*if (ship.getShield() != null) {
                initialShieldCoreColor = ship.getShield().getInnerColor();
                initialShieldRingColor = ship.getShield().getRingColor();
                shieldColorsCaptured = true;
            } else {
                shieldColorsCaptured = false;
            }*/
            activationSound = Global.getSoundPlayer().playSound("system_vengeance", 1f, 1f, ship.getLocation(), ship.getVelocity());

            activateChain();
        }

        // Finds nearby Vengeance-class ships around the Dauntless, then nearby Audacious-class ships around
        // the Dauntless AND around each found Vengeance, and immediately plays each affected ship's activation
        // sound (scaled down per hull) and a red EMP arc linking it back into the chain. The actual reflect/
        // ripple effects for these ships are driven every frame from advance() below, same as the Dauntless's
        // own effect, via chainedShips/chainedMissileMaps populated here.
        private void activateChain() {
            chainedShips.clear();
            chainedMissileMaps.clear();

            List<ShipAPI> nearbyVengeance = new ArrayList<>();
            // getShipsWithinRange only adds the CANDIDATE's own collision radius (it's called with a bare
            // Vector2f, not the Dauntless itself) - prefiltering at 3x range, then re-checking with the
            // ship-to-ship isWithinRange overload below, is what actually adds BOTH ships' radii, same idea
            // as eis_audaciousrelay (which prefilters at 2.5x its own range).
            for (ShipAPI other : CombatUtils.getShipsWithinRange(ship.getLocation(), CHAIN_SCAN_RANGE * 3f)) {
                if (other == ship || other.getOwner() != ship.getOwner()) continue;
                if (!MathUtils.isWithinRange(other, ship, CHAIN_SCAN_RANGE)) continue;
                if (other.getHullSpec() != null && CHAIN_VENGEANCE_HULL_ID.equals(other.getHullSpec().getHullId())) {
                    nearbyVengeance.add(other);
                }
            }

            List<ShipAPI> nearbyAudacious = new ArrayList<>();
            List<ShipAPI> audaciousScanCenters = new ArrayList<>();
            audaciousScanCenters.add(ship);
            audaciousScanCenters.addAll(nearbyVengeance);
            for (ShipAPI center : audaciousScanCenters) {
                for (ShipAPI other : CombatUtils.getShipsWithinRange(center.getLocation(), CHAIN_SCAN_RANGE * 3f)) {
                    if (other == ship || other.getOwner() != ship.getOwner()) continue;
                    if (nearbyVengeance.contains(other) || nearbyAudacious.contains(other)) continue;
                    if (!MathUtils.isWithinRange(other, center, CHAIN_SCAN_RANGE)) continue;
                    if (other.getHullSpec() != null && CHAIN_AUDACIOUS_HULL_ID.equals(other.getHullSpec().getHullId())) {
                        nearbyAudacious.add(other);
                    }
                }
            }

            chainedShips.addAll(nearbyVengeance);
            chainedShips.addAll(nearbyAudacious);
            for (ShipAPI chained : chainedShips) {
                chainedMissileMaps.put(chained, new HashMap<MissileAPI, MissileTracker>());
            }

            CombatEngineAPI engine = Global.getCombatEngine();
            for (ShipAPI vengeance : nearbyVengeance) {
                Global.getSoundPlayer().playSound("system_vengeance", 1f, 0.5f, vengeance.getLocation(), vengeance.getVelocity());
                engine.spawnEmpArcVisual(ship.getLocation(), ship, vengeance.getLocation(), vengeance, 12f, CHAIN_ARC_FRINGE_COLOR, CHAIN_ARC_CORE_COLOR);
            }

            for (ShipAPI audacious : nearbyAudacious) {
                Global.getSoundPlayer().playSound("system_vengeance", 1f, 0.25f, audacious.getLocation(), audacious.getVelocity());
                ShipAPI closest = findClosestAnchor(audacious, nearbyVengeance);
                engine.spawnEmpArcVisual(audacious.getLocation(), audacious, closest.getLocation(), closest, 12f, CHAIN_ARC_FRINGE_COLOR, CHAIN_ARC_CORE_COLOR);
            }
        }

        // The Dauntless itself is always a candidate anchor point, in addition to every found Vengeance.
        private ShipAPI findClosestAnchor(ShipAPI from, List<ShipAPI> vengeanceShips) {
            ShipAPI closest = ship;
            float bestDistSq = MathUtils.getDistanceSquared(from.getLocation(), ship.getLocation());
            for (ShipAPI vengeance : vengeanceShips) {
                float distSq = MathUtils.getDistanceSquared(from.getLocation(), vengeance.getLocation());
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    closest = vengeance;
                }
            }
            return closest;
        }

        @Override
        public void onFinished() {
            Global.getSoundPlayer().playSound("system_temporalshell_off", 1f, 1f, ship.getLocation(), ship.getVelocity());
        }

        // Ported from eis_VengeanceGlowScript.advance() - only the "Revengeance section" glow, not the
        // Audacious relay "wow" ring or the weapon heat-glow section (neither applies to us here).
        private void updateVengeanceGlow(float amount, boolean isPaused) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || !engine.isUIShowingHUD() || engine.isUIShowingDialog() || engine.getCombatUI().isShowingCommandUI()) {
                return;
            }
            boolean isPlayerFlagship = ship == engine.getPlayerShip();
            // Fade in only once fully off cooldown (isReady()) - not merely "not mid-burst" - so the glow
            // stays hidden through the whole IN/ACTIVE/OUT/COOLDOWN cycle, not just the 0.5s ACTIVE window.
            if (!isPlayerFlagship || !isReady() || ship.isHulk() || ship.isPiece() || !ship.isAlive()) {
                glowOpacity = Math.max(0f, glowOpacity - 2f * amount);
            } else {
                glowOpacity = Math.min(1f, glowOpacity + 4f * amount);
            }
            if (isPaused) {
                return;
            }
            glowRotation += VENGEANCE_GLOW_ROTATION_SPEED * amount;
            if (glowRotation > 360f) {
                glowRotation -= 360f;
            }
        }

        @Override
        public void renderWorld(ViewportAPI viewport) {
            if (glowOpacity <= 0f) {
                return;
            }
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null || !engine.isUIShowingHUD() || engine.isUIShowingDialog() || engine.getCombatUI().isShowingCommandUI()) {
                return;
            }
            Vector2f loc = ship.getLocation();
            if (!viewport.isNearViewport(loc, VENGEANCE_GLOW_RANGE)) {
                return;
            }
            float adjustedRange = VENGEANCE_GLOW_RANGE; // not run through getSystemRangeBonus() - see getSystemStatsEffectMult()
            float diameter = (adjustedRange + ship.getCollisionRadius()) * 2f;
            vengeanceGlowSprite.setSize(diameter, diameter);
            vengeanceGlowSprite.setColor(VENGEANCE_GLOW_COLOR);
            vengeanceGlowSprite.setAdditiveBlend();
            vengeanceGlowSprite.setAlphaMult(0.4f * glowOpacity);
            vengeanceGlowSprite.setAngle(glowRotation);
            vengeanceGlowSprite.renderAtCenter(loc.x, loc.y);
        }

        @Override
        public boolean shouldActivateAI(float amount) {
            if (!isReady()) {
                // Don't let aiTracker accumulate/overshoot during the long IN/ACTIVE/OUT/COOLDOWN window -
                // otherwise it fires (and re-activates) again almost instantly the moment the system becomes
                // ready, causing a near-simultaneous double activation right after regen.
                return false;
            }
            aiTracker.advance(amount);
            if (!aiTracker.intervalElapsed()) {
                return false;
            }
            float missileThreatLevel = 0f;
            int missileThreatAmount = 0;
            List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(ship.getLocation(),
                    ship.getCollisionRadius() + AI_EFFECT_RANGE * MathUtils.getRandomNumberInRange(0.5f, 0.8f));
            for (MissileAPI missile : allMissiles) {
                try {
                    if (missile.getBehaviorSpecParams().get("behavior").equals("PROXIMITY_FUSE")) continue;
                } catch (Exception e) {}
                if (missile.getOwner() != ship.getOwner() && !missile.isMine()) {
                    float scale = 1f;
                    switch (missile.getDamageType()) {
                        case FRAGMENTATION:
                            scale = 0.3f;
                            break;
                        case KINETIC:
                            scale = 1.7f;
                            break;
                        case HIGH_EXPLOSIVE:
                            scale = 0.8f;
                            break;
                        default:
                        case ENERGY:
                            break;
                    }
                    missileThreatLevel += missile.getDamageAmount() * scale;
                    if (missile.getDamageAmount() >= 1000f || missile.getEmpAmount() >= 500f) {
                        missileThreatAmount += 2;
                    } else {
                        missileThreatAmount += 1;
                    }
                }
            }
            return missileThreatLevel >= MathUtils.getRandomNumberInRange(0.15f, 1f) * ship.getHitpoints() * (1 - ship.getFluxTracker().getFluxLevel()) / 2.5f
                    || missileThreatAmount >= 4;
        }

        @Override
        public void advance(float amount, boolean isPaused) {
            updateVengeanceGlow(amount, isPaused);

            // Also let COOLDOWN through - the ready click/jitter pulse below needs to run during the last
            // 0.5s of COOLDOWN, not just while IN/ACTIVE/OUT.
            if (isPaused || (!isOn() && !isCooldown())) {
                return;
            }

            if (isOn()) {
                Global.getSoundPlayer().playLoop("system_vengeance_loop", ship, 1f, 1f, ship.getLocation(), ship.getVelocity());
            }

            float jitterLevel = getEffectLevel();
            float jitterRangeBonus;
            float maxRangeBonus = 10f;
            if (isActive()) {
                soundOnce = false;
            } else if (isOut()) {
                // Manually fade the activation sound's volume down as effectLevel tapers to 0 over the OUT
                // duration - mirrors "fadeActivationSoundOnChargedown" from the original vanilla system.
                if (activationSound != null && activationSound.isPlaying()) {
                    activationSound.setVolume(jitterLevel);
                }
                jitterRangeBonus = jitterLevel * maxRangeBonus;
                // The "ready" click sound + shield jitter pulse used to fire here, in the last 0.5s of OUT -
                // moved to the isCooldown() branch below, so it plays in the last 0.5s of COOLDOWN instead
                // (i.e. right before the subsystem is actually ready), rather than right as OUT ends.
                /*if (activeTime > (BUFF_DURATION - 0.5f)) {
                    float warnJitter = Math.max(1 - activeTime / BUFF_DURATION, 1.0f);
                    ship.setJitter(this, PARRY_JITTER_RING_COLOR, warnJitter, 1, 0, jitterRangeBonus);
                    ship.setJitterUnder(this, PARRY_JITTER_RING_COLOR, warnJitter, 10, 0f, 7f + jitterRangeBonus);
                    if (!soundOnce) {
                        Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 0.6f, ship.getLocation(), ship.getVelocity());
                        soundOnce = true;
                    }
                }*/
            } else if (isCooldown()) {
                // Same effect and duration as the original (a sustained 0.5s jitter pulse, refreshed every
                // frame since a single setJitter call only lasts ~0.2s), just re-timed to the end of COOLDOWN
                // instead of the end of OUT, since the subsystem isn't actually ready until COOLDOWN also ends.
                float cooldownRemaining = (1f - getStateCompleteRatio()) * getCooldownDuration();
                if (cooldownRemaining <= 0.5f) {
                    ship.setJitter(this, PARRY_JITTER_RING_COLOR, 1f, 1, 0, 0f);
                    ship.setJitterUnder(this, PARRY_JITTER_RING_COLOR, 1f, 10, 0f, 7f);
                    if (!soundOnce) {
                        Global.getSoundPlayer().playSound("gun_out_of_ammo", 1f, 0.6f, ship.getLocation(), ship.getVelocity());
                        soundOnce = true;
                    }
                }
            }

            float ADJUSTED_RANGE = REFLECT_RANGE; // not run through getSystemRangeBonus() - see getSystemStatsEffectMult()
            float dt = Global.getCombatEngine().getElapsedInLastFrame();

            if (isActive()) {
                if (doEffects < 10) {
                    if (doEffects == 0) {
                        spawnRipple(ship, ADJUSTED_RANGE);
                        for (ShipAPI chained : chainedShips) {
                            spawnRipple(chained, REFLECT_RANGE);
                        }
                    }
                    spawnParticleBurst(ship, ADJUSTED_RANGE);
                    for (ShipAPI chained : chainedShips) {
                        spawnParticleBurst(chained, REFLECT_RANGE);
                    }
                    doEffects++;
                }

                updateMissileReflection(ship, missileMap, ADJUSTED_RANGE, dt, true);
                for (ShipAPI chained : chainedShips) {
                    if (chained == null || !chained.isAlive() || !Global.getCombatEngine().isEntityInPlay(chained)) continue;
                    Map<MissileAPI, MissileTracker> map = chainedMissileMaps.get(chained);
                    if (map == null) continue;
                    updateMissileReflection(chained, map, REFLECT_RANGE, dt, false);
                }
            }

            if (isOut()) {
                activeTime += dt;
                // Shield buff removed for this subsystem.
                /*if (ship.getShield() != null && shieldColorsCaptured) {
                    if (reflectSuccess && activeTime <= BUFF_DURATION) {
                        ship.getShield().setRingColor(PARRY_SUCCESS_RING_COLOR);
                        ship.getShield().setInnerColor(PARRY_SUCCESS_CORE_COLOR);
                        stats.getShieldDamageTakenMult().modifyMult(getBuffId(), 1f - SHIELD_BONUS);
                    }
                    if (reflectSuccess && activeTime > BUFF_DURATION) {
                        ship.getShield().setRingColor(initialShieldCoreColor);
                        ship.getShield().setInnerColor(initialShieldCoreColor);
                        stats.getShieldDamageTakenMult().unmodifyMult(getBuffId());
                    }
                }*/

                if (ship == Global.getCombatEngine().getPlayerShip() && reflectSuccess && activeTime < BUFF_DURATION) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId(), "graphics/icons/hullsys/damper_field.png",
                            getDisplayText(), buffActiveText, false);
                    Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId() + "2", "graphics/icons/hullsys/damper_field.png",
                            getDisplayText(), buffTimeRemainingText + Misc.getRoundedValueMaxOneAfterDecimal(BUFF_DURATION - activeTime), false);
                }
            }
        }

        // One-time ripple distortion burst, sized off the target's own collision radius - reused for the
        // Dauntless itself and for every chained Vengeance/Audacious so the visual scales down naturally on
        // smaller hulls instead of needing a separate range constant per ship type.
        private void spawnRipple(ShipAPI target, float range) {
            RippleDistortion ripple = new RippleDistortion(target.getLocation(), target.getVelocity());
            ripple.setSize(range + target.getCollisionRadius() * 1.75f);
            ripple.setIntensity(target.getCollisionRadius());
            ripple.setFrameRate(60f);
            ripple.fadeInSize(0.75f);
            ripple.fadeOutIntensity(0.5f);
            DistortionShader.addDistortion(ripple);
        }

        // Per-frame particle burst (called once per frame for the first 10 active frames, same cadence as the
        // original Dauntless-only version) - reused for the Dauntless and every chained ship.
        private void spawnParticleBurst(ShipAPI target, float range) {
            for (int i = 0; i < 4; i++) {
                Vector2f particleLoc = target.getLocation();
                Vector2f particleVel = MathUtils.getRandomPointInCircle(null, (target.getCollisionRadius() + range) * 2f);
                Global.getCombatEngine().addSmoothParticle(particleLoc, particleVel, (float) Math.random() * 15f + 5f, 0.5f, 0.75f, Color.white);
            }
        }

        // The original Dauntless-only missile reflect logic, parameterized so it can be run identically for
        // the Dauntless and for each chained Vengeance/Audacious against their own missile-tracking map.
        // trackReflectSuccess is only true for the Dauntless's own call, since reflectSuccess only ever drove
        // the Dauntless's own player-HUD buff status text, which chained ships don't get.
        private void updateMissileReflection(ShipAPI target, Map<MissileAPI, MissileTracker> map, float range, float dt, boolean trackReflectSuccess) {
            List<MissileAPI> missilesInRange = CombatUtils.getMissilesWithinRange(target.getLocation(), range + target.getCollisionRadius());
            for (MissileAPI missile : missilesInRange) {
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher")) continue;
                if (missile.getWeaponSpec() != null && missile.getWeaponSpec().getWeaponId().equals("motelauncher_hf")) continue;
                if (!map.containsKey(missile) && missile.getOwner() != target.getOwner()) map.put(missile, new MissileTracker(missile));
            }
            List<MissileAPI> toRemove = new ArrayList<>();
            for (MissileAPI missile : map.keySet()) {
                if (missile == null || missile.isFading()) {
                    toRemove.add(missile);
                    continue;
                }
                if (missile.isMine()) {
                    toRemove.add(missile);
                    missile.fadeOutThenIn(dt);
                    continue;
                }
                try {
                    if (missile.getBehaviorSpecParams().get("behavior").equals("PROXIMITY_FUSE")) {
                        toRemove.add(missile);
                        continue;
                    }
                } catch (Exception e) {}
                MissileTracker tracker = map.get(missile);
                if (!tracker.isFacingOrigin()) {
                    if (tracker.shouldTurnLeft()) {
                        VectorUtils.rotate(missile.getVelocity(), ROTATION_SPEED * dt);
                        missile.setFacing(missile.getFacing() + ROTATION_SPEED * dt);
                    } else {
                        VectorUtils.rotate(missile.getVelocity(), -ROTATION_SPEED * dt);
                        missile.setFacing(missile.getFacing() - ROTATION_SPEED * dt);
                    }
                }
                if (tracker.getTotalRotation() >= 45f && missile.getOwner() != target.getOwner()) {
                    if (missile.getMissileAI() instanceof GuidedMissileAI) {
                        ((GuidedMissileAI) missile.getMissileAI()).setTarget(null);
                    }
                    if (missile.isGuided()) {
                        missile.setMissileAI(new eis_dummyMissileAI(missile, missile.getSource()));
                    }
                    missile.setOwner(target.getOwner());
                    missile.setSource(target);
                    if (trackReflectSuccess) reflectSuccess = true;
                }
                if (!missilesInRange.contains(missile)) toRemove.add(missile);
            }
            for (MissileAPI missile : toRemove) {
                map.remove(missile);
            }
        }

        @Override
        public void onStateSwitched(State newState) {
            // Shield buff removed for this subsystem - no longer anything to unmodify here.
            /*if (state == State.COOLDOWN && ship.getShield() != null && shieldColorsCaptured) {
                stats.getShieldDamageTakenMult().unmodifyMult(getBuffId());
            }*/

            // "Ready" click + shield jitter pulse now fires during the last 0.5s of COOLDOWN instead - see the
            // isCooldown() branch in advance() - so it's superseded here rather than firing on the READY
            // transition itself.
        }

        private String getBuffId() {
            return "eis_vengeance_dauntless_" + ship.getId();
        }

        private class MissileTracker {
            private final MissileAPI missile;
            private Vector2f source;
            private final float initialFacing;

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
                if (source == null) return false;
                return Math.abs(MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), source))) < 10;
            }

            public boolean shouldTurnLeft() {
                if (source == null) return false;
                float delta = MathUtils.getShortestRotation(missile.getFacing(), VectorUtils.getAngle(missile.getLocation(), source));
                return delta > 0;
            }

            public float getTotalRotation() {
                return Math.abs((missile.getFacing() - initialFacing) % 360);
            }
        }
    }
}
