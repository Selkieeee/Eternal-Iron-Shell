package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.awt.Color;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin.ReputationAdjustmentResult;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.MissionCompletionRep;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActionEnvelope;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepActions;
import com.fs.starfarer.api.impl.campaign.CoreReputationPlugin.RepRewards;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.bases.PirateBaseIntel;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class EISPirateSystemBounty extends HubMissionWithSearch implements FleetEventListener {

	public static float BOUNTY_DAYS = 90f;
	public static float BASE_BOUNTY = 1000f;
	//public static float VS_STATION_BONUS = 50000f;
	
	public static class BountyResult {
		public int payment;
		public float fraction;
		public ReputationAdjustmentResult repFaction;
		public ReputationAdjustmentResult repPerson;
		public BountyResult(int payment, float fraction, ReputationAdjustmentResult repPerson, ReputationAdjustmentResult repFaction) {
			this.payment = payment;
			this.fraction = fraction;
			this.repFaction = repFaction;
			this.repPerson = repPerson;
		}
		
	}
	
	
	public static enum Stage {
		BOUNTY,
		DONE,
	}

	protected StarSystemAPI system;
	protected MarketAPI market;
	protected FactionAPI faction;
	protected FactionAPI enemy;
	protected int baseBounty;
	protected BountyResult latestResult;
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		
		PersonAPI person = getPerson();
		if (person == null) return false;
		
		if (Factions.PIRATES.equals(person.getFaction().getId())) return false;
		
		if (!setPersonMissionRef(person, "$psb_ref")) {
			return false;
		}
		
		//requireMarketFaction(Factions.PIRATES);
		requireMarketFactionCustom(ReqMode.ALL, Factions.CUSTOM_MAKES_PIRATE_BASES);
		requireMarketMemoryFlag(PirateBaseIntel.MEM_FLAG, true);
		requireMarketFactionNot(person.getFaction().getId());
		requireMarketHidden();
		requireMarketIsMilitary();
		preferMarketInDirectionOfOtherMissions();
		market = pickMarket();
		
		if (market == null || market.getStarSystem() == null) return false;
		if (!setMarketMissionRef(market, "$psb_ref")) { // just to avoid targeting the same base with multiple bounties
			return false;
		}
		
                makeImportant(market, "$psb_target", Stage.BOUNTY);
		system = market.getStarSystem();

		faction = person.getFaction();
		enemy = market.getFaction();
		
		baseBounty = getRoundNumber(BASE_BOUNTY * getRewardMult()); 
		
		setStartingStage(Stage.BOUNTY);
		setSuccessStage(Stage.DONE);
		setNoAbandon();
		setNoRepChanges();
		
		connectWithDaysElapsed(Stage.BOUNTY, Stage.DONE, BOUNTY_DAYS);
		
		addTag(Tags.INTEL_BOUNTY);
		
		
		int numPirates = 2 + genRandom.nextInt(3);

		FleetSize [] sizes = new FleetSize [] {
				FleetSize.MEDIUM,
				FleetSize.MEDIUM,
				FleetSize.LARGE,
				FleetSize.VERY_LARGE,
		};
		
		for (int i = 0; i < numPirates; i++) {
			FleetSize size = sizes[i % sizes.length];
			beginWithinHyperspaceRangeTrigger(system, 3f, false, Stage.BOUNTY);
			triggerCreateFleet(size, FleetQuality.DEFAULT, market.getFactionId(), FleetTypes.PATROL_MEDIUM, system);
			triggerAutoAdjustFleetStrengthMajor();
			triggerSetPirateFleet();
			triggerSpawnFleetNear(system.getCenter(), null, null);
			triggerOrderFleetPatrol(system, true, Tags.STATION, Tags.JUMP_POINT);
			endTrigger();
		}
		
		return true;
	}

	protected void updateInteractionDataImpl() {
		set("$psb_manOrWoman", getPerson().getManOrWoman());
		set("$psb_baseBounty", Misc.getWithDGS(baseBounty));
		set("$psb_days", "" + (int) BOUNTY_DAYS);
		set("$psb_systemName", system.getNameWithLowercaseType());
                set("$psb_systemNameShort", system.getNameWithLowercaseTypeShort());
		set("$psb_baseName", market.getName());
		set("$psb_dist", getDistanceLY(market));
	}
	
	@Override
	public void addDescriptionForCurrentStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		float pad = 3f;
		Color h = Misc.getHighlightColor();
		Color tc = getBulletColorForMode(ListInfoMode.IN_DESC);
		if (currentStage == Stage.BOUNTY) {
			float elapsed = getElapsedInCurrentStage();
			int d = (int) Math.round(BOUNTY_DAYS - elapsed);
			PersonAPI person = getPerson();
			
			String locStr = Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty16") + market.getStarSystem().getNameWithLowercaseType();
			
			// Plain fragment concatenation, no %s left in any of the pieces - previously passed enemy.getBaseUIColor()/
			// getPersonNamePrefix() as if for highlight substitution, but EISPirateSystemBounty1 had its own unrelated
			// %s placeholders with no matching args, throwing MissingFormatArgumentException. EISPirateSystemBounty3
			// is the sentence closer here (", a pirate base.") - it previously held unrelated isEnding()/DONE text
			// that has moved to EISPirateSystemBounty2, where it actually belongs.
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty0") + locStr + Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty1") + market.getName() + Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty3"),
					opad);

			if (isEnding()) {
				info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty2"), opad);
				return;
			}

			bullet(info);
			// EISPirateSystemBounty17 ("%s base reward") replaces EISPirateSystemBounty4 here - 4 is "remaining" (no
			// %s of its own), which belongs in the addDays() text below instead, not this credit-amount line.
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty17"), opad, tc, h, Misc.getDGSCredits(baseBounty));
			// EISPirateSystemBounty5 has its own 2 %s placeholders (for faction/person); addDays() only ever fills one
			// %s of its own (the day count), so those 2 must be pre-filled here or String.format throws
			// MissingFormatArgumentException once addDays() builds its combined string internally.
			// EISPirateSystemBounty4 ("remaining") is prepended per its own comment ("function addDays para") -
			// previously misused on the credit-amount line above instead, where its lack of a %s silently dropped
			// the credit value.
			String bountyRepText = Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty4") + ". " +
					String.format(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty5"), faction.getDisplayNameWithArticle(), person.getNameString());
			addDays(info, bountyRepText, d, tc);
			unindent(info);

		} else if (currentStage == Stage.DONE) {
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty2"), opad);
		}

		if (latestResult != null) {
			//Color color = faction.getBaseUIColor();
			//Color dark = faction.getDarkUIColor();
			//info.addSectionHeading("Most Recent Reward", color, dark, Alignment.MID, opad);
			// EISPirateSystemBounty6 ("Most recent bounty payment:") is this section's actual header text per its
			// own value - previously misused inside the (now-removed) reputation-recap paragraph above instead.
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty6"), opad);
			bullet(info);
			// EISPirateSystemBounty7 ("%s received") and 8 ("%s share based on damage dealt") restored to their
			// original %s-templated form - they're used here via the highlight-substituting addPara overload, unlike
			// their previous (fixed-for-a-different-bug) use in the plain-concatenation reputation paragraph.
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty7"), pad, tc, h, Misc.getDGSCredits(latestResult.payment));
			if (Math.round(latestResult.fraction * 100f) < 100f) {
				info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty8"), 0f, tc, h,
						"" + (int) Math.round(latestResult.fraction * 100f) + "%");
			}
			if (latestResult.repPerson != null) {
				CoreReputationPlugin.addAdjustmentMessage(latestResult.repPerson.delta, null, getPerson(), 
														  null, null, info, tc, false, 0f);
			}
			if (latestResult.repFaction != null) {
				CoreReputationPlugin.addAdjustmentMessage(latestResult.repFaction.delta, faction, null, 
														  null, null, info, tc, false, 0f);
			}
			unindent(info);
		}
	}

	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		
		boolean isUpdate = getListInfoParam() != null;
		if (isUpdate && latestResult == getListInfoParam()) {
			// Same reassignment as addDescriptionForCurrentStage: 7/8 (not 10/11) are the %s-templated result lines.
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty7"), pad, tc, h, Misc.getDGSCredits(latestResult.payment));
			if (Math.round(latestResult.fraction * 100f) < 100f) {
				info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty8"), 0f, tc, h,
						"" + (int) Math.round(latestResult.fraction * 100f) + "%");
			}
			if (latestResult.repPerson != null) {
				CoreReputationPlugin.addAdjustmentMessage(latestResult.repPerson.delta, null, getPerson(), 
													  	null, null, info, tc, isUpdate, 0f);
			}
			if (latestResult.repFaction != null) {
				CoreReputationPlugin.addAdjustmentMessage(latestResult.repFaction.delta, faction, null, 
													  	null, null, info, tc, isUpdate, 0f);
			}
			return true;
		}
		
		if (currentStage == Stage.BOUNTY) {
			float elapsed = getElapsedInCurrentStage();
			int d = (int) Math.round(BOUNTY_DAYS - elapsed);
			
			// Same reassignment as addDescriptionForCurrentStage: 17 (not 4) is the %s-templated credit-amount line.
			info.addPara(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty17"), pad, tc, h, Misc.getDGSCredits(baseBounty));
			// Same fix as addDescriptionForCurrentStage: EISPirateSystemBounty5's 2 %s need filling before addDays()
			// appends its own day-count %s, or String.format throws MissingFormatArgumentException. Entry4
			// ("remaining") is prepended here too, matching the fix above.
			PersonAPI person = getPerson();
			String bountyRepText = Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty4") + ". " +
					String.format(Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty5"), faction.getDisplayNameWithArticle(), person.getNameString());
			addDays(info, bountyRepText, d, tc);
			return true;
		} else if (currentStage == Stage.DONE) {
			return false;
		}
		return false;
	}
	
	public String getPostfixForState() {
		// EISPirateSystemBounty9 (" - Over", commented "[Pirate System Bounty] - Over") - previously misused as a
		// bare label inside the Most Recent Reward section instead; this method itself called the never-created
		// EISPirateSystemBounty12.
		if (currentStage == Stage.DONE) return Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty9");
		return super.getPostfixForState();
	}

	@Override
	public String getBaseName() {
		// EISPirateSystemBounty10 ("Pirate Fleet Bounty", commented "getBaseName") - previously misused as the
		// %s-less credit-amount line in the Most Recent Reward section; this method itself called the
		// never-created EISPirateSystemBounty13.
		return Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty10");
	}

	protected String getMissionTypeNoun() {
		// EISPirateSystemBounty11 ("bounty", commented "getMissionTypeNoun") - previously misused as the %s-less
		// damage-share line in the Most Recent Reward section; this method itself called the never-created
		// EISPirateSystemBounty14.
		return Global.getSettings().getString("eis_ironshell", "EISPirateSystemBounty11");
	}
	

	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		return getMapLocationFor(market.getPrimaryEntity());
	}
	
	@Override
	public void acceptImpl(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		Global.getSector().getListenerManager().addListener(this);
//		AddRemoveCommodity.addCreditsLossText(cost, dialog.getTextPanel());
//		Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(cost);
//		adjustRep(dialog.getTextPanel(), null, RepActions.MISSION_SUCCESS);
//		addPotentialContacts(dialog);
	}
	
	@Override
	protected void notifyEnding() {
		Global.getSector().getListenerManager().removeListener(this);
		super.notifyEnding();
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
	}

	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
		if (isEnded() || isEnding()) return;
		
		if (!battle.isPlayerInvolved()) return;
		
		if (!Misc.isNear(primaryWinner, market.getLocationInHyperspace())) return;
		
		int payment = 0;
		float fpDestroyed = 0;
		for (CampaignFleetAPI otherFleet : battle.getNonPlayerSideSnapshot()) {
			if (enemy != otherFleet.getFaction()) continue;
			
			float bounty = 0;
			for (FleetMemberAPI loss : Misc.getSnapshotMembersLost(otherFleet)) {
				float mult = Misc.getSizeNum(loss.getHullSpec().getHullSize());
				bounty += mult * baseBounty;
				fpDestroyed += loss.getFleetPointCost();
			}
			
			payment += (int) (bounty * battle.getPlayerInvolvementFraction());
		}
	
		if (payment > 0) {
			Global.getSector().getPlayerFleet().getCargo().getCredits().add(payment);
			
			float repFP = (int)(fpDestroyed * battle.getPlayerInvolvementFraction());
			
			float fDelta = 0f;
			float pDelta = 0f;
			if (repFP < 30) {
				fDelta = RepRewards.TINY;
				pDelta = RepRewards.TINY;
			} else if (repFP < 70) {
				fDelta = RepRewards.SMALL;
				pDelta = RepRewards.SMALL;
			} else {
				fDelta = RepRewards.SMALL;
				pDelta = RepRewards.MEDIUM;
			}
			
			MissionCompletionRep completionRepPerson = new MissionCompletionRep(
											pDelta, getRewardLimitPerson(), 0, null);
			MissionCompletionRep completionRepFaction = new MissionCompletionRep(
											fDelta, getRewardLimitFaction(), 0, null);
			
			boolean addContacts = latestResult == null;
			latestResult = new BountyResult(payment, battle.getPlayerInvolvementFraction(), null, null);
			if (pDelta != 0) {
				ReputationAdjustmentResult rep = Global.getSector().adjustPlayerReputation(
						new RepActionEnvelope(RepActions.MISSION_SUCCESS, completionRepPerson,
								null, true, false), 
								getPerson());
				latestResult.repPerson = rep;
			}

			if (completionRepFaction.successDelta != 0) {
				ReputationAdjustmentResult rep = Global.getSector().adjustPlayerReputation(
						new RepActionEnvelope(RepActions.MISSION_SUCCESS, completionRepFaction,
								null, true, false), 
								getPerson().getFaction().getId());
				latestResult.repFaction = rep;
			}
			
			sendUpdateIfPlayerHasIntel(latestResult, false);
			
			if (addContacts) {
				addPotentialContacts(null);
			}
		}
	}
	
}











