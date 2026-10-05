package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel.ContactState;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * AddPotentialContact but more based
 */
public class EISAddContact extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		
		SectorEntityToken entity = dialog.getInteractionTarget();
		if (entity == null) return false;
		
		PersonAPI person = null;
		if (params.size() > 0) {
			String personId = params.get(0).getString(memoryMap);
			PersonDataAPI data = Global.getSector().getImportantPeople().getData(personId);
			if (data != null) {
				person = data.getPerson();
			}
		}
		
		if (person == null) {
			person = entity.getActivePerson();
		}
		if (person == null) return false;
		
		// Always added as a real contact, even over the contact cap (the cap only gates developing further contacts).
		// Reuse any existing intel for this person (e.g. a potential contact from before this change) instead of duplicating it.
		ContactIntel intel2 = ContactIntel.getContactIntel(person);
		if (intel2 != null && (intel2.isEnding() || intel2.isEnded())) intel2 = null;
		if (intel2 != null && (intel2.getState() == ContactState.NON_PRIORITY || intel2.getState() == ContactState.PRIORITY)) {
			return true;
		}
		if (intel2 == null) {
			intel2 = new ContactIntel(person, entity.getMarket());
			Global.getSector().getIntelManager().addIntel(intel2, true, dialog.getTextPanel());
		}
		intel2.develop(null);
		Global.getSoundPlayer().playUISound("ui_contact_developed", 1f, 1f);
		intel2.setState(ContactState.PRIORITY);
		intel2.sendUpdate(null, dialog.getTextPanel());
		return true;
	}

}