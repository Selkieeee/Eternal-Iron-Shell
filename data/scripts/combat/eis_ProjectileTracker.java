package data.scripts.combat;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.CombatEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Ported from Ship Mastery System's shipmastery.combat.ProjectileTracker + CombatListenerManager wiring.
// Registered once per combat via data/config/settings.json's "plugins" list. Reads the engine's own live
// projectile list (via the internal com.fs.starfarer.combat.CombatEngine, not the public API) and walks it
// backwards from the end each frame, stopping at the first entry already seen - new projectiles are always
// appended to the end, so this only ever visits newly-spawned ones instead of rescanning the whole battle's
// projectile list every frame. New projectiles are then grouped by source ship and dispatched to that ship's
// own eis_ProjectileCreatedListener instances.
public class eis_ProjectileTracker extends BaseEveryFrameCombatPlugin {
    private List<DamagingProjectileAPI> projectileList = null;
    private final Set<DamagingProjectileAPI> seenProjectiles = new HashSet<>();
    private final IntervalUtil seenCleanupInterval = new IntervalUtil(1f, 2f);

    @Override
    @SuppressWarnings("unchecked")
    public void init(CombatEngineAPI engine) {
        projectileList = (List<DamagingProjectileAPI>) ((CombatEngine) engine).getObjects().getList(DamagingProjectileAPI.class);
        seenProjectiles.clear();
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (projectileList == null) {
            return;
        }
        CombatEngineAPI engine = com.fs.starfarer.api.Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {
            return;
        }

        Map<ShipAPI, List<DamagingProjectileAPI>> newProjectiles = new HashMap<>();
        for (int i = projectileList.size() - 1; i >= 0; i--) {
            DamagingProjectileAPI proj = projectileList.get(i);
            if (seenProjectiles.contains(proj)) {
                break;
            }
            if (proj.getSource() != null) {
                newProjectiles.computeIfAbsent(proj.getSource(), k -> new ArrayList<>()).add(proj);
            }
            seenProjectiles.add(proj);
        }

        for (Map.Entry<ShipAPI, List<DamagingProjectileAPI>> entry : newProjectiles.entrySet()) {
            ShipAPI ship = entry.getKey();
            for (eis_ProjectileCreatedListener listener : ship.getListeners(eis_ProjectileCreatedListener.class)) {
                for (DamagingProjectileAPI proj : entry.getValue()) {
                    listener.reportProjectileCreated(proj);
                }
            }
        }

        seenCleanupInterval.advance(amount);
        if (seenCleanupInterval.intervalElapsed()) {
            seenProjectiles.removeIf(entity -> !engine.isEntityInPlay(entity));
        }
    }
}
