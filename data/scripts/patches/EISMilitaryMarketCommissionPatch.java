package data.scripts.patches;

import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.MilitarySubmarketPlugin;
import com.fs.starfarer.api.util.Misc;
import patchlib.api.context.AfterContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.After;
import patchlib.api.patch.Patch;

/**
 * Lets an Iron Shell commission open Hegemony military markets, and a Hegemony commission open Iron Shell ones.
 *
 * Every commission gate in MilitarySubmarketPlugin (buy/sell legality and the "requires commission" checks) goes through
 * hasCommission(), which only compares the market's faction with the commission faction. This after hook turns a false
 * into true for those two faction pairs only; every other result is left as it is. subtype so mods that replace the
 * military market plugin with a subclass are covered too (the hook is idempotent, so a super call firing it twice is fine).
 * Targets are API names (stable).
 */
@Patch(target = @ClassMatch(subtype = MilitarySubmarketPlugin.class))
public class EISMilitaryMarketCommissionPatch {

    public static final String IRON_SHELL = "ironshell";

    @After(target = @MethodMatch(methodName = "hasCommission", parameterCount = 0))
    public static void afterHasCommission(AfterContext context) {
        if (Boolean.TRUE.equals(context.getReturnValue())) return;

        String commission = Misc.getCommissionFactionId();
        if (!IRON_SHELL.equals(commission) && !Factions.HEGEMONY.equals(commission)) return;

        Object self = context.getSelf();
        if (!(self instanceof SubmarketPlugin)) return;
        SubmarketAPI submarket = ((SubmarketPlugin) self).getSubmarket();
        if (submarket == null || submarket.getFaction() == null) return;
        String marketFaction = submarket.getFaction().getId();

        boolean partner = (IRON_SHELL.equals(commission) && Factions.HEGEMONY.equals(marketFaction))
                || (Factions.HEGEMONY.equals(commission) && IRON_SHELL.equals(marketFaction));
        if (partner) context.setReturnValue(true);
    }
}
