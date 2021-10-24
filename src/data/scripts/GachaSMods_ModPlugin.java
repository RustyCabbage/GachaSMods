package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

import static data.scripts.GachaSMods_Utils.*;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    // settings in data/config/settings.json
    public static final String
            SETTINGS_JSON = "data/config/settings.json",
            NO_SAVE_SCUMMING_SETTING = MOD_ID + "_" + "noSaveScumming",
            DISABLE_RANDOM_SMODS_SETTING = MOD_ID + "_" + "disableRandomSMods",
            DISABLE_REMOVE_SMODS_SETTING = MOD_ID + "_" + "disableRemoveSMods",
            ALLOW_STANDARD_SMODS_SETTING = MOD_ID + "_" + "allowStandardSMods",
            SELECTION_MODE_SETTING = MOD_ID + "_" + "selectionMode",
            TRUE_RANDOM_SETTING = MOD_ID + "_" + "trueRandomMode",
            BLACKLISTED_HULLMODS_ARRAY = MOD_ID + "_" + "blacklistedHullmods",
            ONLY_KNOWN_HULLMODS_SETTING = MOD_ID + "_" + "onlyKnownHullmods",
            ONLY_NOT_HIDDEN_HULLMODS_SETTING = MOD_ID + "_" + "onlyNotHiddenHullmods",
            ONLY_APPLICABLE_HULLMODS_SETTING = MOD_ID + "_" + "onlyApplicableHullmods",
            RESPECT_NO_BUILD_IN_SETTING = MOD_ID + "_" + "respectNoBuildIn", // also used as a tag
            PR_INCR_FOR_FREE_OR_HIDDEN_MODS_SETTING = MOD_ID + "_" + PROPORTIONAL_CODE + "_" + "incrForFreeOrHiddenMods",
            TG_COST_FOR_RARE2_SETTING = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "costForRare2",
            TG_RARE1_MULT_SETTING = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare1Mult",
            TG_RARE2_MULT_SETTING = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare2Mult",
            TG_RARE3_MULT_SETTING = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare3Mult",
            TG_RARE4_MULT_SETTING = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare4Mult",
            MIN_REMOVED_SMODS_SETTING = MOD_ID + "_" + "minRemovedSMods",
            MAX_REMOVED_SMODS_SETTING = MOD_ID + "_" + "maxRemovedSMods";
    // setting to default values. overwritten on application and game load
    public static boolean
            LOADING_FAILED = true,
            NO_SAVE_SCUMMING = true,
            DISABLE_RANDOM_SMODS = false,
            DISABLE_REMOVE_SMODS = false,
            ALLOW_STANDARD_SMODS = false,
            TRUE_RANDOM = false,
            ONLY_KNOWN_HULLMODS = false,
            ONLY_NOT_HIDDEN_HULLMODS = true,
            ONLY_APPLICABLE_HULLMODS = true,
            RESPECT_NO_BUILD_IN = false;
    public static int
            PR_INCR_FOR_FREE_OR_HIDDEN_MODS = 20,
            TG_COST_FOR_RARE2 = 3,
            MIN_REMOVED_SMODS = 1,
            MAX_REMOVED_SMODS = 3;
    public static float
            TG_RARE1_MULT = 52.5f,
            TG_RARE2_MULT = 25f,
            TG_RARE3_MULT = 15f,
            TG_RARE4_MULT = 7.5f;
    public static String
            attemptingToLoad = "",
            SELECTION_MODE = "CL";

    private final Logger log = Global.getLogger(GachaSMods_ModPlugin.class);

    @Override
    public void onApplicationLoad() {
        loadSettings();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        loadSettings();
        // disables the random s-mods hullmod, which kinda makes this whole mod moot but whatever maybe they just want the s-mod removing features
        Global.getSettings().getHullModSpec(RANDOM_SMOD_ID).setHiddenEverywhere(DISABLE_RANDOM_SMODS);
        Global.getSettings().getHullModSpec(RANDOM_SMOD_ID).setHidden(DISABLE_RANDOM_SMODS); // you have to set both hidden and hiddenEverywhere to true, TIL
        // disables the remove s-mods hullmod, as god intended
        Global.getSettings().getHullModSpec(REMOVE_SMOD_ID).setHidden(DISABLE_REMOVE_SMODS);
        Global.getSettings().getHullModSpec(REMOVE_SMOD_ID).setHiddenEverywhere(DISABLE_REMOVE_SMODS);
        // block building-in all the other hullmods
        if (!(ALLOW_STANDARD_SMODS || DISABLE_RANDOM_SMODS)) {
            for (HullModSpecAPI hullModSpec : Global.getSettings().getAllHullModSpecs()) {
                if (hullModSpec.hasTag(Tags.HULLMOD_NO_BUILD_IN) && RESPECT_NO_BUILD_IN) {
                    hullModSpec.addTag(RESPECT_NO_BUILD_IN_SETTING);
                }
                if (!hullModSpec.getId().startsWith(MOD_ID)) {
                    hullModSpec.addTag(Tags.HULLMOD_NO_BUILD_IN);
                }
            }
        }
        // remove hullmods from being known by other factions for save compatibility
        // note that having them be a default hullmod in the .csv avoids it being added to loot tables, I think
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.getId().equals(Factions.PLAYER)) {
                continue;
            }
            Iterator<String> knownHullMods = faction.getKnownHullMods().iterator();
            while (knownHullMods.hasNext()) {
                String hullModId = knownHullMods.next();
                if (hullModId.startsWith(MOD_ID)) {
                    knownHullMods.remove();
                }
            }
        }
        // should these hullmods require a spaceport? no Tags.REQUIRES_SPACEPORT, so I am too lazy
    }

    // for save compatibility
    @Override
    public void beforeGameSave() {
        FactionAPI faction = Global.getSector().getPlayerFaction();
        Iterator<String> knownHullMods = faction.getKnownHullMods().iterator();
        while (knownHullMods.hasNext()) {
            String hullModId = knownHullMods.next();
            if (hullModId.startsWith(MOD_ID)) {
                knownHullMods.remove();
            }
        }
        long tick = System.currentTimeMillis();
        // check for ships in storage / sold to markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                for (FleetMemberAPI m : submarket.getCargo().getMothballedShips().getMembersListCopy()) {
                    Iterator<String> hullMods = m.getVariant().getHullMods().iterator();
                    while (hullMods.hasNext()) {
                        String hullModId = hullMods.next();
                        if (hullModId.startsWith(MOD_ID)) {
                            hullMods.remove();
                        }
                    }
                }
            }
        }
        // frick what if the Cabal steals one or something
        for (LocationAPI loc : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI f : loc.getFleets()) {
                for (FleetMemberAPI m : f.getFleetData().getMembersListCopy()) {
                    Iterator<String> hullMods = m.getVariant().getHullMods().iterator();
                    while (hullMods.hasNext()) {
                        String hullModId = hullMods.next();
                        if (hullModId.startsWith(MOD_ID)) {
                            hullMods.remove();
                        }
                    }
                }
            }
        }
        long tock = System.currentTimeMillis();
        log.info("Time to clean save of hullmods (ms): " + (tock - tick)); //in my test this took 1 ms
    }

    /* this is apparently unnecessary
    @Override
    public void afterGameSave() {
        FactionAPI faction = Global.getSector().getPlayerFaction();
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) { // maybe should just write the two hullmods, but future-proofing?
            if (hullmod.hasTag(MOD_ID)) {
                faction.addKnownHullMod(hullmod.getId());
            }
        }
    }
     */

    private void loadSettings() {
        try {
            log.info("Loading " + MOD_ID + "/" + SETTINGS_JSON + "...");
            attemptingToLoad = SETTINGS_JSON;
            JSONObject settings = Global.getSettings().loadJSON(SETTINGS_JSON, MOD_ID);
            attemptingToLoad = NO_SAVE_SCUMMING_SETTING;
            NO_SAVE_SCUMMING = settings.getBoolean(NO_SAVE_SCUMMING_SETTING);
            attemptingToLoad = DISABLE_RANDOM_SMODS_SETTING;
            DISABLE_RANDOM_SMODS = settings.getBoolean(DISABLE_RANDOM_SMODS_SETTING);
            attemptingToLoad = DISABLE_REMOVE_SMODS_SETTING;
            DISABLE_REMOVE_SMODS = settings.getBoolean(DISABLE_REMOVE_SMODS_SETTING);
            attemptingToLoad = ALLOW_STANDARD_SMODS_SETTING;
            ALLOW_STANDARD_SMODS = settings.getBoolean(ALLOW_STANDARD_SMODS_SETTING);
            attemptingToLoad = SELECTION_MODE_SETTING;
            SELECTION_MODE = settings.getString(SELECTION_MODE_SETTING);
            attemptingToLoad = TRUE_RANDOM_SETTING;
            TRUE_RANDOM = settings.getBoolean(TRUE_RANDOM_SETTING);
            attemptingToLoad = ONLY_KNOWN_HULLMODS_SETTING;
            ONLY_KNOWN_HULLMODS = settings.getBoolean(ONLY_KNOWN_HULLMODS_SETTING);
            attemptingToLoad = ONLY_NOT_HIDDEN_HULLMODS_SETTING;
            ONLY_NOT_HIDDEN_HULLMODS = settings.getBoolean(ONLY_NOT_HIDDEN_HULLMODS_SETTING);
            attemptingToLoad = ONLY_APPLICABLE_HULLMODS_SETTING;
            ONLY_APPLICABLE_HULLMODS = settings.getBoolean(ONLY_APPLICABLE_HULLMODS_SETTING);
            attemptingToLoad = RESPECT_NO_BUILD_IN_SETTING;
            RESPECT_NO_BUILD_IN = settings.getBoolean(RESPECT_NO_BUILD_IN_SETTING);
            attemptingToLoad = PR_INCR_FOR_FREE_OR_HIDDEN_MODS_SETTING;
            PR_INCR_FOR_FREE_OR_HIDDEN_MODS = settings.getInt(PR_INCR_FOR_FREE_OR_HIDDEN_MODS_SETTING);
            attemptingToLoad = TG_COST_FOR_RARE2_SETTING;
            TG_COST_FOR_RARE2 = settings.getInt(TG_COST_FOR_RARE2_SETTING);
            attemptingToLoad = MIN_REMOVED_SMODS_SETTING;
            MIN_REMOVED_SMODS = settings.getInt(MIN_REMOVED_SMODS_SETTING);
            attemptingToLoad = MAX_REMOVED_SMODS_SETTING;
            MAX_REMOVED_SMODS = settings.getInt(MAX_REMOVED_SMODS_SETTING);
            attemptingToLoad = TG_RARE1_MULT_SETTING;
            TG_RARE1_MULT = (float) settings.getDouble(TG_RARE1_MULT_SETTING);
            attemptingToLoad = TG_RARE2_MULT_SETTING;
            TG_RARE2_MULT = (float) settings.getDouble(TG_RARE2_MULT_SETTING);
            attemptingToLoad = TG_RARE3_MULT_SETTING;
            TG_RARE3_MULT = (float) settings.getDouble(TG_RARE3_MULT_SETTING);
            attemptingToLoad = TG_RARE4_MULT_SETTING;
            TG_RARE4_MULT = (float) settings.getDouble(TG_RARE4_MULT_SETTING);
            // load blacklisted hullmods
            attemptingToLoad = BLACKLISTED_HULLMODS_ARRAY;
            JSONArray array = settings.getJSONArray(BLACKLISTED_HULLMODS_ARRAY);
            for (int i = 0; i < array.length(); i++) {
                BLACKLISTED_HULLMODS.add((String) array.get(i));
            }
            //log.info("Blacklisted hullmods: " + BLACKLISTED_HULLMODS);
            LOADING_FAILED = false;
            log.info("Loading " + MOD_ID + "/" + SETTINGS_JSON + " completed");
        } catch (IOException | JSONException e) {
            LOADING_FAILED = true;
            log.error("Could not load " + MOD_ID + "/" + SETTINGS_JSON + ": problem loading " + attemptingToLoad, e);
        }
    }
}