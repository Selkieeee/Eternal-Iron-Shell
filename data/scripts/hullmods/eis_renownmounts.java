package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;

public class eis_renownmounts extends BaseHullMod {
   private static String text1 = Global.getSettings().getString("eis_ironshell", "EISRenownMount");
   private static String weapon = Global.getSettings().getString("eis_ironshell", "eis_weapon");
   private static String bonus = Global.getSettings().getString("eis_ironshell", "eis_bonus");
   private static String total = Global.getSettings().getString("eis_ironshell", "eis_total");
   private static String ballistic = Global.getSettings().getString("eis_ironshell", "eis_ballistic");
   private static String composite = Global.getSettings().getString("eis_ironshell", "eis_composite");
   private int fluxDissipationPerOP = 3;

   public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
      int compositeBallisticOP = 0;
      if (stats.getVariant().getHullSpec().getBuiltInMods().contains("eis_aquila")) {
         for (String slot : stats.getVariant().getNonBuiltInWeaponSlots()) {
            if (stats.getVariant().getSlot(slot).getWeaponType() == WeaponType.COMPOSITE
               && stats.getVariant().getWeaponSpec(slot).getType() == WeaponType.BALLISTIC) {
               compositeBallisticOP = (int)(compositeBallisticOP + stats.getVariant().getWeaponSpec(slot).getOrdnancePointCost(null, stats));
            }
         }

         if (stats.getVariant().hasTag("eis_skill")) {
            stats.getFluxDissipation().modifyFlat(id, this.fluxDissipationPerOP * compositeBallisticOP);
         } else {
            stats.getFluxDissipation().modifyFlat(id, (this.fluxDissipationPerOP + 1) * compositeBallisticOP);
         }
      }
   }

   public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
      if (ship.getCaptain() != null && ship.getCaptain().getStats().hasSkill("eis_xiv")) {
         ship.getVariant().addTag("eis_skill");
      } else {
         ship.getVariant().removeTag("eis_skill");
      }
   }

   public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
      return false;
   }

   public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
      if (ship != null) {
         if (ship.getVariant().hasTag("eis_renown")) {
            tooltip.addPara(
               String.format(text1, ballistic, composite, "+" + Misc.getRoundedValue(this.fluxDissipationPerOP + 1)),
               10.0F,
               new Color[]{Misc.getBallisticMountColor(), Misc.MOUNT_COMPOSITE, Misc.getPositiveHighlightColor()},
               new String[]{ballistic, composite, "+" + Misc.getRoundedValue(this.fluxDissipationPerOP + 1)}
            );
            int totalBonus = 0;
            tooltip.beginTable(
               Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), 15.0F, new Object[]{weapon, 300.0F, bonus, 50.0F}
            );

            for (String slot : ship.getVariant().getNonBuiltInWeaponSlots()) {
               int slotBonus = 0;
               if (ship.getVariant().getSlot(slot).getWeaponType() == WeaponType.COMPOSITE
                  && ship.getVariant().getWeaponSpec(slot).getType() == WeaponType.BALLISTIC) {
                  slotBonus = (int)(
                     slotBonus
                        + ship.getMutableStats().getFluxDissipation().getMult()
                           * ship.getVariant().getWeaponSpec(slot).getOrdnancePointCost(null, ship.getMutableStats())
                           * (this.fluxDissipationPerOP + 1)
                  );
                  tooltip.addRow(
                     new Object[]{
                        Alignment.LMID,
                        Misc.getBallisticMountColor(),
                        ship.getVariant().getWeaponSpec(slot).getWeaponName(),
                        Alignment.RMID,
                        Misc.getPositiveHighlightColor(),
                        "+" + slotBonus
                     }
                  );
               }

               totalBonus += slotBonus;
            }

            tooltip.addRow(
               new Object[]{Alignment.LMID, Misc.getPositiveHighlightColor(), total, Alignment.RMID, Misc.getPositiveHighlightColor(), "+" + totalBonus}
            );
            tooltip.addTable("", 0, 10.0F);
         } else {
            tooltip.addPara(
               String.format(text1, ballistic, composite, "+" + Misc.getRoundedValue(this.fluxDissipationPerOP)),
               2.0F,
               new Color[]{Misc.getBallisticMountColor(), Misc.MOUNT_COMPOSITE, Misc.getHighlightColor()},
               new String[]{ballistic, composite, "+" + Misc.getRoundedValue(this.fluxDissipationPerOP)}
            );
            int totalBonus = 0;
            tooltip.beginTable(
               Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), 15.0F, new Object[]{weapon, 300.0F, bonus, 50.0F}
            );

            for (String slot : ship.getVariant().getNonBuiltInWeaponSlots()) {
               int slotBonus = 0;
               if (ship.getVariant().getSlot(slot).getWeaponType() == WeaponType.COMPOSITE
                  && ship.getVariant().getWeaponSpec(slot).getType() == WeaponType.BALLISTIC) {
                  slotBonus = (int)(slotBonus + ship.getVariant().getWeaponSpec(slot).getOrdnancePointCost(null, ship.getMutableStats()) * this.fluxDissipationPerOP);
                  tooltip.addRow(
                     new Object[]{
                        Alignment.LMID,
                        Misc.getBallisticMountColor(),
                        ship.getVariant().getWeaponSpec(slot).getWeaponName(),
                        Alignment.RMID,
                        Misc.getPositiveHighlightColor(),
                        "+" + slotBonus
                     }
                  );
               }

               totalBonus += slotBonus;
            }

            tooltip.addRow(
               new Object[]{Alignment.LMID, Misc.getPositiveHighlightColor(), total, Alignment.RMID, Misc.getPositiveHighlightColor(), "+" + totalBonus}
            );
            tooltip.addTable("", 0, 10.0F);
         }
      }
   }

   public int getDisplaySortOrder() {
      return 169;
   }

   public String getDescriptionParam(int index, HullSize hullSize, ShipAPI ship) {
      return null;
   }
}
