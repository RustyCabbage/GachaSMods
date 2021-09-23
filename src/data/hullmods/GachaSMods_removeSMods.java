package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;

import java.awt.*;
import java.util.Random;

import static data.hullmods.GachaSMods_randomSMod.getSeed;
import static data.scripts.GachaSMods_Utils.*;


public class GachaSMods_removeSMods extends BaseHullMod {

    private static final Logger log = Global.getLogger(GachaSMods_removeSMods.class);

    private final String HULLMOD_CONFLICT = getString("gachaConflict");
    private final String NO_SMODS = getString("removeInapplicable"); // "Ship is at the built-in hullmod limit"
    private int numSP = -1; //constant per character

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        if (ship.getVariant().getSMods().size() >= Misc.getMaxPermanentMods(ship)) {
            ship.getMutableStats().getDynamic().getMod(Stats.MAX_PERMANENT_HULLMODS_MOD).modifyFlat(id, 1);
        }

        // initialize all these dumb variables
        ShipVariantAPI variant = ship.getVariant();
        Random random = new Random();

        if (variant.getNonBuiltInHullmods().contains(spec.getId())) {
            numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
        }

        // main script that fires when the hullmod has been s-modded
        if (variant.getSMods().contains(spec.getId())) {
            // anti-save scum stuff
            if (ship.getFleetMemberId() != null && Global.getSettings().getBoolean(NO_SAVE_SCUMMING_SETTING)) {
                String seedKey = SEED_KEY + "_" + ship.getFleetMemberId();
                long savedSeed = getSeed(ship, seedKey);
                random = new Random(savedSeed);
            }

            // upon confirmation
            if (Global.getSector().getPlayerPerson().getStats().getStoryPoints() < numSP) {
                log.info("removing s-mods...");
                numSP = Global.getSector().getPlayerPerson().getStats().getStoryPoints();
                variant.removePermaMod(spec.getId());

                int numSMods = variant.getSMods().size();
                WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(random);
                picker.addAll(variant.getSMods());

                int numSModsToRemove = 1 + random.nextInt(numSMods); // min 1 because I don't hate you all that much (can't say I won't change my mind)
                log.info("Removing " + numSModsToRemove + " s-mods");
                for (int i = 0; i < numSModsToRemove; i++) {
                    String pick = picker.pick(random);
                    picker.remove(pick);
                    variant.removePermaMod(pick);
                    log.info("Removing " + pick);
                }
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

    public String getOtherModsInCategory(ShipAPI ship, String currMod, String category) {
        String otherMods = null;
        for (String id : ship.getVariant().getHullMods()) {
            HullModSpecAPI mod = Global.getSettings().getHullModSpec(id);
            if (!mod.hasTag(category)) continue;
            if (id.equals(currMod)) continue;
            otherMods = otherMods == null ? mod.getDisplayName() : otherMods + ", " + mod.getDisplayName(); //todo externalize?
        }
        return otherMods;
    }
}
