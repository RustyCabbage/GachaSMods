package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

import static data.hullmods.GachaSMods_randomSMod.*;
import static data.scripts.GachaSMods_ModPlugin.*;
import static data.scripts.GachaSMods_Utils.*;

public class GachaSMods_removeSMods extends BaseHullMod {

    private final Logger log = Global.getLogger(GachaSMods_removeSMods.class);

    private final String
            HULLMOD_CONFLICT = getString("gachaConflict"), // "Incompatible with "
            NO_SMODS = getString("removeInapplicable"); // "Ship is at the built-in hullmod limit"

    // yeah, yeah they only instantiate once per class blah blah blah
    // they're updated/cleared any time they're used anyways
    private final ArrayList<String> removedMods = new ArrayList<>(); // cleared every time upon confirm or cancel
    private final ArrayList<String> originalSMods = new ArrayList<>(); // cleared every time upon confirm or cancel
    private int numSP = -1; // constant per character, updated every time it's used

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        // initialize all these dumb variables
        ShipVariantAPI variant = ship.getVariant();
        Random random = new Random();
        String seedKey = (ship.getFleetMemberId() != null) ? SAVED_SEED + "_" + ship.getFleetMemberId() : null;
        long savedSeed;
        int minSModsToRemove = boundMinMaxSModsToRemove(MIN_REMOVED_SMODS);
        int maxSModsToRemove = Math.max(minSModsToRemove, boundMinMaxSModsToRemove(MAX_REMOVED_SMODS));

        //frick u tim
        if (!ship.getVariant().getHullMods().toArray()[ship.getVariant().getHullMods().size()-1].toString().equals(spec.getId())) {
            ship.getVariant().removeMod(spec.getId());
            ship.getVariant().addMod(spec.getId());
        }

