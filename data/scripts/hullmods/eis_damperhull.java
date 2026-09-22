package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipwideAIFlags.AIFlags;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicIncompatibleHullmods;
import data.scripts.util.MagicUI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

// Refactored from a BaseHullMod that hijacked the vanilla "Hold Fire" key as an activation button (pressing it
// immediately un-held fire so weapons weren't actually suppressed) into a MagicLib MagicSubsystem, which gives
// its own dedicated hotkey (defaults to Left Alt) instead of piggybacking on an unrelated vanilla control. The
// defense-buff effect, tooltip description, and the missile-threat/low-hull/flux-danger AI activation heuristic
// are preserved as-is; only the activation/timing plumbing changed. getSystemStatsEffectMult() is set to 0f to
// keep this subsystem unaffected by Systems Expertise, matching the original (which had zero connection to the
// vanilla system engine or its skill-scaling hooks at all).
public class eis_damperhull extends BaseHullMod {
    //private static Map duration = new HashMap();
    private static final float ACTIVE_DURATION = 3f; // flat duration for all hull sizes
    private static Map armor = new HashMap();
    private static Set<String> BLOCKED_HULLMODS = new HashSet();
    private static Color color = new Color(255, 135, 240, 200);
    private static Color colorunder = new Color(255, 135, 240, 150);
    static {
        /*duration.put(HullSize.FIGHTER, 1f);
        duration.put(HullSize.FRIGATE, 2f);
        duration.put(HullSize.DESTROYER, 2f);
        duration.put(HullSize.CRUISER, 2.5f);
        duration.put(HullSize.CAPITAL_SHIP, 3f);*/
        armor.put(HullSize.FIGHTER, 0f);
        armor.put(HullSize.FRIGATE, 60f);
        armor.put(HullSize.DESTROYER, 120f);
        armor.put(HullSize.CRUISER, 160f);
        armor.put(HullSize.CAPITAL_SHIP, 200f);
        BLOCKED_HULLMODS.add(HullMods.HEAVYARMOR);
    }
    private static final float lonelyaside = 50f;
    //private static final float smodcooldown = 15f; // replaced by the baste %-increase penalty below
    private static final float cooldown = 20f; // flat baseline recharge time, no longer shield/phase-dependent
    private static final float baste = 25f; // s-mod PENALTY: % increase to recharge time (was a % decrease bonus)
    private static String vanagloriaIcon = "graphics/icons/hullsys/damper_field.png";
    private static final String BULLET = "•";
    private static String vanagloriaTitle = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaTitle");
    private static String vanagloriaText1 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaText1");
    private static String vanagloriaText12 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaText12");
    private static String vanagloriaText1b = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaText1b");
    private static String vanagloriaText1c = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaText1c");
    private static String damperText2 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaDamperText2");
    private static String damperText = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaDamperText1");
    private static String damperText3 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaDamperText3");
    // damperText4 (old s-mod bullet line) removed - the s-mod penalty now lives in hull_mods.csv's sModDesc
    // column, rendered via the vanilla "S-mod penalty" section (addSModEffectSection() below).
    private static String damperText3b = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaDamperText3b");
    private static String vanagloriaText6 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaText6");
    private static String vanagloriaText7 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaText7");
    private static String vanagloriaStatusTexta = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaStatusTexta");
    private static String vanagloriaStatusTextb = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaStatusTextb");
    private static String vanagloriaStatusTextUI1 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaStatusTextUI1");
    private static String vanagloriaStatusTextUI2 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaStatusTextUI2");
    private static String vanagloriaStatusTextUI3 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaStatusTextUI3");
    private static String vanagloriaStatusTextUI4 = Global.getSettings().getString("eis_ironshell", "eis_vanagloriaStatusTextUI4");

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        for (String tmp : BLOCKED_HULLMODS) {
            if (ship.getVariant().getHullMods().contains(tmp)) {
                MagicIncompatibleHullmods.removeHullmodWithWarning(ship.getVariant(), tmp, "eis_damperhull");
            }
        }
        MagicSubsystemsManager.addSubsystemToShip(ship, new eis_damperhull_subsystem(ship));
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
            stats.getEffectiveArmorBonus().modifyFlat(id, (float)armor.get(hullSize));
    }

    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return !(ship.getVariant().hasHullMod(HullMods.HEAVYARMOR) || ship.getHullSpec().isPhase());
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 50f;
        float PAD = 10f;
        Color YELLOW = new Color(241, 199, 0);

        TooltipMakerAPI vanagloria = tooltip.beginImageWithText(vanagloriaIcon, HEIGHT);
        vanagloria.addPara(vanagloriaTitle, 0f, YELLOW, vanagloriaTitle);

        if (ship != null) {
                vanagloria.addPara(vanagloriaText1, 0f, Misc.getPositiveHighlightColor(),
                    BULLET, "+" + Math.round((Float) armor.get(hullSize)));
        } else {
            vanagloria.addPara(vanagloriaText12, 0f, Misc.getPositiveHighlightColor(),
                    BULLET,
                    "+" + Math.round((Float) armor.get(HullSize.DESTROYER)),
                    "" + Math.round((Float) armor.get(HullSize.CRUISER)),
                    "" + Math.round((Float) armor.get(HullSize.CAPITAL_SHIP)));
        }
        if (ship != null && ship.isShipWithModules()) {vanagloria.addPara(vanagloriaText1b, 0f, Misc.getHighlightColor(), BULLET);}
        vanagloria.addPara(vanagloriaText1c, 0f, YELLOW, BULLET, getSubsystemKeyText(ship));
        vanagloria.addPara(damperText, 0f, Misc.getPositiveHighlightColor(), BULLET, Misc.getRoundedValue(lonelyaside)+"%", Misc.getRoundedValueMaxOneAfterDecimal(ACTIVE_DURATION));
        vanagloria.addPara(damperText3, 0f, YELLOW, BULLET, Misc.getRoundedValue(cooldown));
        /*if (ship != null) {
            if (ship.getShield() != null) {
            vanagloria.addPara(damperText3b, 0f, Misc.getPositiveHighlightColor(), BULLET, ship.getHullSpec().getHullNameWithDashClass(),Misc.getRoundedValueMaxOneAfterDecimal(eis_damperhull3(ship)));
            }
        }*/
        //vanagloria.addPara(damperText2, 0f, Misc.getNegativeHighlightColor(), BULLET);
        tooltip.addImageWithText(PAD);
        tooltip.addPara(vanagloriaText7, 5f, Misc.getNegativeHighlightColor(), vanagloriaText7);
        tooltip.addPara(vanagloriaText6, 5f, Misc.getNegativeHighlightColor(), vanagloriaText6);
    }

    // The s-mod effect here is a PENALTY (increased recharge time), not a bonus - shows the vanilla
    // "S-mod penalty" heading in red instead of "S-mod bonus" in gold.
    @Override
    public boolean isSModEffectAPenalty() {
        return true;
    }

    // hull_mods.csv's sModDesc column ("Recharge time increased by %s.") is filled in from this.
    @Override
    public String getSModDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) {
            return "+" + Misc.getRoundedValue(baste) + "%";
        }
        return null;
    }

    // Same vanilla "S-mod bonus/penalty" section body as BaseHullMod.addSModEffectSection(), just with the
    // gray-until-smodded / negative-highlight-once-smodded color toggle damperText4 used to have, instead of
    // vanilla's fixed Misc.getHighlightColor().
    @Override
    public void addSModEffectSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec, boolean isForBuildInList) {
        float opad = 10f;
        boolean smod = !isForModSpec && ship != null
                && (ship.getHullSpec().isBuiltInMod("eis_damperhull") || ship.getVariant().getSMods().contains("eis_damperhull"));
        Color h = smod ? Misc.getNegativeHighlightColor() : Misc.getGrayColor();
        String[] params = new String[]{
                getSModDescriptionParam(0, hullSize, ship),
                getSModDescriptionParam(1, hullSize, ship),
                getSModDescriptionParam(2, hullSize, ship),
                getSModDescriptionParam(3, hullSize, ship),
                getSModDescriptionParam(4, hullSize, ship),
                getSModDescriptionParam(5, hullSize, ship),
                getSModDescriptionParam(6, hullSize, ship),
                getSModDescriptionParam(7, hullSize, ship),
                getSModDescriptionParam(8, hullSize, ship),
                getSModDescriptionParam(9, hullSize, ship)
        };
        tooltip.addPara(spec.getSModDescription(hullSize).replaceAll("%", "%%"), opad, h, Misc.getHighlightColor(), params);
    }

    // Flat 20s baseline recharge time - no longer dependent on shield efficiency/phase upkeep/defense type.
    // S-modding this hullmod is now a 25% recharge time PENALTY instead of the old reduced-cooldown bonus.
    private static float eis_damperhull3(ShipAPI ship) {
        if (ship.getVariant().getHullSpec().isBuiltInMod("eis_damperhull") || ship.getVariant().getSMods().contains("eis_damperhull")) {
            return cooldown * (1f + baste / 100f);
        }
        return cooldown;
    }
    /*private static float eis_damperhull3Old(ShipAPI ship) {
        float localcooldown = cooldown;
        if (ship.getVariant().getHullSpec().isBuiltInMod("eis_damperhull") || ship.getVariant().getSMods().contains("eis_damperhull")) {
            localcooldown = smodcooldown;
        }
        if (ship.getShield() != null) {
            return (localcooldown/(((ship.getShield().getFluxPerPointOfDamage()*ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue()) > 1.33f) ? 1.33f : (ship.getShield().getFluxPerPointOfDamage()*ship.getMutableStats().getShieldDamageTakenMult().getModifiedValue())));
        } else if (ship.getShield() == null && !ship.getHullSpec().isPhase() && (ship.getPhaseCloak() == null || !ship.getHullSpec().getHints().contains(ShipTypeHints.PHASE))) {
            return localcooldown/1.33f;
        } else if (ship.getHullSpec().isPhase() && (ship.getPhaseCloak() != null || ship.getHullSpec().getHints().contains(ShipTypeHints.PHASE))){
            return localcooldown/((ship.getPhaseCloak().getFluxPerSecond()+ship.getPhaseCloak().getFluxPerUse())*6f/ship.getHullSpec().getFluxCapacity()); //ship.getMaxFlux(), this gets total things!
        } else {
            return localcooldown/0.3f;
        }
    }*/

    // The subsystem's key is assigned dynamically by MagicLib (defaults to Left Alt), not a fixed vanilla
    // control binding, so we look up the actual per-ship subsystem instance to display its current key.
    private static String getSubsystemKeyText(ShipAPI ship) {
        if (ship != null) {
            List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
            if (subsystems != null) {
                for (MagicSubsystem s : subsystems) {
                    if (s instanceof eis_damperhull_subsystem) {
                        return s.getKeyText();
                    }
                }
            }
        }
        return "L-ALT";
    }

    public static class eis_damperhull_subsystem extends MagicSubsystem {
        private final IntervalUtil aiTracker = new IntervalUtil(1f, 1f);
        private float missiledetected = 0f;

        public eis_damperhull_subsystem(ShipAPI ship) {
            super(ship);
        }

        @Override
        public int getOrder() {
            return ORDER_SHIP_UNIQUE;
        }

        // Disables MagicSubsystem's own internal scaling, so no subsystem parameter is affected by Systems
        // Expertise - the original hullmod had zero connection to the vanilla system engine or its skill-scaling
        // hooks at all, so this preserves that.
        @Override
        public float getSystemStatsEffectMult() {
            return 0f;
        }

        @Override
        public String getDisplayText() {
            return vanagloriaStatusTextUI1;
        }

        @Override
        public float getBaseActiveDuration() {
            return ACTIVE_DURATION;
        }

        @Override
        public float getBaseCooldownDuration() {
            return eis_damperhull3(ship);
        }

        @Override
        public void onActivate() {
            Global.getSoundPlayer().playSound("system_damper", 1.1f, 0.3f, ship.getLocation(), ship.getVelocity());
        }

        // Mirrors the original's AI heuristic exactly - missile-threat detection, low hull + needs-help,
        // flux/incoming-damage danger, and flux/hull + critical-DPS danger - just re-homed onto
        // shouldActivateAI() instead of a manual advanceInCombat() branch gated on ship.getAI() != null.
        @Override
        public boolean shouldActivateAI(float amount) {
            if (!isReady()) {
                return false;
            }
            if (ship.getPhaseCloak() != null && ship.getPhaseCloak().isActive()) {
                return false;
            }
            aiTracker.advance(amount);
            if (!aiTracker.intervalElapsed()) {
                return false;
            }
            missiledetected = 0f;
            List<MissileAPI> allMissiles = CombatUtils.getMissilesWithinRange(ship.getLocation(), ship.getCollisionRadius() + MathUtils.getRandomNumberInRange(50f, 100f));
            for (MissileAPI missile : allMissiles) {
                if (missile.getOwner() != ship.getOwner() && !missile.isMine()) {
                    float scale = 1f;
                    switch (missile.getDamageType()) {
                        case FRAGMENTATION:
                            scale = 0.3f;
                            break;
                        case KINETIC:
                            scale = 0.85f;
                            break;
                        case HIGH_EXPLOSIVE:
                            scale = 1.7f;
                            break;
                        default:
                        case ENERGY:
                            break;
                    }
                    missiledetected += missile.getDamageAmount() * scale;
                }
            }
            return missiledetected >= 1000f
                    || (ship.getHullLevel() <= 0.5f && ship.getAIFlags().hasFlag(AIFlags.NEEDS_HELP))
                    || ((MathUtils.getRandomNumberInRange(ship.getFluxLevel(), 1f) >= 0.5f || MathUtils.getRandomNumberInRange(ship.getFluxLevel(), 1.3f) >= ship.getHullLevel()) && ship.getAIFlags().hasFlag(AIFlags.HAS_INCOMING_DAMAGE))
                    || ((ship.getFluxLevel() >= 0.6f || ship.getHullLevel() <= 0.4f && !ship.isPhased()) && ship.getAIFlags().hasFlag(AIFlags.IN_CRITICAL_DPS_DANGER));
        }

        @Override
        public void advance(float amount, boolean isPaused) {
            if (isPaused) {
                return;
            }

            if (isActive()) {
                ship.getMutableStats().getHullDamageTakenMult().modifyMult(getBuffId(), lonelyaside / 100f);
                ship.getMutableStats().getArmorDamageTakenMult().modifyMult(getBuffId(), lonelyaside / 100f);
                ship.getMutableStats().getEmpDamageTakenMult().modifyMult(getBuffId(), lonelyaside / 100f);
                ship.getMutableStats().getVentRateMult().modifyMult(getBuffId(), 0f);
                ship.setJitterUnder(ship.getId(), color, 1f, 15, 0f, 6f);
                ship.setJitter(ship.getId(), colorunder, 1f, 1, 0f, 4f);
                Global.getSoundPlayer().playLoop("system_damper_loop", ship, 1.1f, 0.3f, ship.getLocation(), ship.getVelocity());
                ship.setDefenseDisabled(true);
                ship.setShipSystemDisabled(true);
                if (ship.getParentStation() == null && ship.getChildModulesCopy() != null && !ship.getChildModulesCopy().isEmpty()) {
                    for (ShipAPI childModulesCopy : ship.getChildModulesCopy()) {
                        childModulesCopy.setJitterShields(false);
                        childModulesCopy.setDefenseDisabled(true);
                        childModulesCopy.setShipSystemDisabled(true);
                        childModulesCopy.getMutableStats().getHullDamageTakenMult().modifyMult(getBuffId(), lonelyaside / 100f);
                        childModulesCopy.getMutableStats().getArmorDamageTakenMult().modifyMult(getBuffId(), lonelyaside / 100f);
                        childModulesCopy.getMutableStats().getEmpDamageTakenMult().modifyMult(getBuffId(), lonelyaside / 100f);
                        childModulesCopy.getMutableStats().getVentRateMult().modifyMult(getBuffId(), 0f);
                        childModulesCopy.setJitterUnder(childModulesCopy.getId(), color, 1f, 5, 0f, 7f);
                        childModulesCopy.setJitter(childModulesCopy.getId(), colorunder, 1f, 1, 0f, 5f);
                    }
                }
            } else {
                ship.getMutableStats().getHullDamageTakenMult().unmodifyMult(getBuffId());
                ship.getMutableStats().getArmorDamageTakenMult().unmodifyMult(getBuffId());
                ship.getMutableStats().getEmpDamageTakenMult().unmodifyMult(getBuffId());
                ship.getMutableStats().getVentRateMult().unmodifyMult(getBuffId());
                ship.setDefenseDisabled(false);
                ship.setShipSystemDisabled(false);
                if (ship.getParentStation() == null && ship.getChildModulesCopy() != null && !ship.getChildModulesCopy().isEmpty()) {
                    for (ShipAPI childModulesCopy : ship.getChildModulesCopy()) {
                        childModulesCopy.setJitterShields(true);
                        childModulesCopy.setDefenseDisabled(false);
                        childModulesCopy.setShipSystemDisabled(false);
                        childModulesCopy.getMutableStats().getHullDamageTakenMult().unmodifyMult(getBuffId());
                        childModulesCopy.getMutableStats().getArmorDamageTakenMult().unmodifyMult(getBuffId());
                        childModulesCopy.getMutableStats().getEmpDamageTakenMult().unmodifyMult(getBuffId());
                        childModulesCopy.getMutableStats().getVentRateMult().unmodifyMult(getBuffId());
                    }
                }
            }

            if (Global.getCombatEngine().getPlayerShip() == ship) {
                if (isActive()) {
                    // Time-remaining ratio for the active buff, mirroring the original's activeTime/maxActiveTime -
                    // 1 - getStateCompleteRatio() starts at 1.0 right after activation and decays to 0 by the time
                    // ACTIVE ends (same math MagicSubsystem itself uses for OUT's own getEffectLevel()).
                    float remainingRatio = 1f - getStateCompleteRatio();
                    // The vanagloriaSetting text-status fallback was made to work around a problem specific to
                    // the old shared-instance/hold-fire implementation - the MagicSubsystem framework (private
                    // per-ship state, its own key handling) removes that problem, so this now always uses the
                    // MagicUI progress bar.
                    /*if (vanagloriaSetting || (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().equals("damper"))) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId() + "2", "graphics/icons/hullsys/damper_field.png",
                                vanagloriaStatusTextUI1, (int) lonelyaside + vanagloriaStatusTextUI2, false);
                        Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId() + "2", "graphics/icons/hullsys/damper_field.png",
                                vanagloriaStatusTextUI1, vanagloriaStatusTextUI3 + Math.round(remainingRatio * 100) + "%", false);
                    } else {*/
                        // Charge/cooldown bar and key-bind status are now shown natively by the MagicSubsystem
                        // framework's own HUD widget, so our manually-drawn versions are redundant - only the
                        // damage-reduction status text is kept.
                        //MagicUI.drawInterfaceStatusBar(ship, remainingRatio, Misc.getNegativeHighlightColor(), Misc.getNegativeHighlightColor(), remainingRatio, vanagloriaStatusTextUI4, Math.round(remainingRatio * 100));
                        Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId() + "2", "graphics/icons/hullsys/damper_field.png",
                                vanagloriaStatusTextUI1, (int) lonelyaside + vanagloriaStatusTextUI2, false);
                    //}
                } /*else if (isCooldown()) {
                    float chargeRatio = getStateCompleteRatio();
                    if (vanagloriaSetting || (ship.getPhaseCloak() != null && ship.getPhaseCloak().getId().equals("damper"))) {
                        Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId() + "2", "graphics/icons/hullsys/damper_field.png",
                                vanagloriaStatusTextUI1, vanagloriaStatusTextUI3 + Math.round(chargeRatio * 100) + "%", true);
                    } else {
                        MagicUI.drawInterfaceStatusBar(ship, chargeRatio, null, null, chargeRatio, vanagloriaStatusTextUI4, Math.round(chargeRatio * 100));
                    }
                } else if (isReady()) {
                    Global.getCombatEngine().maintainStatusForPlayerShip(getBuffId(), "graphics/icons/hullsys/damper_field.png",
                            vanagloriaStatusTextUI1, vanagloriaStatusTexta + getKeyText() + vanagloriaStatusTextb, false);
                }*/
            }
        }

        private String getBuffId() {
            return "eis_damperhull_" + ship.getId();
        }
    }
}
