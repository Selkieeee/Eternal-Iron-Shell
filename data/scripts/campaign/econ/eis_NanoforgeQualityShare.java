package data.scripts.campaign.econ;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableStat.StatMod;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;

/**
 * Vanilla's ShipQuality shares a nanoforge's quality bonus only within one faction (keyed by faction + econ group),
 * so a nanoforge in a Hegemony colony never helps Iron Shell.
 * If the best nanoforge bonus in a Hegemony market is greater than the best one in any Iron Shell market, every Iron Shell
 * market gets a flat production quality modifier that tops it up to the Hegemony value. Otherwise the modifier is removed.
 * Registered as a transient script from onGameLoad, so nothing is added to the save.
 */
public class eis_NanoforgeQualityShare implements EveryFrameScript {

    public static final String MOD_ID = "eis_hegemony_nanoforge";
    // Source id the vanilla nanoforge items use on Stats.PRODUCTION_QUALITY_MOD.
    public static final String NANOFORGE_SOURCE = "nanoforge";
    public static final String IRON_SHELL = "ironshell";

    protected IntervalUtil interval = new IntervalUtil(0.25f, 0.5f);

    @Override
    public void advance(float amount) {
        interval.advance(Global.getSector().getClock().convertToDays(amount));
        if (!interval.intervalElapsed()) return;
        update();
    }

    protected void update() {
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();

        float hegemonyBest = 0f;
        MarketAPI hegemonyBestMarket = null;
        float ironShellBest = 0f;
        for (MarketAPI market : markets) {
            if (Factions.HEGEMONY.equals(market.getFactionId())) {
                float bonus = getNanoforgeBonus(market);
                if (bonus > hegemonyBest) {
                    hegemonyBest = bonus;
                    hegemonyBestMarket = market;
                }
            } else if (IRON_SHELL.equals(market.getFactionId())) {
                ironShellBest = Math.max(ironShellBest, getNanoforgeBonus(market));
            }
        }

        boolean share = hegemonyBest > ironShellBest && hegemonyBestMarket != null;
        // The economy tooltip appends the production source market's name to every modifier description, so this
        // names the Hegemony market the bonus comes from instead of repeating "Hegemony".
        String desc = share ? hegemonyBestMarket.getName() + " Nanoforge" : null;
        for (MarketAPI market : markets) {
            if (!IRON_SHELL.equals(market.getFactionId())) continue;
            StatBonus quality = market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD);
            float topUp = share ? hegemonyBest - getNanoforgeBonus(market) : 0f;
            if (topUp > 0f) {
                quality.modifyFlat(MOD_ID, topUp, desc);
            } else {
                quality.unmodifyFlat(MOD_ID);
            }
        }
    }

    protected float getNanoforgeBonus(MarketAPI market) {
        StatMod mod = market.getStats().getDynamic().getMod(Stats.PRODUCTION_QUALITY_MOD).getFlatBonus(NANOFORGE_SOURCE);
        return mod != null ? mod.value : 0f;
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
