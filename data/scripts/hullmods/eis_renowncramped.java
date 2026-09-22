package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

public class eis_renowncramped extends BaseHullMod {

    private boolean AlreadyCame = false;
    private int storedHashCode;
    private int iMissile = 0;
    private static String failIcon = Global.getSettings().getSpriteName("misc", "eis_parryfail");
    private static final String BULLET = "•";

    private static String indeezTitle = Global.getSettings().getString("eis_ironshell", "eis_renowncrampedTitle");
    private static String indeezText1 = Global.getSettings().getString("eis_ironshell", "eis_renowncrampedText1");
    private static String indeezText1b = Global.getSettings().getString("eis_ironshell", "eis_renowncrampedText1b");
    private static String indeezText1c = Global.getSettings().getString("eis_ironshell", "eis_renowncrampedText1c");
    private static String indeezText1d = Global.getSettings().getString("eis_ironshell", "eis_renowncrampedText1d");
    private static final float REDUCTION_PERCENT = 40f;

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (Global.getCombatEngine().isPaused()) {return;}
        if (Global.getCombatEngine().hashCode() != storedHashCode) {
          AlreadyCame = false;
          ship.getMutableStats().getPeakCRDuration().unmodifyMult("eis_renowncramped");
          iMissile = 0;
          storedHashCode = Global.getCombatEngine().hashCode();
        }
        if (!AlreadyCame) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo() && w.getSize() == WeaponSize.MEDIUM) {iMissile += 1;}
                if (w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo() && w.getSize() == WeaponSize.LARGE) {iMissile += 2;}
                if (iMissile > 2) {break;}
            }
        }
        if (iMissile > 2 && !AlreadyCame) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getType() == WeaponAPI.WeaponType.MISSILE && w.usesAmmo()) {
                    int modifiedAmmo = (int) (w.getMaxAmmo()*(1f - 0.4f));
                    w.setMaxAmmo(modifiedAmmo);
                    w.setAmmo(w.getMaxAmmo());
                }
            }
            ship.getMutableStats().getPeakCRDuration().modifyMult("eis_renowncramped", 0.6f);
            if (ship == Global.getCombatEngine().getPlayerShip()) Global.getSoundPlayer().playUISound("cr_playership_warning", 1f, 1f);
        }
	if (ship == Global.getCombatEngine().getPlayerShip() && iMissile > 2) {
            Global.getCombatEngine().maintainStatusForPlayerShip("eis_renowncramped_indeez", failIcon, indeezTitle, indeezText1d, true);
	}
        AlreadyCame = true;
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
    }
    @Override
    public int getDisplaySortOrder() {
        return 101;
    }
}
