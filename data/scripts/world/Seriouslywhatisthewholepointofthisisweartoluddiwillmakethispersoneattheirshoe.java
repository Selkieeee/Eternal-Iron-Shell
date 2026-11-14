package data.scripts.world;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorGeneratorPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.eis_nex;
import static data.scripts.eis_modPlugin.IRONSTANDSETERNAL;

public class Seriouslywhatisthewholepointofthisisweartoluddiwillmakethispersoneattheirshoe implements SectorGeneratorPlugin {
    @Override
    public void generate(SectorAPI sector) {
        Global.getSector().addScript(new MyLoveForIron());
        Makethefactionrelationshipgobrrrrt(sector);
    }

    public static void Makethefactionrelationshipgobrrrrt(SectorAPI sector) {
        FactionAPI iron = sector.getFaction(IRONSTANDSETERNAL);
        //sector.getFaction(Factions.PLAYER).setRelationship(iron.getId(), RepLevel.COOPERATIVE);
        iron.setRelationship(Factions.HEGEMONY, 1.00f); //Tax-exempted.
        if (data.scripts.eis_modPlugin.getEISBooleanSetting("GreaterHegemony", true)) {
            eis_nex.createGreaterHegemony(true); // Nexerelin alliance; without Nexerelin the two are simply treated as allied
        }
        iron.setRelationship(Factions.PLAYER, Global.getSector().getPlayerFaction().getRelationship(Factions.HEGEMONY)); //Be a good little taxpayer.
        // Starting relationships: from Nexerelin's faction config, or the same table read directly when Nexerelin isn't installed.
        eis_nex.applyStartRelationships(iron);
    }
}