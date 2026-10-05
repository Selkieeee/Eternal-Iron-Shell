package com.fs.starfarer.api.impl.campaign.missions.cb;

import java.util.ArrayList;
import java.util.List;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty.Stage;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator.CustomBountyData;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission.ConditionChecker;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.util.Misc;

import data.scripts.EISContactFaction;

/**
 * CBEnemyStation picks stations hostile to the giver's faction and cancels the bounty when that hostility ends. For Iron Shell
 * contacts both must keep using Iron Shell even after a recruited contact has switched to the player faction.
 */
public class EIS_CBEnemyStation extends CBEnemyStation {

	// keep the vanilla id so the vanilla offer text rules (CBEnemyStationOfferDesc) and completion counters still apply
	@Override
	public String getId() {
		return "CBEnemyStation";
	}

	// getFrequency and createBounty both go through this, so the contact is Iron Shell for the whole station search
	// and stations beyond EISMilitaryCustomBounty.MAX_TARGET_DISTANCE_LY of the contact's market are never offered
	@Override
	public List<CampaignFleetAPI> getStations(HubMissionWithBarEvent mission, int difficulty) {
		PersonAPI person = mission.getPerson();
		List<CampaignFleetAPI> stations;
		String previousFaction = EISContactFaction.enter(person);
		try {
			stations = super.getStations(mission, difficulty);
		} finally {
			EISContactFaction.exit(person, previousFaction);
		}
		MarketAPI origin = person == null ? null : person.getMarket();
		if (origin == null) return stations;
		List<CampaignFleetAPI> inRange = new ArrayList<CampaignFleetAPI>();
		for (CampaignFleetAPI station : stations) {
			float distance = Misc.getDistanceLY(origin.getLocationInHyperspace(), station.getLocationInHyperspace());
			if (distance <= EISMilitaryCustomBounty.MAX_TARGET_DISTANCE_LY) inRange.add(station);
		}
		return inRange;
	}

	// same as CBEnemyStation.notifyAccepted except hostilities are checked against Iron Shell instead of person.getFaction(),
	// which is evaluated every day and would flip to the player faction after recruitment
	@Override
	public void notifyAccepted(MarketAPI createdAt, HubMissionWithBarEvent mission, CustomBountyData data) {
		MarketAPI market = data.market;
		Object stage = data.custom2;
		if (data.difficulty <= 7) {
			mission.triggerCreateLargePatrolAroundMarket(market, stage, 0f);
			mission.triggerCreateSmallPatrolAroundMarket(market, stage, 0f);
		} else if (data.difficulty <= 9) {
			mission.triggerCreateLargePatrolAroundMarket(market, stage, 0f);
			mission.triggerCreateMediumPatrolAroundMarket(market, stage, 0f);
			mission.triggerCreateSmallPatrolAroundMarket(market, stage, 0f);
		} else {
			mission.triggerCreateLargePatrolAroundMarket(market, stage, 0f);
			mission.triggerCreateLargePatrolAroundMarket(market, stage, 0f);
			mission.triggerCreateMediumPatrolAroundMarket(market, stage, 0f);
			mission.triggerCreateSmallPatrolAroundMarket(market, stage, 0f);
		}
		EISHostilitiesEndedChecker checker = new EISHostilitiesEndedChecker(mission.getPerson(), market);
		mission.connectWithCustomCondition(Stage.BOUNTY, Stage.FAILED_NO_PENALTY, checker);
		mission.setStageOnCustomCondition(Stage.FAILED_NO_PENALTY, checker);
	}

	// named static class (not anonymous) because the mission keeps it in its saved stage connections
	public static class EISHostilitiesEndedChecker implements ConditionChecker {
		public PersonAPI person;
		public MarketAPI market;
		public EISHostilitiesEndedChecker(PersonAPI person, MarketAPI market) {
			this.person = person;
			this.market = market;
		}
		public boolean conditionsMet() {
			return !EISContactFaction.get(person).isHostileTo(market.getFaction());
		}
	}
}
