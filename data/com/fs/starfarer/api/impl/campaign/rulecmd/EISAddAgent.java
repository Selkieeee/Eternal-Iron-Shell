package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc.Token;
import data.scripts.eis_nex;
import java.util.List;
import java.util.Map;

/**
 * Nexerelin agents, offered by Kim Quy. Without Nexerelin there are no agents and this always returns false.
 * EISAddAgent                  - false if the player can't take another agent
 * EISAddAgent <level>          - same check, and stores the agent's salary/hiring bonus on the active person
 * EISAddAgent <level> <role>   - adds the agent
 */
public class EISAddAgent extends BaseCommandPlugin {
	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null || dialog.getInteractionTarget() == null || dialog.getInteractionTarget().getMarket() == null || eis_nex.agentsAtCap()) return false;
		if (params.size() == 1) {
			int salary = eis_nex.getAgentSalary(params.get(0).getInt(memoryMap));
			dialog.getInteractionTarget().getActivePerson().getMemoryWithoutUpdate().set("$eis_agent_salary", salary, 0);
			dialog.getInteractionTarget().getActivePerson().getMemoryWithoutUpdate().set("$eis_agent_hiringbonus", salary*4, 0);
		}
		if (params.size() > 1) {
			eis_nex.addAgent(params.get(0).getInt(memoryMap), params.get(1).getString(memoryMap), dialog);
		}
		return true;
	}
}
