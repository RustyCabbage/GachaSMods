package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

import static data.hullmods.GachaSMods_randomSMod.*;
import static data.scripts.GachaSMods_Utils.*;


public class GachaSMods_removeSMods extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_removeSMods.class);

    private final String HULLMOD_CONFLICT = getString("gachaConflict");
    private final String NO_SMODS = getString("removeInapplicable"); // "Ship is at the built-in hullmod limit"
    private final static int MAX_REMOVABLE_SMODS = 3;

    // yeah, yeah they only instantiate once per class blah blah blah
    // they're updated/cleared any time they're used anyways
    private final ArrayList<String> removedMods = new ArrayList<>(); //cleared every time
    private int numSP = -1; //constant per character

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        // if at 1 s mod and max 2, we need max to stay at 2 after beginning s-modding
        // if at 2 s-mods and max 2, we need max to increase to 3
        if ((ship.getVariant().getSMods().size() == Misc.getMaxPermanentMods(ship)
                && !ship.getVariant().hasHullMod(PLACEHOLDER_ID + "0"))
                || ship.getVariant().getSMods().size() > Misc.getMaxPermanentMods(ship)) {
            ship.getMutableStats().getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, 1);
        }

        // initialize all these dumb variables
        ShipVariantAPI variant = ship.getVariant();
        Random random = new Random();

        // main script that fires when the hullmod has been s-modded
        if (variant.getSMods().contains(spec.getId())) {
            // anti-save scum stuff
            if (ship.getFleetMemberId() != null && Global.getSettings().getBoolean(NO_SAVE_SCUMMING_SETTING)) {
                String seedKey = SAVED_SEED + "_" + ship.getFleetMemberId();
                long savedSeed = getSeed(ship, seedKey);
                random = new Random(savedSeed);
                //log.info("Loaded seed" + savedSeed);
            }
            numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
            variant.removePermaMod(spec.getId());
            // dumb way to continue checks during confirmation screen, because otherwise the script stops when it's removed
            // also I don't make it a non-s-mod perma-mod because then you can't s-mod multiple times in a row
            variant.addMod(spec.getId());

            log.info("Removing s-mods...");
            int numSMods = variant.getSMods().size();
            WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(random);
            picker.addAll(variant.getSMods());

            // min 1 because I don't hate you all that much (can't say I won't change my mind)
            // max 3 because I don't love you all that much (and I don't hate myself that much)
            // todo: make this adjustable
            int numSModsToRemove = 1 + random.nextInt(Math.min(MAX_REMOVABLE_SMODS, numSMods));
            log.info("Removing " + numSModsToRemove + " s-mods");
            for (int i = 1; i <= numSModsToRemove; i++) {
                String pickId = picker.pick(random);
                picker.remove(pickId);
                variant.removePermaMod(pickId);
                removedMods.add(pickId); // may need to add them back if we cancel, so remember them
                log.info("Removing " + pickId);

                // we need to add # removed s-mods + 1 or else you can't press confirm
                adjustHullModForSModding(PLACEHOLDER_ID + i, ship.getHullSize(), false, random);
                variant.addPermaMod(PLACEHOLDER_ID + i, true);
            }

            // need this so that you can actually confirm the s-mod process
            adjustHullModForSModding(PLACEHOLDER_ID + "0", ship.getHullSize(), false, random);
            if (variant.hasHullMod(PLACEHOLDER_ID + "0")) {
                variant.removeMod(PLACEHOLDER_ID + "0");
            }
            variant.addPermaMod(PLACEHOLDER_ID + "0", true);
            // blocks the s-modding process, so you can't do it more than once
            spec.addTag(Tags.HULLMOD_NO_BUILD_IN);
        }

        // will usually do nothing, exceptions being when it has been s-modded / s-modding has been cancelled
        // had to be done because the confirmation screen also fires this script
        // meaning weird stuff would happen when you cancel the s-modding process
        // in the case that the player cancels the s-mod process, re-add the removed hullmods
        if (variant.getNonBuiltInHullmods().contains(spec.getId())) {
            if (variant.getNonBuiltInHullmods().contains(PLACEHOLDER_ID + "0")) {
                log.info("Cancelled s-mod removal");
                for (String removedModId : removedMods) {
                    variant.addPermaMod(removedModId, true);
                    log.info("Re-added " + removedModId);
                }

                for (int i = 0; i <= MAX_REMOVABLE_SMODS; i++) {
                    if (variant.hasHullMod(PLACEHOLDER_ID + i)) {
                        variant.removeMod(PLACEHOLDER_ID + i);
                        restoreHullMod(PLACEHOLDER_ID + i);
                        //log.info("Removing hullmod " + PLACEHOLDER_ID + i);
                    }
                }
                removedMods.clear();
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
            }
            // check confirmation by number of story points
            if (Global.getSector().getPlayerPerson().getStats().getStoryPoints() < numSP) {
                log.info("Confirmed s-mod removal");
                numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();

                // it gets removed upon switching away to another ship, but will remain with any action that checks the variant
                // (i.e. anything that isn't swapping to another ship or leaving the refit screen)
                // it's very necessary to remove this mod for save compatibility, so we'll do our best
                for (int i = 0; i <= MAX_REMOVABLE_SMODS; i++) {
                    variant.removePermaMod(PLACEHOLDER_ID + i);
                    restoreHullMod(PLACEHOLDER_ID + i);
                }
                removedMods.clear();
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship.getVariant().getSMods().size() == 0) {
            return false;
        }
        if (shipHasOtherModInCategory(ship, spec.getId(), MOD_ID)) {
            return false;
        }
        return true;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().getSMods().size() == 0) {
            return NO_SMODS;
        }
        if (shipHasOtherModInCategory(ship, spec.getId(), MOD_ID)) {
            return HULLMOD_CONFLICT + getOtherModsInCategory(ship, spec.getId(), MOD_ID);
        }
        return null;
    }

    @Override
    public Color getNameColor() {
        return Misc.getHighlightColor();
    }
}
