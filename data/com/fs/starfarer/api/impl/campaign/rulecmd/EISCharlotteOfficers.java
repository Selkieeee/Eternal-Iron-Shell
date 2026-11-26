package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.SkillSelectorDialogDelegate;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.AvailableOfficer;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent.SkillPickPreference;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Charlotte's officer search, a copy of the vanilla personnel officer (OfficerManagerEvent "showSkillPicker"):
 *
 * EISCharlotteOfficers hasMarket - true if the dialog has a market to list candidates in (disable row check)
 * EISCharlotteOfficers PICK      - skill picker limited to one skill; 2-3 level 1 officers with that skill
 * EISCharlotteOfficers XIV       - one level 1 officer whose only skill is Iron Heritage (the 1 SP option; the Story
 *                                  Point is spent by the game through SetStoryOption on the option row)
 *
 * Officers are Iron Shell and always female, take the personality picked earlier ($EISCharlottePickedPersonality in
 * local memory), and are listed for hire in the comm directory of the market the dialog is at, like vanilla's.
 * Both routes then set $EISCharlotteOfficerTimeout (30-60 days, local memory) and fire EISCharlotteOfficersPicked.
 * Unlike vanilla (whose candidates are pruned within 1-3 days), the candidates stay listed for the same duration as
 * the timeout.
 */
public class EISCharlotteOfficers extends BaseCommandPlugin {

    public static final String FACTION_ID = "ironshell";
    public static final String IRON_HERITAGE = "eis_xiv";
    public static final String PERSONALITY_KEY = "$EISCharlottePickedPersonality";
    public static final String TIMEOUT_KEY = "$EISCharlotteOfficerTimeout";
    public static final String PICKED_TRIGGER = "EISCharlotteOfficersPicked";
    public static final int OFFICER_LEVEL = 1;
    public static final int MAX_SKILLS = 1;

    @Override
    public boolean execute(String ruleId, final InteractionDialogAPI dialog, List<Token> params, final Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty()) return false;
        String action = params.get(0).getString(memoryMap);

