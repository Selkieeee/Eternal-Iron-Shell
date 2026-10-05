package com.fs.starfarer.api.impl.campaign.missions.cb;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;

import data.scripts.EISContactFaction;

/**
 * CBDeserter builds the deserter fleet from the giver's faction. For Iron Shell contacts that must stay Iron Shell even
 * after a recruited contact has switched to the player faction, so the contact is temporarily Iron Shell while the fleet is built.
 */
public class EIS_CBDeserter extends CBDeserter {

	// keep the vanilla id so the vanilla offer text rules (CBDeserterOfferDesc) and completion counters still apply
	@Override
	public String getId() {
		return "CBDeserter";
	}

	@Override
	public CustomBountyData createBounty(MarketAPI createdAt, HubMissionWithBarEvent mission, int difficulty, Object bountyStage) {
		PersonAPI person = mission.getPerson();
		String previousFaction = EISContactFaction.enter(person);
		try {
			return super.createBounty(createdAt, mission, difficulty, bountyStage);
		} finally {
			EISContactFaction.exit(person, previousFaction);
		}
	}
}
