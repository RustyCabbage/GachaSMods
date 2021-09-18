package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

public class GachaSMods_randomSMod extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_randomSMod.class);

    // used to check for random mode
    public static String TRULY_RANDOM = "eurobeat";
    public static String TRUE_RANDOM_SETTING = "GachaSMods_trueRandomMode";
    // used to fix up hullmods I messed with
    public static String SHOULD_BE_HIDDEN = "GachaSMods_shouldBeHidden";
    public static String SHOULD_BE_DMOD = "GachaSMods_shouldBeDMod";
    // not applicable message
    public static String AT_S_MOD_LIMIT = "Ship is at the built-in hullmod limit";

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
            if (ship.getName().toLowerCase().equals(TRULY_RANDOM) || Global.getSettings().getBoolean(TRUE_RANDOM_SETTING)) {
                chosenSModId = getRandomHullmod(ship, false, false);
                HullModSpecAPI chosenSModSpec = Global.getSettings().getHullModSpec(chosenSModId);
                // do this or else it auto adds the s-mod without checking for story point usage
                // similar story for d-mods
                if (chosenSModSpec.isHidden()) {
                    chosenSModSpec.setHidden(false);
                    chosenSModSpec.addTag(SHOULD_BE_HIDDEN);
                    //log.info("setting hidden to false for " + chosenSModId);
                }
                if (chosenSModSpec.hasTag(Tags.HULLMOD_DMOD)) {
                    chosenSModSpec.getTags().remove(Tags.HULLMOD_DMOD);
                    chosenSModSpec.addTag(SHOULD_BE_DMOD);
                    //log.info("removing dmod tag from " + chosenSModId);
                }
            } else {
                chosenSModId = getRandomHullmod(ship, true, true);
            }
            if (chosenSModId == null) {
                //return;
                chosenSModId = HullMods.DEFECTIVE_MANUFACTORY; // this being the error hullmod mildly amuses me
            }
            if (variant.hasHullMod(chosenSModId)) {
                variant.removeMod(chosenSModId);
            }

            // s-mod if not d-mod
            // you can restore the ship even if a d-mod is s-modded, so no point in forcing a restore to add more hullmods
            variant.addPermaMod(chosenSModId, !Global.getSettings().getHullModSpec(chosenSModId).hasTag(Tags.HULLMOD_DMOD));

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
                    if (addedModSpec.hasTag(SHOULD_BE_HIDDEN)) {
                        addedModSpec.setHidden(true);
                        addedModSpec.getTags().remove(SHOULD_BE_HIDDEN);
                    }
                    if (addedModSpec.hasTag(SHOULD_BE_DMOD)) {
                        addedModSpec.addTag(Tags.HULLMOD_DMOD);
                        addedModSpec.getTags().remove(SHOULD_BE_DMOD);
                    }
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
                    // as above so below - fixing up changes made
                    if (addedModSpec.hasTag(SHOULD_BE_HIDDEN)) {
                        addedModSpec.setHidden(true);
                        addedModSpec.getTags().remove(SHOULD_BE_HIDDEN);
                    }
                    if (addedModSpec.hasTag(SHOULD_BE_DMOD)) {
                        addedModSpec.addTag(Tags.HULLMOD_DMOD);
                        addedModSpec.getTags().remove(SHOULD_BE_DMOD);
                    }
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
    public String getRandomHullmod(ShipAPI ship, boolean onlyKnownHullmods, boolean onlyApplicableHullmods) {
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
                    if (onlyApplicableHullmods && !hullmodSpec.getEffect().isApplicableToShip(ship)) {
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
                    if (onlyApplicableHullmods && !hullmodSpec.getEffect().isApplicableToShip(ship)) {
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
}