        if ("hasMarket".equals(action)) {
            return getMarket(dialog) != null;
        }
        if ("PICK".equals(action)) {
            if (getMarket(dialog) == null) return false;
            dialog.showSkillPickerDialog(new SkillSelectorDialogDelegate() {
                @Override
                public void skillsSelected(List<SkillSpecAPI> selectedSkills) {
                    MarketAPI market = getMarket(dialog);
                    if (market == null) return;
                    Random random = new Random();
                    int num = 2 + random.nextInt(2);
                    float duration = rollDuration(random);
                    String personality = getPickedPersonality(memoryMap);
                    for (int i = 0; i < num; i++) {
                        PersonAPI officer = createOfficer(personality, random);
                        swapInSkills(officer, selectedSkills);
                        list(officer, market, duration);
                    }
                    finish(dialog, memoryMap, duration);
                }

                @Override
                public void skillSelectionCancelled() {
                }

                @Override
                public String getTitle() {
                    return null;
                }

                @Override
                public List<SkillSpecAPI> getSkills() {
                    // same filter as vanilla's personnel officer; Iron Heritage is npc_only/deprecated, so it never shows
                    List<SkillSpecAPI> skills = new ArrayList<>();
                    for (String id : Global.getSettings().getSortedSkillIds()) {
                        SkillSpecAPI spec = Global.getSettings().getSkillSpec(id);
                        if (!spec.isCombatOfficerSkill()) continue;
                        if (spec.hasTag(Skills.TAG_DEPRECATED)) continue;
                        if (spec.hasTag(Skills.TAG_NPC_ONLY) && !spec.hasTag(Skills.TAG_AI_CORE)) continue;
                        if (spec.hasTag(Skills.TAG_NO_AI_CORE)) continue;
                        if (spec.hasTag(Skills.TAG_PLAYER_ONLY)) continue;
                        skills.add(spec);
                    }
                    return skills;
                }

                @Override
                public int getMaxSelectable() {
                    return MAX_SKILLS;
                }
            });
            return true;
        }
        if ("XIV".equals(action)) {
            MarketAPI market = getMarket(dialog);
            if (market == null) return false;
            Random random = new Random();
            float duration = rollDuration(random);
            PersonAPI officer = createOfficer(getPickedPersonality(memoryMap), random);
            // replace the generated skill: Iron Heritage is the officer's only skill
            for (SkillLevelAPI sl : officer.getStats().getSkillsCopy()) {
                if (sl.getLevel() > 0) officer.getStats().setSkillLevel(sl.getSkill().getId(), 0);
            }
            officer.getStats().setSkillLevel(IRON_HERITAGE, 1);
            list(officer, market, duration);
            finish(dialog, memoryMap, duration);
            return true;
        }
        return false;
    }

    private static MarketAPI getMarket(InteractionDialogAPI dialog) {
        SectorEntityToken target = dialog.getInteractionTarget();
        return target == null ? null : target.getMarket();
    }

    private static float rollDuration(Random random) {
        return 30f + random.nextFloat() * 30f;
    }

    private static String getPickedPersonality(Map<String, MemoryAPI> memoryMap) {
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) return null;
        String p = local.getString(PERSONALITY_KEY);
        if (Personalities.TIMID.equals(p) || Personalities.CAUTIOUS.equals(p) || Personalities.STEADY.equals(p)
                || Personalities.AGGRESSIVE.equals(p) || Personalities.RECKLESS.equals(p)) {
            return p;
        }
        return null;
    }

    /** A level 1 Iron Shell officer with no elite skills, made female (name and portrait from a female Iron Shell person). */
    private static PersonAPI createOfficer(String personality, Random random) {
        FactionAPI faction = Global.getSector().getFaction(FACTION_ID);
        PersonAPI officer = OfficerManagerEvent.createOfficer(faction, OFFICER_LEVEL, SkillPickPreference.ANY,
                false, null, false, false, -1, random);
        if (officer.getGender() != Gender.FEMALE) {
            PersonAPI female = faction.createRandomPerson(Gender.FEMALE, random);
            officer.setGender(Gender.FEMALE);
            officer.setName(female.getName());
            officer.setPortraitSprite(female.getPortraitSprite());
        }
        officer.setPostId(Ranks.POST_OFFICER_FOR_HIRE);
        if (personality != null) officer.setPersonality(personality);
        return officer;
    }

    /** Same skill swap as vanilla: keep picked skills the officer already has, otherwise replace an unwanted skill. */
    private static void swapInSkills(PersonAPI officer, List<SkillSpecAPI> selectedSkills) {
        OUTER:
        for (SkillSpecAPI curr : new ArrayList<>(selectedSkills)) {
            for (SkillLevelAPI sl : officer.getStats().getSkillsCopy()) {
                if (sl.getLevel() >= 1 && curr.getId().equals(sl.getSkill().getId())) continue OUTER;
            }
            for (SkillLevelAPI sl : officer.getStats().getSkillsCopy()) {
                if (sl.getLevel() > 0 && !selectedSkills.contains(sl.getSkill())) {
                    officer.getStats().setSkillLevel(sl.getSkill().getId(), 0);
                    break;
                }
            }
            officer.getStats().setSkillLevel(curr.getId(), 1);
        }
    }

    /** Lists the officer for hire at the market, with vanilla's hiring fee (level x 2000) and salary. */
    private static void list(PersonAPI officer, MarketAPI market, float duration) {
        List<OfficerManagerEvent> listeners = Global.getSector().getListenerManager().getListeners(OfficerManagerEvent.class);
        if (listeners.isEmpty()) return;
        int salary = (int) Misc.getOfficerSalary(officer, false);
        AvailableOfficer available = new AvailableOfficer(officer, market.getId(), officer.getStats().getLevel() * 2000, salary);
        available.timeRemaining = duration;
        listeners.get(0).addAvailable(available);
    }

    private static void finish(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap, float duration) {
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local != null) local.set(TIMEOUT_KEY, true, duration);
        FireBest.fire(null, dialog, memoryMap, PICKED_TRIGGER);
    }
}
