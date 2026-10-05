package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;

/**
 * Iron Shell contacts (everyone carrying the eis_mission tag) stay Iron Shell for mission purposes even after a recruited
 * contact's faction is switched to the player's (FleetData.addOfficer does that). Anyone else keeps their actual faction.
 */
public class EISContactFaction {

    public static final String IRON_SHELL = "ironshell";
    public static final String CONTACT_TAG = "eis_mission";

    public static boolean isContact(PersonAPI person) {
        return person != null && person.hasTag(CONTACT_TAG);
    }

    public static String getId(PersonAPI person) {
        if (isContact(person)) return IRON_SHELL;
        return person.getFaction().getId();
    }

    public static FactionAPI get(PersonAPI person) {
        if (isContact(person)) return Global.getSector().getFaction(IRON_SHELL);
        return person.getFaction();
    }

    /**
     * For vanilla code that reads person.getFaction() internally and can't be edited: temporarily makes a contact
     * Iron Shell. Returns the faction id to hand back to exit(), or null if nothing was changed.
     * Always pair with exit() in a finally block.
     */
    public static String enter(PersonAPI person) {
        if (!isContact(person)) return null;
        String previous = person.getFaction().getId();
        if (IRON_SHELL.equals(previous)) return null;
        person.setFaction(IRON_SHELL);
        return previous;
    }

    public static void exit(PersonAPI person, String previous) {
        if (previous != null && person != null) person.setFaction(previous);
    }
}
