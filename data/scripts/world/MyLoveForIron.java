package data.scripts.world;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Voices;

public class MyLoveForIron implements EveryFrameScript {

    private boolean done = false;

    // Guards against startUpgrading() NPEs (BaseIndustry.startUpgrading() crashes if the industry's
    // upgrade id is unset or a mod has removed/renamed the spec it points to).
    private static boolean canUpgrade(Industry industry) {
        String upgradeId = industry.getSpec().getUpgrade();
        return upgradeId != null && Global.getSettings().getIndustrySpec(upgradeId) != null;
    }

    @Override
    public void advance(float amount) {
	if (!done) {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        MarketAPI market = Global.getSector().getEconomy().getMarket("eis_chitagupta");
        MarketAPI market2 = Global.getSector().getEconomy().getMarket("eis_yami");
        if (market != null && "ironshell".equals(market.getFactionId()) && market2 != null && "ironshell".equals(market.getFactionId())) {
            market.getPlanetEntity().setInteractionImage("illustrations", "is_station_illustration");
            for (SectorEntityToken linked : market.getConnectedEntities()) {
                if (!"eis_chitagupta".equals(linked.getId())) {
                    linked.setName(Global.getSettings().getString("eis_ironshell", "eis_lekhanistation"));
                    //linked.setInteractionImage("illustrations", "is_station_illustration");
                    linked.setCustomDescriptionId("station_chitagupta");
                }
            }
            //I did this math of upgrade time in my head, have 59 + IndustryRegularUpgradeTime
            market2.getPlanetEntity().setInteractionImage("illustrations", "is_yami_illustration");
            Industry spaceport = market.getIndustry("spaceport");
            if (spaceport != null && canUpgrade(spaceport)) {spaceport.startUpgrading();
            ((BaseIndustry) spaceport).setBuildProgress(-274f);}
            Industry heavyindustry = market.getIndustry("heavyindustry");
            if (heavyindustry != null && canUpgrade(heavyindustry)) {heavyindustry.startUpgrading();
            ((BaseIndustry) heavyindustry).setBuildProgress(-274f);}
            Industry battlestation = market.getIndustry("battlestation");
            if (battlestation != null && canUpgrade(battlestation)) {battlestation.startUpgrading();
            ((BaseIndustry) battlestation).setBuildProgress(-609);}
            Industry militarybase = market.getIndustry("militarybase");
            if (militarybase != null && canUpgrade(militarybase)) {militarybase.startUpgrading();
            ((BaseIndustry) militarybase).setBuildProgress(-213);}
            //depreciated
            //factionLeader.getStats().setSkillLevel(Skills.SPACE_OPERATIONS, 3);
            //factionLeader.getStats().setSkillLevel(Skills.PLANETARY_OPERATIONS, 3);
            //factionLeader.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 3);
            //heartless.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 3);
            //heartless.getStats().setSkillLevel(Skills.RANGED_SPECIALIZATION, 1);
            //sweetperson.getStats().setSkillLevel(Skills.RANGED_SPECIALIZATION, 1);
            //sweetperson.getStats().setSkillLevel(Skills.WEAPON_DRILLS, 1);
            //sweetperson.getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 1); no selkie u cant even use this...
            //sweetperson.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
            //coffeemom.getStats().setSkillLevel(Skills.RELIABILITY_ENGINEERING, 3);
            PersonAPI factionLeader = Global.getFactory().createPerson();
            factionLeader.setId("eiskimquy");
            factionLeader.setFaction("ironshell");
            factionLeader.setGender(FullName.Gender.FEMALE);
            factionLeader.setPostId(Ranks.POST_FACTION_LEADER);
            factionLeader.setRankId(Ranks.FACTION_LEADER);
            factionLeader.setImportance(PersonImportance.VERY_HIGH);
            factionLeader.getName().setFirst("Kim");
            factionLeader.getName().setLast("Quy");
            factionLeader.setPortraitSprite("graphics/portraits/eis_hegemomy.png");
            factionLeader.addTag("VNSector");
            //Administrator
            factionLeader.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            //Admiral (6)
            factionLeader.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            factionLeader.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
            factionLeader.getStats().setSkillLevel(Skills.OFFICER_TRAINING, 1);
            factionLeader.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            factionLeader.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
            factionLeader.getStats().setSkillLevel(Skills.OFFICER_MANAGEMENT, 1);
            //Distinguished Combatant during the Second AI War (8 + 8)
            factionLeader.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            factionLeader.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            factionLeader.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            factionLeader.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            factionLeader.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
            factionLeader.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            factionLeader.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
            factionLeader.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
            if (Global.getSettings().getSkillSpec("eis_xiv") == null) {throw new RuntimeException(Global.getSettings().getString("eis_ironshell", "EISLoveForIron"));}
            //Amongus + Sad
            factionLeader.getStats().setSkillLevel("eis_xiv", 2);
            factionLeader.getStats().setSkillLevel(Skills.HULL_RESTORATION, 1);
            factionLeader.getStats().setSkillLevel(Skills.NAVIGATION, 1);
            factionLeader.getStats().setSkillLevel(Skills.SENSORS, 1);
            factionLeader.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES, 1);
            factionLeader.getStats().setLevel(9);
            factionLeader.getMemoryWithoutUpdate().set("$nex_preferredAdmin", true);
            factionLeader.getMemoryWithoutUpdate().set("$nex_preferredAdmin_factionId", "ironshell");
            //factionLeader.addTag(Tags.CONTACT_MILITARY);
            factionLeader.addTag("eis_military");
            factionLeader.setVoice(Voices.OFFICIAL);
            ip.addPerson(factionLeader);
            
            PersonAPI heartless = Global.getFactory().createPerson();
            heartless.setId("eisdarren");
            heartless.setFaction("ironshell");
            heartless.setGender(FullName.Gender.MALE);
            heartless.setPostId("CommissionerIS");
            heartless.setRankId("CommissionerIS");
            heartless.setImportance(PersonImportance.VERY_HIGH);
            heartless.getName().setFirst("Darren");
            heartless.getName().setLast("Hartley");
            heartless.setPortraitSprite("graphics/portraits/eis_dunscaith.png");
            heartless.addTag("VNSector");
            //Askonian Veterans during its Crisis (7 + 5)
            heartless.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            heartless.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
            heartless.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 1);
            heartless.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
            heartless.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            heartless.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            heartless.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
            // Attention! I am Head Commissioner here! You will obey or executed! (5)
            heartless.getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 1);
            heartless.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
            heartless.getStats().setSkillLevel(Skills.OFFICER_TRAINING, 1);
            heartless.getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 1);
            heartless.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            //Sad
            heartless.getStats().setSkillLevel(Skills.HULL_RESTORATION, 1);
            heartless.getStats().setSkillLevel(Skills.SENSORS, 1);
            heartless.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES, 1);
            //I will never die until my love's death has been avenged
            heartless.getStats().setSkillLevel("eis_xiv", 2);
            heartless.getStats().setLevel(8); //(20 in actuality)
            heartless.setPersonality(Personalities.AGGRESSIVE);
            heartless.addTag("eis_military");
            heartless.setVoice(Voices.OFFICIAL);
            ip.addPerson(heartless);
            
            PersonAPI sweetperson = Global.getFactory().createPerson();
            sweetperson.setId("eisceleste");
            sweetperson.setFaction("ironshell");
            sweetperson.setGender(FullName.Gender.FEMALE);
            sweetperson.setPostId("ResearcherIS");
            sweetperson.setRankId("ResearcherIS");
            sweetperson.setImportance(PersonImportance.VERY_HIGH);
            sweetperson.getName().setFirst("Caeda");
            sweetperson.getName().setLast("Celeste");
            sweetperson.setPortraitSprite("graphics/portraits/eis_celeste.png");
            sweetperson.addTag("VNSector");
            //Eat Dee's Elf! (7 + 5) I am the last for my House Celeste of Eventide
            sweetperson.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
            sweetperson.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
            sweetperson.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            sweetperson.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            sweetperson.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            sweetperson.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
            sweetperson.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 1);
            //Admiral (4)
            sweetperson.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
            sweetperson.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
            sweetperson.getStats().setSkillLevel(Skills.CYBERNETIC_AUGMENTATION, 1);
            sweetperson.getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 1);
            //Sad
            sweetperson.getStats().setSkillLevel(Skills.HULL_RESTORATION, 1);
            sweetperson.getStats().setSkillLevel(Skills.NAVIGATION, 1);
            sweetperson.getStats().setSkillLevel(Skills.SENSORS, 1);
            sweetperson.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES, 1);
            //Amongus
            sweetperson.getStats().setSkillLevel("eis_xiv", 1);
            sweetperson.getStats().setLevel(8);
            sweetperson.addTag(Tags.CONTACT_SCIENCE);
            sweetperson.addTag("eis_celeste");
            sweetperson.setVoice(Voices.SCIENTIST);
            ip.addPerson(sweetperson);

            PersonAPI warden = Global.getSector().getFaction("ironshell").createRandomPerson(FullName.Gender.FEMALE);
            warden.setId("eissneed");
            warden.setPostId("WardenIS");
            warden.setRankId("WardenIS");
            warden.getName().setFirst("Charlotte");
            warden.getName().setLast("Hex");
            warden.setPortraitSprite("graphics/portraits/eis_charlotte.png");
            warden.addTag("VNSector");
            //Forgive her she's still new... (7 vanilla skill points + 2 npc-only skills)
            warden.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 1);
            warden.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            warden.getStats().setSkillLevel(Skills.FIELD_MODULATION, 1);
            warden.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 1);
            warden.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 1);
            warden.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
            
            //AMONG US (0)
            warden.getStats().setSkillLevel("eis_xiv", 2);
            warden.getStats().setSkillLevel("eis_weaponmastery", 2);
            warden.getStats().setLevel(8);
            warden.setPersonality(Personalities.RECKLESS); //Takes after her mother too much...
            warden.addTag(Tags.CONTACT_UNDERWORLD);
            warden.addTag("eis_military");
            warden.setImportance(PersonImportance.MEDIUM);
            warden.setVoice(Voices.SOLDIER);
            
            ip.addPerson(warden);
            
            PersonAPI coffeemom = Global.getSector().getFaction("ironshell").createRandomPerson(FullName.Gender.FEMALE);
            coffeemom.setId("eisava");
            coffeemom.setPostId("HeadWardenIS");
            coffeemom.setRankId("HeadWardenIS");
            coffeemom.setImportance(PersonImportance.VERY_HIGH);
            coffeemom.getName().setFirst("Ava");
            coffeemom.getName().setLast("Nitia");
            coffeemom.setPortraitSprite("graphics/portraits/eis_ava.png");
            coffeemom.addTag("VNSector");
            coffeemom.getMemoryWithoutUpdate().set("$nex_preferredAdmin", true);
            coffeemom.getMemoryWithoutUpdate().set("$nex_preferredAdmin_factionId", "ironshell");
            //Gacha whatever pilot (7 + 5 elite)
            coffeemom.getStats().setSkillLevel(Skills.HELMSMANSHIP, 1);
            coffeemom.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 1);
            coffeemom.getStats().setSkillLevel(Skills.IMPACT_MITIGATION, 2);
            coffeemom.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
            coffeemom.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            coffeemom.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
            coffeemom.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            //Admiral (4)
            coffeemom.getStats().setSkillLevel(Skills.CREW_TRAINING, 1);
            coffeemom.getStats().setSkillLevel(Skills.FLUX_REGULATION, 1);
            coffeemom.getStats().setSkillLevel(Skills.SUPPORT_DOCTRINE, 1);
            coffeemom.getStats().setSkillLevel(Skills.OFFICER_TRAINING, 1);
            //Administrator (1)
            coffeemom.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);
            //Sad (3)
            coffeemom.getStats().setSkillLevel(Skills.HULL_RESTORATION, 1);//coffeemom.getStats().setSkillLevel(Skills.NAVIGATION, 1);
            coffeemom.getStats().setSkillLevel(Skills.SENSORS, 1);
            coffeemom.getStats().setSkillLevel(Skills.CONTAINMENT_PROCEDURES, 1);
            //Amongus
            coffeemom.getStats().setSkillLevel("eis_xiv", 2);
            coffeemom.getStats().setLevel(14);
            coffeemom.setPersonality(Personalities.AGGRESSIVE);
            //coffeemom.addTag(Tags.CONTACT_MILITARY);
            coffeemom.addTag("eis_military");
            coffeemom.setVoice(Voices.OFFICIAL);
            ip.addPerson(coffeemom);
            
            market.setAdmin(factionLeader);
            market.getCommDirectory().addPerson(factionLeader, 0);
            market.addPerson(factionLeader);
            market.getCommDirectory().addPerson(sweetperson, 2);
            market.addPerson(sweetperson);
            market.getCommDirectory().addPerson(heartless, 1);
            market.addPerson(heartless);
            //market.getMemoryWithoutUpdate().set("$BarCMD_shownEvents", new ArrayList<>());
            market.getMemoryWithoutUpdate().set("$nex_colony_growth_limit", 6);
            market.setImmigrationIncentivesOn(true);
            market2.getCommDirectory().addPerson(coffeemom, 0);
            market2.setAdmin(coffeemom);
            market2.addPerson(coffeemom);
            market2.getCommDirectory().addPerson(warden, 1);
            market2.addPerson(warden);
            market2.getMemoryWithoutUpdate().set("$nex_colony_growth_limit", 5);
            market2.setImmigrationIncentivesOn(true);
            //market2.getMemoryWithoutUpdate().set("$BarCMD_shownEvents", new ArrayList<>());
        }
    }
        done = true;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
