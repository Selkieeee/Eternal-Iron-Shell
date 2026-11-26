package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager;
import com.fs.starfarer.api.impl.campaign.intel.punitive.PunitiveExpeditionManager.PunExData;
import com.fs.starfarer.api.util.Misc;

/**
 * Vanilla's PunitiveExpeditionManager already skips the faction the player holds a commission with. This extends that to
 * its partner: while the player is commissioned with Iron Shell, the Hegemony sends no punitive expeditions, and while
 * commissioned with the Hegemony, Iron Shell sends none.
 *
 * Both factions' punitiveExpeditionData is territorial only (vsCompetitors and vsFreePort are false), so their only
 * expeditions are the ones against player colonies in systems they claim (Misc.getClaimingFaction). Blocking the partner
 * outright therefore means: no expeditions for colonizing a Hegemony or Iron Shell system while commissioned with either.
 *
 * The block holds the partner's expedition timeout above zero (the manager only checks for a new expedition at zero) and
 * clears its built-up anger, so nothing fires the moment the commission ends; the normal checks resume about a day later.
 * An expedition already under way when the commission is taken is left alone. Registered as a transient script from
 * onGameLoad, so nothing is added to the save.
 */
public class eis_PartnerPunitiveExpeditionBlock implements EveryFrameScript {

    public static final String IRON_SHELL = "ironshell";
    // Days the partner's timeout is held at; the manager subtracts elapsed days before checking it.
    public static final float HOLD_TIMEOUT_DAYS = 1f;

    @Override
    public void advance(float amount) {
        String commission = Misc.getCommissionFactionId();
        String partnerId;
        if (IRON_SHELL.equals(commission)) {
            partnerId = Factions.HEGEMONY;
        } else if (Factions.HEGEMONY.equals(commission)) {
            partnerId = IRON_SHELL;
        } else {
            return;
        }

        PunitiveExpeditionManager manager = PunitiveExpeditionManager.getInstance();
        if (manager == null) return;
        FactionAPI partner = Global.getSector().getFaction(partnerId);
        if (partner == null) return;
        PunExData data = manager.getDataFor(partner);
        if (data == null) return;

        data.anger = 0f;
        if (data.timeout < HOLD_TIMEOUT_DAYS) data.timeout = HOLD_TIMEOUT_DAYS;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }
}
