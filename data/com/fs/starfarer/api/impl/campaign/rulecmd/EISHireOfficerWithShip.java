package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * EISHireOfficerWithShip <person id> <ship variant id>
 *
 * Combines DA Mistral's officer-join pattern (Mistral_RecruitRamiel - adds the person to the
 * player fleet's officer roster) with Iron Shell's own ship-granting convention (EISAddShip -
 * clones the variant, gives it a proper random ship name, adds it to the fleet), and additionally
 * assigns the recruited person as that new ship's captain (matching Volantian Reclamation
 * Initiative's VRIAddSuperSpecialShipAndOfficer pattern for a combined recruit-with-ship offer).
 * Prints the same highlighted "gained officer"/"gained ship" text vanilla uses for comparable
 * milestones. Deliberately does not touch the person's comm directory or market presence (unlike
 * Volantian's version) - joining as an officer here doesn't remove Charlotte from further
 * VNSector conversations.
 */
public class EISHireOfficerWithShip extends BaseCommandPlugin {

    private static final String JOIN_SOUND = "ui_contact_developed";

    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.size() < 2) return false;

        String personId = params.get(0).getString(memoryMap);
        String variantId = params.get(1).getString(memoryMap);

        PersonAPI person = Global.getSector().getImportantPeople().getPerson(personId);
        if (person == null) return false;

        String shipName = Global.getSector().getFaction("ironshell").pickRandomShipName();
        FleetMemberAPI ship = Global.getFactory().createFleetMember(FleetMemberType.SHIP, Global.getSettings().getVariant(variantId).clone());
        ship.setShipName(shipName);
        ship.setCaptain(person);

        Global.getSector().getPlayerFleet().getFleetData().addFleetMember(ship);
        Global.getSector().getPlayerFleet().getFleetData().addOfficer(person);

        Global.getSoundPlayer().playUISound(JOIN_SOUND, 1f, 1f);
        AddRemoveCommodity.addOfficerGainText(person, dialog.getTextPanel());
        AddRemoveCommodity.addFleetMemberGainText(ship.getVariant(), dialog.getTextPanel());

        return true;
    }
}
