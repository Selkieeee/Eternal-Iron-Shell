package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.Global;
import java.awt.Color;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import data.scripts.EISContactFaction;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepRewards;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class EISSpySatDeployment extends HubMissionWithSearch {
	public static float PROB_PATROL_AROUND_TARGET = 0.1f; //from 0.5f
	
	public static float MISSION_DAYS = 120f;
	
	public static enum Stage {
		DEPLOY,
		COMPLETED,
		FAILED,
	}
	
	protected MarketAPI market;
	protected SectorEntityToken target;
	
	// Faction rep goes to Iron Shell even after a recruited contact has switched to the player faction.
	@Override
	protected void adjustRep(TextPanelAPI textPanel, HubMissionResult result, RepActions action) {
		String previousFaction = EISContactFaction.enter(getPerson());
		try {
			super.adjustRep(textPanel, result, action);
		} finally {
			EISContactFaction.exit(getPerson(), previousFaction);
		}
	}

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		PersonAPI person = getPerson();
		if (person == null) return false;
		
		
		if (!setPersonMissionRef(person, "$ssat_ref")) {
			return false;
		}
		
		requireMarketIsNot(createdAt);
		requireMarketLocationNot(createdAt.getContainingLocation());
		requireMarketFactionNotPlayer();
		requireMarketFactionNot(EISContactFaction.getId(person));requireMarketFactionNot("hegemony");
		requireMarketFactionCustom(ReqMode.NOT_ANY, Factions.CUSTOM_ALLOWS_TRANSPONDER_OFF_TRADE);
		//requireMarketMilitary();
		requireMarketNotHidden();
		requireMarketNotInHyperspace();
                //preferMarketMilitary();
                preferMarketNotMilitary();
		preferMarketInDirectionOfOtherMissions();
		market = pickMarket();
		
		if (market == null) return false;
		
		target = spawnMissionNode(
					new LocData(EntityLocationType.ORBITING_PARAM, market.getPrimaryEntity(), market.getStarSystem()));
		if (!setEntityMissionRef(target, "$ssat_ref")) return false;
		
		makeImportant(target, "$ssat_target", Stage.DEPLOY);
                setMapMarkerNameColor(market.getTextColorForFactionOrPlanet());
		
		setStartingStage(Stage.DEPLOY);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		
		setStageOnMemoryFlag(Stage.COMPLETED, target, "$ssat_completed");
		setTimeLimit(Stage.FAILED, MISSION_DAYS, null);
		
//		int sizeModifier = market.getSize() * 10000;
//		setCreditReward(10000 + sizeModifier, 30000 + sizeModifier);
		setCreditReward(CreditReward.AVERAGE, market.getSize());
		// reputation on completion: person +6 / faction +5 (failure penalties unchanged: person -2 / faction -1)
		setRepChanges(0.06f, RepRewards.SMALL, 0.05f, RepRewards.TINY);
		
		if (rollProbability(PROB_PATROL_AROUND_TARGET)) {
                        triggerCreateSmallPatrolAroundMarket(market, Stage.DEPLOY, 1f);
		}
		
		return true;
	}
	
	protected void updateInteractionDataImpl() {
		set("$ssat_barEvent", isBarEvent());
		set("$ssat_underworld", getPerson().hasTag(Tags.CONTACT_UNDERWORLD));
		set("$ssat_manOrWoman", getPerson().getManOrWoman());
		set("$ssat_reward", Misc.getWithDGS(getCreditsReward()));
		
		set("$ssat_personName", getPerson().getNameString());
		set("$ssat_systemName", market.getStarSystem().getNameWithLowercaseTypeShort());
		set("$ssat_marketName", market.getName());
		set("$ssat_marketOnOrAt", market.getOnOrAt());
		set("$ssat_dist", getDistanceLY(market));
	}
	
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.DEPLOY) {
			// EISSpySatDeployment1 is already a complete "Deploy a spysat in orbit of %s in the %s." template
			// (matches its own comment) - previously concatenated with EISSpySatDeployment2/3, which belong to
			// addNextStepText and getBaseName respectively and don't belong in this sentence at all.
			info.addPara(String.format(Global.getSettings().getString("eis_ironshell", "EISSpySatDeployment1"), market.getName(), market.getStarSystem().getNameWithLowercaseTypeShort()), opad);
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.DEPLOY) {
			// EISSpySatDeployment2 is already the complete "Deploy spysat near %s in the %s." template for this
			// exact method - was previously wired to the never-created EISSpySatDeployment4 plus itself as a
			// mid-sentence connector fragment.
			info.addPara(String.format(Global.getSettings().getString("eis_ironshell", "EISSpySatDeployment2"), market.getName(), market.getStarSystem().getNameWithLowercaseTypeShort()), tc, pad);
			return true;
		}
		return false;
	}

	@Override
	public String getBaseName() {
		// EISSpySatDeployment3 ("SpySat Deployment", commented "getBaseName") - previously misused as the
		// trailing fragment inside addDescriptionForNonEndStage; getBaseName itself called the never-created
		// EISSpySatDeployment5.
		return Global.getSettings().getString("eis_ironshell", "EISSpySatDeployment3");
	}
	
}

