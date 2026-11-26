package data.scripts.patches;

import com.fs.starfarer.api.campaign.rules.ExpressionAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import patchlib.api.context.MethodCallContext;
import patchlib.api.match.ClassMatch;
import patchlib.api.match.MethodMatch;
import patchlib.api.patch.Patch;
import patchlib.api.patch.RedirectMethodCall;
import patchlib.api.query.MethodQuery;
import patchlib.api.spec.MethodQuerySpec;

/**
 * Lets vanilla (and other mods') rules.csv conditions that check for a Hegemony commission also pass for an Iron Shell one.
 *
 * Rules read the commission from player memory, $fcm_faction (set by FactionCommissionIntel) and its copy
 * $commissionFactionId (CoreCampaignPluginImpl), and compare the value as a plain string: "$player.fcm_faction == hegemony".
 * This redirects the MemoryAPI.get call inside the rule expression's execute(). When the key is one of those two, the
 * value read is "ironshell", and the right-hand side of the condition is the literal "hegemony", the read returns
 * "hegemony" instead, so "== hegemony" passes and "!= hegemony" fails. Every other read is passed through unchanged:
 * "== ironshell" rows, comparisons against another variable, script assignments and text tokens are unaffected, and
 * nothing is written to memory. Java checks (Misc.getCommissionFactionId(), the Commission rules command) still see "ironshell".
 *
 * Targets: ExpressionAPI, execute(String, InteractionDialogAPI, Map) and MemoryAPI.get(String) are API names (stable).
 * getSecond() is an inferred target: a readable public method on the obfuscated expression class, returning the API type
 * Misc.Token for the condition's right-hand side (0.98.5a-RC5 Windows: com.fs.starfarer.campaign.rules.super; the class
 * name will change, the method name has to be re-checked after game updates). If it is missing the patch steps aside.
 */
@Patch(target = @ClassMatch(subtype = ExpressionAPI.class))
public class EISHegemonyCommissionRulesPatch {

    public static final String IRON_SHELL = "ironshell";
    // Copy of the commission id that CoreCampaignPluginImpl writes into player memory (no MemFlags constant for it).
    public static final String COMMISSION_FACTION_ID_KEY = "$commissionFactionId";

    private static final MethodQuerySpec GET_SECOND = MethodQuery.create().methodName("getSecond").parameterCount(0).build();

    @RedirectMethodCall(
            target = @MethodMatch(methodName = "execute", parameterCount = 3),
            call = @MethodMatch(methodName = "get", parameterCount = 1),
            owner = @ClassMatch(subtype = MemoryAPI.class))
    public static void redirectCommissionRead(MethodCallContext context) {
        Object value = context.call();
        context.setResult(value);

        // cheapest checks first: this runs for every variable read while rule conditions are evaluated
        if (!IRON_SHELL.equals(value)) return;
        Object key = context.getCallArg(0);
        if (!MemFlags.FCM_FACTION.equals(key) && !COMMISSION_FACTION_ID_KEY.equals(key)) return;

        if (!context.hasMethod(GET_SECOND)) return;
        Object second = context.getMethod(GET_SECOND).call();
        if (!(second instanceof Misc.Token)) return;
        Misc.Token token = (Misc.Token) second;
        if (token.isLiteral() && Factions.HEGEMONY.equals(token.string)) {
            context.setResult(Factions.HEGEMONY);
        }
    }
}