        // todo: gives bonus xp depending on number of s mods removed. bad!
        // if the ship's at the max number of s-mods, add 1 to the max so you can s-mod this one
        // can't remember why the placeholder stuff, but it was important
        if ((ship.getVariant().getSMods().size() == Misc.getMaxPermanentMods(ship)
                && !ship.getVariant().hasHullMod(PLACEHOLDER_ID + "0"))
                || ship.getVariant().getSMods().size() > Misc.getMaxPermanentMods(ship)) {
            ship.getMutableStats().getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, 1);
        }

        // fix for s-modding being possible when only placeholders are present
        // key is that it doesn't do anything if there are both placeholders and non-placeholders
        // since there are cases where it both should and should not be possible to s-mod
        // e.g. pre-confirmation: no s-modding, post-confirmation with additional hullmods that can be removed: yes
        if (!variant.getSMods().isEmpty()) {
            boolean noPlaceholders = true, onlyPlaceholders = true;
            for (String sModId : variant.getSMods()) {
                if (sModId.startsWith(PLACEHOLDER_ID)) {
                    noPlaceholders = false;
                } else {
                    onlyPlaceholders = false;
                }
            }
            if (noPlaceholders) {
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
            }
            if (onlyPlaceholders) {
                spec.addTag(Tags.HULLMOD_NO_BUILD_IN);
            }
        }

        // main script that fires when the hullmod has been s-modded
        if (variant.getSMods().contains(spec.getId())) {
            // anti-save scum stuff
            if (NO_SAVE_SCUMMING && !TRUE_RANDOM) {
                savedSeed = getSeed(ship, seedKey);
                random = new Random(savedSeed);
                //log.info("Loaded seed " + savedSeed);
            } else {
                random = new Random();
            }
            numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();

            // dumb way to continue checks during confirmation screen, because otherwise the script stops when it's removed
            // also I don't make it a non-s-mod perma-mod because I copy-pasted from randomSMod
            variant.removePermaMod(spec.getId());
            variant.addMod(spec.getId());

            //log.info("Removing s-mods...");
            WeightedRandomPicker<String> sModRemovalPicker = new WeightedRandomPicker<>(random);
            // tree set or upon cancellation the s-mods will be added in a different order, messing up the which mods are removed
            TreeSet<String> sModIds = new TreeSet<>(variant.getSMods());
            for (String sModId : sModIds) {
                originalSMods.add(sModId);
                adjustHullModForSModding(sModId, ship.getHullSize(), false, null);
                if (!sModId.startsWith(MOD_ID)) { // need to not add placeholders into the sModRemovalPicker if repeated s-mod removal is done
                    sModRemovalPicker.add(sModId);
                }
            }

            // need this so that you can actually confirm the s-mod process
            adjustHullModForSModding(PLACEHOLDER_ID + "0", ship.getHullSize(), false, null);
            if (variant.hasHullMod(PLACEHOLDER_ID + "0")) {
                variant.removeMod(PLACEHOLDER_ID + "0"); // do I need to do this? I think not but bleh
                variant.removePermaMod(PLACEHOLDER_ID + "0"); // do I need to do this? I think not but bleh
            }
            variant.addPermaMod(PLACEHOLDER_ID + "0", true);

            // todo: do not allow removing more s-mods than you have story points (probably not worth the hassle)
            // remove at least min(numSMods, minSModsToRemove from settings)
            // remove at most min(numSMods, maxSModsToRemove from settings)
            // I think I added like four fail-safes here, so it's super redundant but oh well
            int tempMin = Math.min(minSModsToRemove, sModRemovalPicker.getItems().size());
            int tempMax = Math.max(tempMin, Math.min(maxSModsToRemove, sModRemovalPicker.getItems().size()));
            //log.info("tempMin: " + tempMin + ", tempMax: " + tempMax);
            int numSModsToRemove = tempMin + random.nextInt(Math.max(1, tempMax - tempMin + 1));
            log.info("Removing " + numSModsToRemove + " s-mods");

            for (int i = 1; i <= numSModsToRemove; i++) {
                String pickId = sModRemovalPicker.pick(random); // can't select seed with pickAndRemove() wtheck (tho I get that it probably makes no sense to do so)
                sModRemovalPicker.remove(pickId);
                variant.removePermaMod(pickId);
                removedMods.add(pickId); // may need to add them back if we cancel, so remember them
                log.info("Removed " + pickId);

                // we need to add [# removed s-mods] placeholders or else you can't press confirm
                // hidden hullmods don't add to the counter so this shouldn't be necessary if we remove a hidden hullmod
                if (!Global.getSettings().getHullModSpec(pickId).isHidden()) {
                    adjustHullModForSModding(PLACEHOLDER_ID + i, ship.getHullSize(), false, null);
                    variant.addPermaMod(PLACEHOLDER_ID + i, true);
                }
            }
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
                for (String originalSModId : originalSMods) {
                    restoreHullMod(originalSModId);
                    // for some reason restoreHullMod will un-smod hidden hullmods, so we have to re-smod every hullmod, not just the removed ones
                    variant.addPermaMod(originalSModId, true);
                    //log.info("Restored and re-added " + originalSModId);
                }
                /*
                for (String removedModId : removedMods) {
                    variant.addPermaMod(removedModId, true);
                    log.info("Re-added " + removedModId);
                }

                 */
                for (int i = 0; i <= maxSModsToRemove; i++) {
                    if (variant.hasHullMod(PLACEHOLDER_ID + i)) {
                        variant.removeMod(PLACEHOLDER_ID + i);
                        restoreHullMod(PLACEHOLDER_ID + i);
                        //log.info("Removing hullmod " + PLACEHOLDER_ID + i);
                    }
                }
                removedMods.clear();
                originalSMods.clear();
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
            }
            // check confirmation by number of story points
            if (Global.getSector().getPlayerPerson().getStats().getStoryPoints() < numSP) {
                log.info("Confirmed s-mod removal");
                numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();

                for (String originalSModId : originalSMods) {
                    restoreHullMod(originalSModId);
                }
                // it gets removed upon switching away to another ship, but will remain with any action that checks the variant
                // (i.e. anything that isn't swapping to another ship or leaving the refit screen)
                // it's very necessary to remove this mod for save compatibility, so we'll do our best
                // edit: todo this might not actually be necessary anymore
                for (int i = 0; i <= maxSModsToRemove; i++) {
                    variant.removePermaMod(PLACEHOLDER_ID + i);
                    restoreHullMod(PLACEHOLDER_ID + i);
                }
                removedMods.clear();
                originalSMods.clear();
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);

                // update the seed. we need this because you can undergo the removal process multiple times in a row
                // I don't actually know if this is necessary because changing the number of s-mods will change the picker and stuff but just in case
                savedSeed = random.nextLong();
                Global.getSector().getPersistentData().put(seedKey, savedSeed);
            }
            // check if there are hidden s-mods
            int numHiddenSMods = variant.getSMods().size() - Misc.getCurrSpecialMods(variant);
            for (String sModId : variant.getSMods()) {
                if (sModId.startsWith(PLACEHOLDER_ID)) {
                    numHiddenSMods--;
                }
            }
            if (numHiddenSMods > 0) {
                variant.addPermaMod(HIDDEN_FIX_ID, false);
            } else {
                variant.removePermaMod(HIDDEN_FIX_ID);
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship.getVariant().getSMods().size() == 0) {
            return false;
        }
        return !shipHasOtherModInCategory(ship, spec.getId(), MOD_ID);
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

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float PAD = 10f;
        if (LOADING_FAILED) {
            tooltip.addSectionHeading(getString("loadingFailedHeading"),
                    Misc.getNegativeHighlightColor(), Misc.getGrayColor(), Alignment.MID, PAD);
            tooltip.addPara(getString("attemptingToLoad"), PAD, Misc.getNegativeHighlightColor(), attemptingToLoad);
        }

        int minSModsToRemove = boundMinMaxSModsToRemove(MIN_REMOVED_SMODS);
        int maxSModsToRemove = Math.max(minSModsToRemove, boundMinMaxSModsToRemove(MAX_REMOVED_SMODS));
        tooltip.addPara(getString("removeBetween"), PAD, Misc.getHighlightColor(),
                Integer.toString(minSModsToRemove), Integer.toString(maxSModsToRemove));

        if (!ONLY_NOT_HIDDEN_HULLMODS || TRUE_RANDOM) {
            tooltip.addSectionHeading(getString("removeHiddenHeading"), Alignment.MID, PAD);
            tooltip.addPara(getString("removeHiddenSPCost1") + getString("removeHiddenSPCost2"), PAD,
                    Misc.getNegativeHighlightColor(), getString("removeHiddenSPCost1"));
            tooltip.addPara(getString("removeHiddenDMods1") + getString("removeHiddenDMods2"), PAD,
                    Misc.getHighlightColor(), getString("removeHiddenDMods1"));
        }
    }

    // hard limits 'cuz setting it dynamically would be very annoying
    public int boundMinMaxSModsToRemove(int minOrMaxSModsToRemove) {
        if (minOrMaxSModsToRemove < 0) {
            minOrMaxSModsToRemove = 0;
        } else if (minOrMaxSModsToRemove > 10) {
            minOrMaxSModsToRemove = 10;
        }
        return minOrMaxSModsToRemove;
    }
}