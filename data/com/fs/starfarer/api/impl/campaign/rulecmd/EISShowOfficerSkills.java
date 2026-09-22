package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISShowOfficerSkills <person id>
 *
 * Mirrors DA Mistral's Mistral_ShowOfficerSkills: (re)applies the officer's level, skills, and
 * personality, then prints the resulting skill panel into the dialog text, the same way vanilla's
 * officer recruitment dialog does when asked "what can you do?" (OfficerManagerEvent's
 * "printSkills" action). Run when a hire offer is first shown, before the accept/decline choice,
 * so the panel reflects the build immediately rather than only once actually recruited.
 *
 * Currently only supports Charlotte Hex (eissneed) - her build matches the skills she's already
 * created with in MyLoveForIron.java (kept in sync manually for now). Add an else-if branch here
 * for other characters' hire offers as they're implemented.
 */
public class EISShowOfficerSkills extends BaseCommandPlugin {

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.size() == 0) return false;

        String personId = params.get(0).getString(memoryMap);
        PersonAPI person = Global.getSector().getImportantPeople().getPerson(personId);
        if (person == null) return false;

        if ("eissneed".equals(personId)) {
            person.getStats().setLevel(8);
            // Skills from the old build that are no longer part of it; zeroed so an already-created Charlotte loses them too.
            person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 0);
            person.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 0);
            person.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 0);
            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            person.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
            person.getStats().setSkillLevel("eis_xiv", 2);
            person.getStats().setSkillLevel("eis_weaponmastery", 2);
            person.setPersonality(Personalities.RECKLESS);
        }

        TextPanelAPI text = dialog.getTextPanel();
        text.addSkillPanel(person, false);

        return true;
    }

}
