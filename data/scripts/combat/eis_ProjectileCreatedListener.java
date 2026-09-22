package data.scripts.combat;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;

// Ported from Ship Mastery System's shipmastery.combat.listeners.ProjectileCreatedListener pattern.
// Attach to a ship via ship.addListener(...) - eis_ProjectileTracker then calls this once for every new
// projectile (including missiles) that ship fires, instead of every listener having to poll
// Global.getCombatEngine().getMissiles() (the whole battle's missile list) every frame itself.
public interface eis_ProjectileCreatedListener {
    void reportProjectileCreated(DamagingProjectileAPI proj);
}
