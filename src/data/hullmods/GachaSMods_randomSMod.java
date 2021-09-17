package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

public class GachaSMods_randomSMod extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_randomSMod.class);

    public static String TRULY_RANDOM = "eurobeat";
    public static String AT_S_MOD_LIMIT = "Ship is at the built-in hullmod limit";

    // yeah yeah they only instantiate once per class blah blah blah it should be fine
    // they're updated/cleared any time they're used anyways
    private static final ArrayList<String> addedMods = new ArrayList<>();
    private static int numSP = -1;

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        ShipVariantAPI variant = ship.getVariant();

        // main script that fires when the hullmod has been s-modded
        if (variant.getSMods().contains(spec.getId())) {
            numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
            variant.removePermaMod(spec.getId());
            // dumb way to continue checks during confirmation screen, because otherwise the script stops when it's removed
            // also I don't make it a non-s-mod perma-mod because then you can't s-mod multiple times in a row
            variant.addMod(spec.getId());

            String chosenSMod;
            if (ship.getName().toLowerCase().equals(TRULY_RANDOM)) {
                chosenSMod = getRandomHullmod(ship, false, false);
            } else {
                chosenSMod = getRandomHullmod(ship, true, true);
            }
            if (chosenSMod == null) {
                //return;
                chosenSMod = HullMods.DEFECTIVE_MANUFACTORY; // this being the error hullmod mildly amuses me
            }
            if (variant.hasHullMod(chosenSMod)) {
                variant.removeMod(chosenSMod);
            }
            variant.addPermaMod(chosenSMod, true);
            addedMods.add(chosenSMod);
        }

        // will usually do nothing, exceptions being when it has been s-modded / s-modding has been cancelled
        // had to be done because the confirmation screen also fires this script
        // meaning weird shit would happen when you cancel the s-modding process
        if (variant.hasHullMod(spec.getId())) {
            // in the case that the player cancels the s-mod process, the potential s-mods will become regular mods
            // which must be removed
            if (!Collections.disjoint(variant.getNonBuiltInHullmods(),addedMods)) {
                for (String addedMod : addedMods) {
                    if (variant.getNonBuiltInHullmods().contains(addedMod)) {
                        variant.removeMod(addedMod);
                        //log.info("removing not-s-modded mod " + addedMod);
                    }
                }
                addedMods.clear();
                // variant.removeMod(spec.getId());
            }
            // need a way to clear the addedMods list when you confirm
            if (Global.getSector().getPlayerPerson().getStats().getStoryPoints() < numSP) {
                numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
                addedMods.clear();
                // variant.removeMod(spec.getId()); // for some reason this doesn't work - gets re-added some point after this step
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (Misc.getCurrSpecialMods(ship.getVariant()) > Misc.getMaxPermanentMods(ship)) {
            return false;
        }
        // prevent re-adding when it's at the exact amount
        // apparently if !isApplicableToShip, variant.addMod() does not work
        // so this separate rule was needed if you would hit the max # of s-mods, blocking the reinstallation of the hullmod
        // and thereby blocking the checking mechanism
        if (Misc.getCurrSpecialMods(ship.getVariant()) == Misc.getMaxPermanentMods(ship) && !ship.getVariant().hasHullMod(spec.getId())) {
            return false;
        }
        return true;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (Misc.getCurrSpecialMods(ship.getVariant()) >= Misc.getMaxPermanentMods(ship)) {
            return AT_S_MOD_LIMIT;
        }
        return null;
    }

    @Override
    public Color getNameColor() {
        return Misc.FLOATY_ARMOR_DAMAGE_COLOR;
    }

    // to do: add weights or something, that'd be pretty pog
    // like inverse of their OP, so you'd have a 4x better chance of getting expanded mags than heavy armor for capital ships
    public String getRandomHullmod(ShipAPI ship, boolean onlyKnownHullmods, boolean onlyApplicableHullmods) {
        ShipVariantAPI variant = ship.getVariant();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        if (onlyKnownHullmods) {
            for (String hullmodId : Global.getSector().getPlayerFaction().getKnownHullMods()) {
                HullModSpecAPI hullmod = Global.getSettings().getHullModSpec(hullmodId);
                // so like I should consider if it's reasonable to only s-mod dock-only mods when actually at a spaceport
                // I don't want to make it super tedious by making people leave the spaceport for better odds tho
                if (!variant.getPermaMods().contains(hullmodId)
                        && !variant.getHullSpec().getBuiltInMods().contains(hullmodId)
                        && !hullmodId.equals(spec.getId())) {
                    if (onlyApplicableHullmods && !hullmod.getEffect().isApplicableToShip(ship)) {
                        continue;
                    }
                    picker.add(hullmodId);
                    //log.info(hullmodId + " added to picker");
                }
            }
        } else {
            for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) {
                String hullmodId = hullmod.getId();
                if (!variant.getPermaMods().contains(hullmodId)
                        && !variant.getHullSpec().getBuiltInMods().contains(hullmodId)
                        //&& hullmod.isHiddenEverywhere() // what if shardspawner were an option
                        && hullmod != spec) {
                    if (onlyApplicableHullmods && !hullmod.getEffect().isApplicableToShip(ship)) {
                        continue;
                    }
                    picker.add(hullmod.getId());
                    //log.info(hullmod.getId() + " added to picker");
                }
            }
        }
        String pick = picker.pick();
        //log.info("Picked " + pick);
        return pick;
    }
}
