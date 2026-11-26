package data.scripts.patches;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.intel.events.sd.SDResolution;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;

/**
 * Counts an Iron Shell commission as a Hegemony one in the Askonia crisis resolution.
 *
 * SDResolution.endStandoffInAskonia reads the player's commission once ($fcm_faction from player memory) and uses it to
 * add Hegemony influence for a Hegemony commission, and, when no faction dominates, to pick the council chair
 * ($global.sdResolutionChairFaction). An Iron Shell commission added no influence and could make "ironshell" the chair,
 * which no rule covers. This redirect returns "hegemony" for that one read when the value is "ironshell"; memory itself is
 * not changed. Targets are API names (stable).
 */
@Patch(target = @ClassMatch(type = SDResolution.class))
public class EISAskoniaResolutionCommissionPatch {

    public static final String IRON_SHELL = "ironshell";

    @RedirectMethodCall(
            target = @MethodMatch(methodName = "endStandoffInAskonia", parameterCount = 1),
            call = @MethodMatch(methodName = "getString", parameterCount = 1),
            owner = @ClassMatch(subtype = MemoryAPI.class))
    public static void redirectCommissionRead(MethodCallContext context) {
        Object value = context.call();
        context.setResult(value);
        if (IRON_SHELL.equals(value) && MemFlags.FCM_FACTION.equals(context.getCallArg(0))) {
            context.setResult(Factions.HEGEMONY);
        }
    }
}
