package data.scripts.console;

import com.fs.starfarer.api.Global;
import data.scripts.world.eis_LeviathanGen;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

// eis_generatelevi: generates the Leviathan system for testing while its on-load generation is disabled
// (eis_modPlugin.GENERATE_LEVIATHAN). Uses the same exists() check as the mid-save generation, so it never makes a second copy.
public class eis_generatelevi implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (context != CommandContext.CAMPAIGN_MAP) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }
        if (eis_LeviathanGen.exists()) {
            Console.showMessage("Error: the Leviathan system already exists in this save.");
            return CommandResult.ERROR;
        }
        new eis_LeviathanGen().generate(Global.getSector());
        Console.showMessage("Leviathan system generated.");
        return CommandResult.SUCCESS;
    }
}
