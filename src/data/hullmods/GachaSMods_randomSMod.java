package data.hullmods;

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
import java.util.Random;

public class GachaSMods_randomSMod extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_randomSMod.class);

    // todo: maybe some day I'll externalize these strings
    public static final String MOD_PREFIX = "GachaSMods";
    // used to check for random mode
    public static final String TRULY_RANDOM = "eurobeat";
    public static final String TRUE_RANDOM_SETTING = MOD_PREFIX + "_" + "trueRandomMode";
    public static final String ONLY_KNOWN_HULLMODS_SETTING = MOD_PREFIX + "_" + "onlyKnownHullmods";
    // used to fix up hullmods I messed with
    public static final String SHOULD_BE_HIDDEN = MOD_PREFIX + "_" + "shouldBeHidden";
    public static final String SHOULD_BE_DMOD = MOD_PREFIX + "_" + "shouldBeDMod";
    // if your hullmod costs over 9999 i'll kill you
    public static final int RANDOM_COST_MIN = 2;
    public static final int RANDOM_COST_MAX = 10;
    public static final String FRIGATE_CODE = "ff";
    public static final String DESTROYER_CODE = "dd";
    public static final String CRUISER_CODE = "cc";
    public static final String CAPITAL_SHIP_CODE = "bb";
    public static final String OP_COST_REGEX = "^(" + MOD_PREFIX + ")" + "_"
            + "(" + FRIGATE_CODE + "|" + DESTROYER_CODE + "|" + CRUISER_CODE + "|" + CAPITAL_SHIP_CODE + ")" + "_"
            + "([0-9]$|[1-9][0-9]$|[1-9][0-9][0-9]$|[1-9][0-9][0-9][0-9])$";
    // not applicable message
    public static final String AT_S_MOD_LIMIT = "Ship is at the built-in hullmod limit";

    // yeah yeah they only instantiate once per class blah blah blah it should be fine
    // they're updated/cleared any time they're used anyways
    private static final ArrayList<String> addedMods = new ArrayList<>();
    private static int numSP = -1;

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

            String chosenSModId;
            if (ship.getName().equalsIgnoreCase(TRULY_RANDOM) || Global.getSettings().getBoolean(TRUE_RANDOM_SETTING)) {
                chosenSModId = getRandomHullmod(ship, false, false, false);

            } else {
                // only known hullmods is set to false by default to discourage strategic learning of hullmods
                chosenSModId = getRandomHullmod(ship, Global.getSettings().getBoolean(ONLY_KNOWN_HULLMODS_SETTING), true, true);
            }
            if (chosenSModId == null) {
                //return;
                chosenSModId = HullMods.DEFECTIVE_MANUFACTORY; // this being the error hullmod mildly amuses me
            }
            // need to do some clean up beforehand if the hullmod is a hidden mod or a d-mod
            // also randomizes the cost so you can't predict the hullmod based on the xp cost
            HullModSpecAPI chosenSModSpec = Global.getSettings().getHullModSpec(chosenSModId);
            adjustHullModForSModding(chosenSModSpec, ship.getHullSize());

            // s-mod if not d-mod
            // you can restore the ship even if a d-mod is s-modded, so setting them as s-mods is simply unaesthetic for no gain
            if (variant.hasHullMod(chosenSModId)) {
                variant.removeMod(chosenSModId);
            }
            variant.addPermaMod(chosenSModId, !(chosenSModSpec.hasTag(Tags.HULLMOD_DMOD) || chosenSModSpec.hasTag(SHOULD_BE_DMOD)));

            // add to a list of mods to fix up after confirming/cancelling the s-mod process
            addedMods.add(chosenSModId);
        }

        // will usually do nothing, exceptions being when it has been s-modded / s-modding has been cancelled
        // had to be done because the confirmation screen also fires this script
        // meaning weird shit would happen when you cancel the s-modding process
        if (variant.hasHullMod(spec.getId())) {
            // in the case that the player cancels the s-mod process, the potential s-mods will become regular mods
            // which must be removed
            if (!Collections.disjoint(variant.getNonBuiltInHullmods(), addedMods)) {
                //log.info("s-modding cancelled");
                for (String addedModId : addedMods) {
                    HullModSpecAPI addedModSpec = Global.getSettings().getHullModSpec(addedModId);
                    if (variant.getNonBuiltInHullmods().contains(addedModId)) {
                        variant.removeMod(addedModId);
                        //log.info("removing not-s-modded mod " + addedModId);
                    }
                    // fix any changes made earlier
                    restoreHullMod(addedModSpec);
                    //log.info("restoring hullmod");
                }
                addedMods.clear();
                // variant.removeMod(spec.getId());
            }
            // need a way to clear the addedMods list when you confirm
            if (Global.getSector().getPlayerPerson().getStats().getStoryPoints() < numSP) {
                //log.info("s-modding confirmed");
                numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
                for (String addedModId : addedMods) {
                    HullModSpecAPI addedModSpec = Global.getSettings().getHullModSpec(addedModId);
                    // as above, so below - fixing up changes made
                    restoreHullMod(addedModSpec);
                    //log.info("restoring hullmod");
                }
                addedMods.clear();
                // for some reason this doesn't work - gets re-added some point after this step
                // and I can't find any way of removing it, like what the heck. RIP
                //variant.removeMod(spec.getId());

            }
            // the game doesn't track s-mods that are hidden, which I think I corrected for but just in case putting it here too
            if (ship.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(ship)) {
                spec.addTag(Tags.HULLMOD_NO_BUILD_IN);
            } else {
                spec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
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

    // todo: add weights or something, that'd be pretty pog
    // like inverse of their OP, so you'd have a 4x better chance of getting expanded mags than heavy armor for capital ships
    public String getRandomHullmod(ShipAPI ship, boolean onlyKnownHullmods, boolean onlyNotHiddenHullmods, boolean onlyApplicableHullmods) {
        ShipVariantAPI variant = ship.getVariant();
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        if (onlyKnownHullmods) {
            for (String hullmodId : Global.getSector().getPlayerFaction().getKnownHullMods()) {
                HullModSpecAPI hullmodSpec = Global.getSettings().getHullModSpec(hullmodId);
                // so like I should consider if it's reasonable to only s-mod dock-only mods when actually at a spaceport
                // I don't want to make it super tedious by making people leave the spaceport for better odds tho
                if (!variant.getPermaMods().contains(hullmodId)
                        && !variant.getHullSpec().getBuiltInMods().contains(hullmodId)
                        && !hullmodId.equals(spec.getId())) {
                    if ((onlyApplicableHullmods && !hullmodSpec.getEffect().isApplicableToShip(ship))
                            || (onlyNotHiddenHullmods && hullmodSpec.isHidden())) {
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
                        && !hullmodSpec.isHiddenEverywhere() // what if shardspawner were an option? nah taking it out cuz there's too many headaches from it
                        && hullmodSpec != spec) {
                    if ((onlyApplicableHullmods && !hullmodSpec.getEffect().isApplicableToShip(ship))
                            || (onlyNotHiddenHullmods && hullmodSpec.isHidden())) {
                        continue;
                    }
                    // todo: i'll probably make a proper blacklisting system later but lazy for now
                    // Vast Bulk is blocked because it doesn't actually explode your ship on deploy, just turns off the AI and makes it invincible.
                    if (hullmodId.equals(HullMods.VASTBULK)) {
                        continue;
                    }
                    picker.add(hullmodSpec.getId());
                    //log.info(hullmodSpec.getId() + " added to picker");
                }
            }
        }
        String pick = picker.pick();
        //log.info("Picked " + pick);
        return pick;
    }

    // basically if you add a hidden or d-mod the game doesn't know how to remove it in the confirmation screen so we're fixing that
    public static void adjustHullModForSModding(HullModSpecAPI hullModSpec, ShipAPI.HullSize hullSize) {
        // do this or else it auto adds the s-mod without checking for story point usage
        // similar story for d-mods
        if (hullModSpec.isHidden()) {
            hullModSpec.setHidden(false);
            hullModSpec.addTag(SHOULD_BE_HIDDEN);
            //log.info("setting hidden to false for " + chosenSModId);
        }
        if (hullModSpec.hasTag(Tags.HULLMOD_DMOD)) {
            hullModSpec.getTags().remove(Tags.HULLMOD_DMOD);
            hullModSpec.addTag(SHOULD_BE_DMOD);
            //log.info("removing dmod tag from " + chosenSModId);
        }
        // randomizes OP cost so that you can't predict the hullmod based on xp gain
        // adds a tag so that we can remember the original cost
        Random random = new Random();
        // random number between 2 (ATG/Expanded Mags) to 10 (ECM/Nav Relay for FFs)
        // if you get Command Center or SO, lucky you!
        int opCost = (RANDOM_COST_MIN + random.nextInt(RANDOM_COST_MAX + 1 - RANDOM_COST_MIN));
        log.info(opCost);
        switch (hullSize) {
            case FRIGATE:
                hullModSpec.addTag(MOD_PREFIX + "_" + FRIGATE_CODE + "_" + hullModSpec.getFrigateCost());
                hullModSpec.setFrigateCost(opCost);
                break;
            case DESTROYER:
                hullModSpec.addTag(MOD_PREFIX + "_" + DESTROYER_CODE + "_" + hullModSpec.getDestroyerCost());
                hullModSpec.setDestroyerCost(2 * opCost);
                break;
            case CRUISER:
                hullModSpec.addTag(MOD_PREFIX + "_" + CRUISER_CODE + "_" + hullModSpec.getCruiserCost());
                hullModSpec.setCruiserCost(3 * opCost);
                break;
            case CAPITAL_SHIP:
                hullModSpec.addTag(MOD_PREFIX + "_" + CAPITAL_SHIP_CODE + "_" + hullModSpec.getCapitalCost());
                hullModSpec.setCapitalCost(5 * opCost);
                break;
            case DEFAULT:
                break; // do nothing
        }
    }

    public static void restoreHullMod(HullModSpecAPI hullModSpec) {
        if (hullModSpec.hasTag(SHOULD_BE_HIDDEN)) {
            hullModSpec.setHidden(true);
            hullModSpec.getTags().remove(SHOULD_BE_HIDDEN);
        }
        if (hullModSpec.hasTag(SHOULD_BE_DMOD)) {
            hullModSpec.addTag(Tags.HULLMOD_DMOD);
            hullModSpec.getTags().remove(SHOULD_BE_DMOD);
        }
        String opCostTag = null;
        for (String tag : hullModSpec.getTags()) {
            log.info(tag + ": " + tag.matches(OP_COST_REGEX));
            if (tag.matches(OP_COST_REGEX)) {
                opCostTag = tag;
                String hullSize = tag.split("_")[1]; // does this also need to be externalized? idek
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
}
