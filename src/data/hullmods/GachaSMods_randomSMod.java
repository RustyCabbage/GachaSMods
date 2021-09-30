package data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import static data.scripts.GachaSMods_Utils.*;

public class GachaSMods_randomSMod extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_randomSMod.class);
    // tags used to fix up hullmods I messed with
    private final String HULLMOD_CONFLICT = getString("gachaConflict");
    private static final String SHOULD_BE_HIDDEN = MOD_ID + "_" + "shouldBeHidden";
    private static final String SHOULD_BE_DMOD = MOD_ID + "_" + "shouldBeDMod";
    private static final String SHOULD_BE_HIDDEN_EVERYWHERE = MOD_ID + "_" + "shouldBeHiddenEverywhere";
    // used to randomize OP costs
    private static final int RANDOM_COST_MIN = 2;
    private static final int RANDOM_COST_MAX = 10;
    private static final String FRIGATE_CODE = "ff";
    private static final String DESTROYER_CODE = "dd";
    private static final String CRUISER_CODE = "cc";
    private static final String CAPITAL_SHIP_CODE = "bb";
    // if your hullmod costs over 9999 i'll kill you
    private static final String OP_COST_REGEX = "^(" + MOD_ID + ")" + "_"
            + "(" + FRIGATE_CODE + "|" + DESTROYER_CODE + "|" + CRUISER_CODE + "|" + CAPITAL_SHIP_CODE + ")" + "_"
            + "([0-9]$|[1-9][0-9]$|[1-9][0-9][0-9]$|[1-9][0-9][0-9][0-9])$";
    // externalized strings
    private final String RANDOM_OVERRIDE = getString("randomOverride"); // "eurobeat"
    private final String AT_S_MOD_LIMIT = getString("randomInapplicable"); // "Ship is at the built-in hullmod limit"

    // yeah, yeah they only instantiate once per class blah blah blah
    // they're updated/cleared any time they're used anyways
    private final ArrayList<String> addedMods = new ArrayList<>(); //cleared every time
    private int numSP = -1; //constant per character

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        // initialize all these dumb variables
        ShipVariantAPI variant = ship.getVariant();
        Map<String, Object> saveData = Global.getSector().getPersistentData();
        String seedKey = null;
        String loadKey = null;
        String tempSeedKey = null;
        Random random = new Random();
        // these will be replaced if noSaveScumming is enabled, else they don't matter
        long savedSeed = random.nextLong();
        long tempSeed = savedSeed;
        boolean shouldLoadSeed;

        // this stuff should only have a unique result once per ship so shouldn't cause any issues
        if (ship.getFleetMemberId() != null) {
            seedKey = SAVED_SEED + "_" + ship.getFleetMemberId();
            loadKey = SHOULD_LOAD_SEED + "_" + ship.getFleetMemberId();
            tempSeedKey = TEMP_SEED + "_" + ship.getFleetMemberId();
            if (saveData.get(loadKey) == null) {
                saveData.put(loadKey, true);
                //log.info("initializing load key");
            }
            if (saveData.get(tempSeedKey) == null) {
                saveData.put(tempSeedKey, tempSeed);
                //log.info("initializing seed key");
            } else if (Global.getSettings().getBoolean(NO_SAVE_SCUMMING_SETTING)) {
                tempSeed = (long) saveData.get(tempSeedKey); // gotta do this or there are problems when saving the temp seed
            }
        }

        // main script that fires when the hullmod has been s-modded
        if (variant.getSMods().contains(spec.getId())) {
            // anti-save scum stuff
            // essentially it had to be done in this roundabout way to make certain of 3 things:
            // 1. seed changes every time you confirm (so if you restore a d-mod you can still get new s-mods)
            // 2. s-modding one at a time has the same effect as s-modding everything at once
            // 3. s-modding is consistent when taking the hullmod on/off and stuff
            //    (so it can only update when in the process of s-modding so there's a bunch of ugly initialization stuff)
            if (Global.getSettings().getBoolean(NO_SAVE_SCUMMING_SETTING)) {
                savedSeed = getSeed(ship, seedKey);
                // replace temp seed with the saved seed if told to (i.e. just after a cancellation process), else continue the temp seed chain
                shouldLoadSeed = (boolean) saveData.get(loadKey);
                if (shouldLoadSeed) {
                    tempSeed = savedSeed;
                    //log.info("Loaded seed " + tempSeed);
                    saveData.put(loadKey, false);
                } else {
                    tempSeed = (long) saveData.get(tempSeedKey);
                }
                random = new Random(tempSeed); // create new random based on the temp seed. it will either be the saved seed or the continued temp seed chain
                tempSeed = random.nextLong(); // shuffles to the next long every time the hullmod is s-modded.
                saveData.put(tempSeedKey, tempSeed); // save to continue the chain
                //log.info("Next seed " + tempSeed);
            }

            numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
            variant.removePermaMod(spec.getId());
            // dumb way to continue checks during confirmation screen, because otherwise the script stops when it's removed
            // also I don't make it a non-s-mod perma-mod because then you can't s-mod multiple times in a row
            variant.addMod(spec.getId());

            // choose the hullmod to s-mod
            String chosenSModId;
            if (ship.getName().equalsIgnoreCase(RANDOM_OVERRIDE) || Global.getSettings().getBoolean(TRUE_RANDOM_SETTING)) {
                random = new Random();
                chosenSModId = getRandomHullmod(ship, random, false, false, false);
            } else {
                boolean onlyKnownHullMods = Global.getSettings().getBoolean(ONLY_KNOWN_HULLMODS_SETTING);
                boolean onlyNotHiddenHullmods = Global.getSettings().getBoolean(ONLY_NOT_HIDDEN_HULLMODS_SETTING);
                boolean onlyApplicableHullmods = Global.getSettings().getBoolean(ONLY_APPLICABLE_HULLMODS_SETTING);
                chosenSModId = getRandomHullmod(ship, random, onlyKnownHullMods, onlyNotHiddenHullmods, onlyApplicableHullmods);
            }
            // if nothing is available for some reason...
            if (chosenSModId == null) {
                //return;
                chosenSModId = HullMods.DEFECTIVE_MANUFACTORY; // this being the error hullmod mildly amuses me
            }
            // need to do some clean up beforehand if the hullmod is a hidden mod or a d-mod
            // also randomizes the cost, so you can't predict the hullmod based on the xp cost
            adjustHullModForSModding(chosenSModId, ship.getHullSize(), true, random);
            // make sure the variant doesn't already have it as a modular hullmod. I can't remember if this caused issues but overkill never fails
            if (variant.hasHullMod(chosenSModId)) {
                variant.removeMod(chosenSModId);
            }
            variant.addPermaMod(chosenSModId, true);
            // don't do this because then they don't count as s-mods which means they don't cost story points
            // variant.addPermaMod(chosenSModId, !(chosenSModSpec.hasTag(Tags.HULLMOD_DMOD) || chosenSModSpec.hasTag(SHOULD_BE_DMOD)));

            // add to a list of mods to fix up after confirming/cancelling the s-mod process
            addedMods.add(chosenSModId);
        }

        // will usually do nothing, exceptions being when it has been s-modded / s-modding has been cancelled
        // had to be done because the confirmation screen also fires this script
        // meaning weird stuff would happen when you cancel the s-modding process
        if (variant.getNonBuiltInHullmods().contains(spec.getId())) {
            // in the case that the player cancels the s-mod process
            // the potential s-mods will become regular mods which must be removed
            if (!Collections.disjoint(variant.getNonBuiltInHullmods(), addedMods)) {
                //log.info("S-modding cancelled");
                for (String addedModId : addedMods) {
                    if (variant.getNonBuiltInHullmods().contains(addedModId)) {
                        variant.removeMod(addedModId);
                        //log.info("removing not-s-modded mod " + addedModId);
                    }
                    restoreHullMod(addedModId); // fix any changes made earlier
                    //log.info("restoring hullmod");
                }
                addedMods.clear();
                saveData.put(loadKey, true);
                //variant.removeMod(spec.getId()); // kind of annoying to have to re-add repeatedly
            }
            // need a way to clear the addedMods list when you confirm. to do so we check that story points were spent on s-modding
            if (Global.getSector().getPlayerPerson().getStats().getStoryPoints() < numSP) {
                //log.info("S-modding confirmed");
                numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
                for (String addedModId : addedMods) {
                    restoreHullMod(addedModId); // as above, so below - fixing up changes made
                    //log.info("restoring hullmod");
                }
                addedMods.clear();
                saveData.put(loadKey, false);
                saveData.put(seedKey, tempSeed);
                //log.info("Saved seed " + tempSeed);
                // for some reason this doesn't work - gets re-added some point after this step,
                // and I can't find any way of removing it, like what the heck. RIP
                // update: it gets removed upon switching away to another ship, but will remain with any action that checks the variant
                // (i.e. anything that isn't swapping to another ship or leaving the refit screen)
                // the inconsistency is probably more annoying than just leaving the hullmod there, so commented out it is
                // variant.removeMod(spec.getId());
            }
            // blocks the s-modding process if you've reached the max amount
            // this is necessary because hidden mods don't count towards the s-mod limit
            // similarly, this is why we use ship.getVariant().getSMods().size() instead of Misc.getCurrSpecialMods(ship.getVariant)
            if (ship.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(ship)) {
                spec.addTag(Tags.HULLMOD_NO_BUILD_IN);
            } else {
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
            }
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship.getVariant().getSMods().size() > Misc.getMaxPermanentMods(ship)) {
            return false;
        }
        // prevent re-adding when it's at the exact amount
        // old comment:
        // // apparently if !isApplicableToShip, variant.addMod() does not work
        // // so this separate rule was needed if you would hit the max # of s-mods, blocking the re-installation of the hullmod
        // // and thereby blocking the checking mechanism
        // todo: this maybe wasn't the actual cause of the issue, but I don't know what it was, and I don't really care to find out since it works like this
        if (ship.getVariant().getSMods().size() == Misc.getMaxPermanentMods(ship) && !ship.getVariant().hasHullMod(spec.getId())) {
            return false;
        }
        if (shipHasOtherModInCategory(ship, spec.getId(), MOD_ID)) {
            return false;
        }
        return true;
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(ship)) {
            return AT_S_MOD_LIMIT;
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

    // Loads the seed entry for a given ship, or creates it if none exists
    public static long getSeed(ShipAPI ship, String seedKey) {
        long seed = new Random().nextLong();
        if (Global.getSector().getPersistentData().get(seedKey) != null) {
            seed = (long) Global.getSector().getPersistentData().get(seedKey);
            //log.info("Loaded seed key entry: " + seed);
        } else {
            if (Global.getSector().getSeedString() != null) {
                seed = Global.getSector().getSeedString().hashCode();
            }
            if (!Global.getSector().getPlayerPerson().getNameString().isEmpty()) {
                seed *= Global.getSector().getPlayerPerson().getNameString().hashCode();
            }
            if (ship.getFleetMemberId() != null) {
                seed *= ship.getFleetMemberId().hashCode();
            }
            Global.getSector().getPersistentData().put(seedKey, seed);
            //log.info("Created seed key entry: " + seed);
        }
        return seed;
    }

    // todo: add weights or something, that'd be pretty pog
    // // like inverse of their OP, so you'd have a 4x better chance of getting expanded mags than heavy armor for capital ships
    // // and then you could customize weights in a .json or something, would allow for interesting randomizer setups
    public String getRandomHullmod(ShipAPI ship, Random random, boolean onlyKnownHullmods, boolean onlyNotHiddenHullmods, boolean onlyApplicableHullmods) {
        ShipVariantAPI variant = ship.getVariant();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        if (random != null) {
            picker = new WeightedRandomPicker<>(random);
        }
        if (onlyKnownHullmods) {
            // playerFaction.getKnownHullMods() NPEs if done in a mission, so I guess you're stuck with defective manufactory
            // if you try s-modding this with onlyKnownHullmods = true
            if (Global.getCurrentState() != GameState.CAMPAIGN) {
                return null;
            }
            for (String hullmodId : Global.getSector().getPlayerFaction().getKnownHullMods()) {
                HullModSpecAPI hullmodSpec = Global.getSettings().getHullModSpec(hullmodId);
                // so like I should consider if it's reasonable to only s-mod dock-only mods when actually at a spaceport
                // I don't want to make it super tedious by making people leave the spaceport for better odds tho
                if (!variant.getPermaMods().contains(hullmodId)
                        && !variant.getHullSpec().getBuiltInMods().contains(hullmodId)
                        && !hullmodSpec.getTags().contains(MOD_ID)) {
                    if ((onlyApplicableHullmods && !hullmodSpec.getEffect().isApplicableToShip(ship))
                            || (onlyNotHiddenHullmods && hullmodSpec.isHidden())
                            || BLACKLISTED_HULLMODS.contains(hullmodId)) {
                        continue;
                    }
                    picker.add(hullmodId);
                    //log.info(hullmodId + " added to picker");
                }
            }
        } else {
            for (HullModSpecAPI hullmodSpec : Global.getSettings().getAllHullModSpecs()) {
                String hullmodId = hullmodSpec.getId();
                if (!variant.getPermaMods().contains(hullmodId)
                        && !variant.getHullSpec().getBuiltInMods().contains(hullmodId)
                        && !hullmodSpec.isHiddenEverywhere() // what if shard spawner were an option? nah, taking it out cuz there's too many headaches from it
                        && !hullmodSpec.getTags().contains(MOD_ID)) {
                    if ((onlyApplicableHullmods && !hullmodSpec.getEffect().isApplicableToShip(ship))
                            || (onlyNotHiddenHullmods && hullmodSpec.isHidden())
                            || BLACKLISTED_HULLMODS.contains(hullmodId)) {
                        continue;
                    }
                    picker.add(hullmodSpec.getId());
                    //log.info(hullmodSpec.getId() + " added to picker");
                }
            }
        }
        String pick = picker.pick(random);
        log.info("Picked " + pick);
        return pick;
    }

    // basically if you add a hidden or d-mod the game doesn't know how to remove it in the confirmation screen, so we're fixing that
    public static void adjustHullModForSModding(HullModSpecAPI hullModSpec, ShipAPI.HullSize hullSize, boolean adjustOPCost, Random random) {
        // do this or else it auto adds the s-mod without checking for story point usage
        // similar story for d-mods
        if (hullModSpec.isHidden()) {
            hullModSpec.setHidden(false);
            hullModSpec.addTag(SHOULD_BE_HIDDEN);
            //log.info("setting hidden to false for " + hullModSpec.getId());
        }
        if (hullModSpec.isHiddenEverywhere()) {
            hullModSpec.setHiddenEverywhere(false);
            hullModSpec.addTag(SHOULD_BE_HIDDEN_EVERYWHERE);
        }
        if (hullModSpec.hasTag(Tags.HULLMOD_DMOD)) {
            hullModSpec.getTags().remove(Tags.HULLMOD_DMOD);
            hullModSpec.addTag(SHOULD_BE_DMOD);
            //log.info("removing dmod tag from " + hullModSpec.getId());
        }
        if (adjustOPCost) {
            // randomizes OP cost so that you can't predict the hullmod based on xp gain
            // adds a tag so that we can remember the original cost
            if (random == null) {
                random = new Random();
            }
            // random number between 2 (ATG/Expanded Mags) to 10 (ECM/Nav Relay for FFs)
            // if you get Command Center or SO, lucky you!
            int opCost = (RANDOM_COST_MIN + random.nextInt(RANDOM_COST_MAX + 1 - RANDOM_COST_MIN));
            //log.info(opCost);
            switch (hullSize) {
                case FRIGATE:
                    hullModSpec.addTag(MOD_ID + "_" + FRIGATE_CODE + "_" + hullModSpec.getFrigateCost());
                    hullModSpec.setFrigateCost(opCost);
                    break;
                case DESTROYER:
                    hullModSpec.addTag(MOD_ID + "_" + DESTROYER_CODE + "_" + hullModSpec.getDestroyerCost());
                    hullModSpec.setDestroyerCost(2 * opCost);
                    break;
                case CRUISER:
                    hullModSpec.addTag(MOD_ID + "_" + CRUISER_CODE + "_" + hullModSpec.getCruiserCost());
                    hullModSpec.setCruiserCost(3 * opCost);
                    break;
                case CAPITAL_SHIP:
                    hullModSpec.addTag(MOD_ID + "_" + CAPITAL_SHIP_CODE + "_" + hullModSpec.getCapitalCost());
                    hullModSpec.setCapitalCost(5 * opCost);
                    break;
                case DEFAULT:
                    break; // do nothing
            }
        }
    }

    public static void adjustHullModForSModding(String hullModId, ShipAPI.HullSize hullSize, boolean adjustOPCost, Random random) {
        adjustHullModForSModding(Global.getSettings().getHullModSpec(hullModId), hullSize, adjustOPCost, random);
    }

    // fix all the stuff we broke in the previous method
    public static void restoreHullMod(HullModSpecAPI hullModSpec) {
        // fix Hidden
        if (hullModSpec.hasTag(SHOULD_BE_HIDDEN)) {
            hullModSpec.setHidden(true);
            hullModSpec.getTags().remove(SHOULD_BE_HIDDEN);
        }
        // fix HiddenEverywhere
        if (hullModSpec.hasTag(SHOULD_BE_HIDDEN_EVERYWHERE)) {
            hullModSpec.setHiddenEverywhere(true);
            hullModSpec.getTags().remove(SHOULD_BE_HIDDEN_EVERYWHERE);
        }
        // fix dmod tag
        if (hullModSpec.hasTag(SHOULD_BE_DMOD)) {
            hullModSpec.addTag(Tags.HULLMOD_DMOD);
            hullModSpec.getTags().remove(SHOULD_BE_DMOD);
        }
        // fix OP costs
        String opCostTag = null;
        for (String tag : hullModSpec.getTags()) {
            //(tag + ": " + tag.matches(OP_COST_REGEX));
            if (tag.matches(OP_COST_REGEX)) {
                opCostTag = tag;
                String hullSize = tag.split("_")[1];
                int opCost = Integer.parseInt(tag.split("_")[2]);
                switch (hullSize) {
                    case FRIGATE_CODE:
                        hullModSpec.setFrigateCost(opCost);
                        break;
                    case DESTROYER_CODE:
                        hullModSpec.setDestroyerCost(opCost);
                        break;
                    case CRUISER_CODE:
                        hullModSpec.setCruiserCost(opCost);
                        break;
                    case CAPITAL_SHIP_CODE:
                        hullModSpec.setCapitalCost(opCost);
                        break;
                    default:
                        break; // do nothing
                }
                break;
            }
        }
        if (opCostTag != null) {
            hullModSpec.getTags().remove(opCostTag);
        }
    }

    public static void restoreHullMod(String hullModId) {
        restoreHullMod(Global.getSettings().getHullModSpec(hullModId));
    }

    // method used to check for incompatibilities
    // look at this poggers ternary operator
    public static String getOtherModsInCategory(ShipAPI ship, String currMod, String category) {
        String otherMods = null;
        for (String id : ship.getVariant().getHullMods()) {
            HullModSpecAPI mod = Global.getSettings().getHullModSpec(id);
            if (!mod.hasTag(category)) continue;
            if (id.equals(currMod)) continue;
            otherMods = otherMods == null ? mod.getDisplayName() : otherMods + ", " + mod.getDisplayName();
        }
        return otherMods;
    }
}