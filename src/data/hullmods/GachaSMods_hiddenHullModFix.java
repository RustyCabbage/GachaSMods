package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;

import static data.scripts.GachaSMods_ModPlugin.ALLOW_STANDARD_SMODS;
import static data.scripts.GachaSMods_Utils.PLACEHOLDER_ID;

public class GachaSMods_hiddenHullModFix extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_hiddenHullModFix.class);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        int numHiddenSMods = ship.getVariant().getSMods().size() - Misc.getCurrSpecialMods(ship.getVariant());
        //log.info(String.format("Ship has %d hidden s-mods", numHiddenSMods));
        if (numHiddenSMods <= 0) {
            ship.getVariant().removePermaMod(spec.getId());
        } else {
            ship.getMutableStats().getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, -numHiddenSMods);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
