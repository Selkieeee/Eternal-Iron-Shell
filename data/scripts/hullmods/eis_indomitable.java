package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

public class eis_indomitable extends BaseHullMod {
    
    // One hullmod object is shared by every ship that has it, so the per-ship state lives in the combat engine's custom data instead of in fields here.
    private static final String DATA_KEY = "eis_indomitable_data";
    private static String failIcon = Global.getSettings().getSpriteName("misc", "eis_parryfail");
    //private static String pogIcon = "graphics/icons/hullsys/burn_drive.png";
    private static final String BULLET = "\u2022";
    
    private static String indeezTitle = Global.getSettings().getString("eis_ironshell", "eis_indeezTitle");
    private static String indeezText1 = Global.getSettings().getString("eis_ironshell", "eis_indeezText1");
    private static String indeezText1b = Global.getSettings().getString("eis_ironshell", "eis_indeezText1b");
    private static String indeezText1c = Global.getSettings().getString("eis_ironshell", "eis_indeezText1c");
    private static String indeezText1d = Global.getSettings().getString("eis_ironshell", "eis_indeezText1d");
    //private static String indeezTitle2 = Global.getSettings().getString("eis_ironshell", "eis_indeezTitle2");
    //private static String indeezText1a = Global.getSettings().getString("eis_ironshell", "eis_indeezText1a");
    private static final float REDUCTION_PERCENT = 40f;
    //private static final float REDUCTION_PERCENT2 = 66f;
    
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused()) {return;}
        String key = DATA_KEY + "_" + ship.getId();
        IndomitableData data = (IndomitableData) engine.getCustomData().get(key);
        if (data == null) {
            // first frame of this ship in this battle
            data = new IndomitableData();
            engine.getCustomData().put(key, data);
            ship.getMutableStats().getPeakCRDuration().unmodifyMult("eis_indomitable");
        }
        if (!data.alreadyCame) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo() && w.getSize() == WeaponSize.MEDIUM) {data.missileCount += 1;}
                if (w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo() && w.getSize() == WeaponSize.LARGE) {data.missileCount += 2;}
                if (data.missileCount > 2) {break;}
            }
        }
        if (data.missileCount > 2 && !data.alreadyCame) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo()) {
                    int modifiedAmmo = (int) (w.getMaxAmmo()*(1f - 0.4f));
                    w.setMaxAmmo(modifiedAmmo);
                    w.setAmmo(w.getMaxAmmo());
                }
            }
            ship.getMutableStats().getPeakCRDuration().modifyMult("eis_indomitable", 0.6f);
            if (ship == Global.getCombatEngine().getPlayerShip()) Global.getSoundPlayer().playUISound("cr_playership_warning", 1f, 1f);
        }
	if (ship == Global.getCombatEngine().getPlayerShip() && data.missileCount > 2) {
            Global.getCombatEngine().maintainStatusForPlayerShip("eis_indeez", failIcon, indeezTitle, indeezText1d, true);
	}
        data.alreadyCame = true;
    }

    private static class IndomitableData {
        boolean alreadyCame = false;
        int missileCount = 0;
    }
    
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float HEIGHT = 50f;
        float PAD = 10f;
        Color YELLOW = new Color(241,199,0);

        TooltipMakerAPI indeez = tooltip.beginImageWithText(failIcon, HEIGHT);
        indeez.addPara(indeezTitle, 0f, YELLOW, indeezTitle);
        indeez.addPara(indeezText1, 0f);
        indeez.addPara(indeezText1b, 0f, Misc.getNegativeHighlightColor(), BULLET, "" + Math.round(REDUCTION_PERCENT) + "%");
        indeez.addPara(indeezText1c, 0f, Misc.getNegativeHighlightColor(), BULLET, "" + Math.round(REDUCTION_PERCENT) + "%");
        tooltip.addImageWithText(PAD);
        /*TooltipMakerAPI indeez2 = tooltip.beginImageWithText(pogIcon, HEIGHT);
        indeez2.addPara(indeezTitle2, 0f, YELLOW, indeezTitle2);
        indeez2.addPara(indeezText1a, 0f, Misc.getPositiveHighlightColor(), BULLET, "" + Math.round(REDUCTION_PERCENT2) + "%");
        tooltip.addImageWithText(PAD);*/
    }
    @Override
    public int getDisplaySortOrder() {
        return 101;
    }
}


