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

import static data.scripts.GachaSMods_OnlineBlacklist.loadOnlineBlackList;
import static data.scripts.GachaSMods_Utils.*;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    // settings in data/config/settings.json
    public static final String
            SETTINGS_JSON = "data/config/settings.json",
            GREYLIST_JSON = "data/config/greylistedHullmods.json",
            ONLINE_BLACKLIST_URL = "https://raw.githubusercontent.com/RustyCabbage/GachaSMods/main/data/config/settings.json",
            ONLINE_GREYLIST_URL = "https://raw.githubusercontent.com/RustyCabbage/GachaSMods/main/data/config/greylistedHullmods.json",
            NO_SAVE_SCUMMING_SETTING = MOD_ID + "_" + "noSaveScumming",
            USE_ONLINE_BLACKLIST_SETTING = MOD_ID + "_" + "useOnlineBlacklist",
            DISABLE_RANDOM_SMODS_SETTING = MOD_ID + "_" + "disableRandomSMods",
            DISABLE_REMOVE_SMODS_SETTING = MOD_ID + "_" + "disableRemoveSMods",
            ALLOW_STANDARD_SMODS_SETTING = MOD_ID + "_" + "allowStandardSMods",
            SELECTION_MODE_SETTING = MOD_ID + "_" + "selectionMode",
            TRUE_RANDOM_SETTING = MOD_ID + "_" + "trueRandomMode",
            BLACKLISTED_HULLMODS_ARRAY = MOD_ID + "_" + "blacklistedHullmods",
            GREYLISTED_HULLMODS_ARRAY = MOD_ID + "_" + "greylistedHullmods",
            USE_GREYLIST_SETTING = MOD_ID + "_" + "useGreylist",
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
            USE_ONLINE_BLACKLIST = false,
            DISABLE_RANDOM_SMODS = false,
            DISABLE_REMOVE_SMODS = false,
            ALLOW_STANDARD_SMODS = false,
            TRUE_RANDOM = false,
            ONLY_KNOWN_HULLMODS = false,
            ONLY_NOT_HIDDEN_HULLMODS = true,
            ONLY_APPLICABLE_HULLMODS = true,
            RESPECT_NO_BUILD_IN = false,
            USE_GREYLIST = false;
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
        for (HullModSpecAPI hullModSpec : Global.getSettings().getAllHullModSpecs()) {
            if (hullModSpec.hasTag(Tags.HULLMOD_NO_BUILD_IN)) {
                hullModSpec.addTag(RESPECT_NO_BUILD_IN_SETTING);
            }
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        loadSettings();
        // block building-in all the other hullmods
        for (HullModSpecAPI hullModSpec : Global.getSettings().getAllHullModSpecs()) {
            if (!(ALLOW_STANDARD_SMODS || DISABLE_RANDOM_SMODS)) {
                if (!hullModSpec.getId().startsWith(MOD_ID)) {
                    hullModSpec.addTag(Tags.HULLMOD_NO_BUILD_IN);
                }
            } else {
                if (hullModSpec.hasTag(Tags.HULLMOD_NO_BUILD_IN) && !hullModSpec.hasTag(RESPECT_NO_BUILD_IN_SETTING)) {
                    hullModSpec.getTags().remove(Tags.HULLMOD_NO_BUILD_IN);
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
        // should these hullmods require a spaceport? there is no Tags.REQUIRES_SPACEPORT, so I am too lazy
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
            JSONObject settings = Global.getSettings().getMergedJSONForMod(SETTINGS_JSON, MOD_ID);
            attemptingToLoad = NO_SAVE_SCUMMING_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            NO_SAVE_SCUMMING = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = USE_ONLINE_BLACKLIST_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            USE_ONLINE_BLACKLIST = settings.getBoolean(attemptingToLoad);
            // loads the online blacklist if enabled
            if (USE_ONLINE_BLACKLIST) {
                System.setProperty("https.protocols", "SSLv3,TLSv1,TLSv1.1,TLSv1.2");
                log.info("----------------------------------");
                log.info("---- LOADING ONLINE BLACKLIST ----");
                log.info("----------------------------------");
                loadOnlineBlackList(ONLINE_BLACKLIST_URL, BLACKLISTED_HULLMODS_ARRAY, log);
            }

            attemptingToLoad = DISABLE_RANDOM_SMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            DISABLE_RANDOM_SMODS = settings.optBoolean(attemptingToLoad, false);
            // if true, disables the random s-mods hullmod, which kinda makes this whole mod moot but whatever maybe they just want the s-mod removing features
            Global.getSettings().getHullModSpec(RANDOM_SMOD_ID).setHidden(DISABLE_RANDOM_SMODS);
            Global.getSettings().getHullModSpec(RANDOM_SMOD_ID).setHiddenEverywhere(DISABLE_RANDOM_SMODS);

            attemptingToLoad = DISABLE_REMOVE_SMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            DISABLE_REMOVE_SMODS = settings.optBoolean(attemptingToLoad, false);
            // if true, disables the remove s-mods hullmod, as god intended
            Global.getSettings().getHullModSpec(REMOVE_SMOD_ID).setHidden(DISABLE_REMOVE_SMODS);
            Global.getSettings().getHullModSpec(REMOVE_SMOD_ID).setHiddenEverywhere(DISABLE_REMOVE_SMODS);

            attemptingToLoad = ALLOW_STANDARD_SMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            ALLOW_STANDARD_SMODS = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = SELECTION_MODE_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getString(attemptingToLoad));
            SELECTION_MODE = settings.getString(attemptingToLoad);

            attemptingToLoad = TRUE_RANDOM_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            TRUE_RANDOM = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = ONLY_KNOWN_HULLMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            ONLY_KNOWN_HULLMODS = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = ONLY_NOT_HIDDEN_HULLMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            ONLY_NOT_HIDDEN_HULLMODS = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = ONLY_APPLICABLE_HULLMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            ONLY_APPLICABLE_HULLMODS = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = RESPECT_NO_BUILD_IN_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            RESPECT_NO_BUILD_IN = settings.getBoolean(attemptingToLoad);

            attemptingToLoad = PR_INCR_FOR_FREE_OR_HIDDEN_MODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getInt(attemptingToLoad));
            PR_INCR_FOR_FREE_OR_HIDDEN_MODS = settings.getInt(attemptingToLoad);

            attemptingToLoad = TG_COST_FOR_RARE2_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getInt(attemptingToLoad));
            TG_COST_FOR_RARE2 = settings.getInt(attemptingToLoad);

            attemptingToLoad = MIN_REMOVED_SMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getInt(attemptingToLoad));
            MIN_REMOVED_SMODS = settings.getInt(attemptingToLoad);

            attemptingToLoad = MAX_REMOVED_SMODS_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getInt(attemptingToLoad));
            MAX_REMOVED_SMODS = settings.getInt(attemptingToLoad);

            attemptingToLoad = TG_RARE1_MULT_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getDouble(attemptingToLoad));
            TG_RARE1_MULT = (float) settings.getDouble(attemptingToLoad);

            attemptingToLoad = TG_RARE2_MULT_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getDouble(attemptingToLoad));
            TG_RARE2_MULT = (float) settings.getDouble(attemptingToLoad);

            attemptingToLoad = TG_RARE3_MULT_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getDouble(attemptingToLoad));
            TG_RARE3_MULT = (float) settings.getDouble(attemptingToLoad);

            attemptingToLoad = TG_RARE4_MULT_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getDouble(attemptingToLoad));
            TG_RARE4_MULT = (float) settings.getDouble(attemptingToLoad);

            // load blacklisted hullmods
            attemptingToLoad = BLACKLISTED_HULLMODS_ARRAY;
            log.info("Attempting to load " + attemptingToLoad);
            JSONArray array = settings.getJSONArray(attemptingToLoad);
            for (int i = 0; i < array.length(); i++) {
                BLACKLISTED_HULLMODS.add((String) array.get(i));
            }
            //log.info("Blacklisted hullmods: " + BLACKLISTED_HULLMODS);

            attemptingToLoad = USE_GREYLIST_SETTING;
            log.info("Attempting to load " + attemptingToLoad + ": " + settings.getBoolean(attemptingToLoad));
            USE_GREYLIST = settings.getBoolean(attemptingToLoad);
            if (USE_GREYLIST) {
                log.info("Adding greylisted hullmods to blacklist");
                JSONObject greylistJSON = Global.getSettings().getMergedJSONForMod(GREYLIST_JSON, MOD_ID);
                JSONArray greylist = greylistJSON.getJSONArray(GREYLISTED_HULLMODS_ARRAY);
                for (int i = 0; i < greylist.length(); i++) {
                    BLACKLISTED_HULLMODS.add((String) greylist.get(i));
                }
                if (USE_ONLINE_BLACKLIST) {
                    log.info("---------------------------------");
                    log.info("---- LOADING ONLINE GREYLIST ----");
                    log.info("---------------------------------");
                    loadOnlineBlackList(ONLINE_GREYLIST_URL, GREYLISTED_HULLMODS_ARRAY, log);
                }
            }

            LOADING_FAILED = false;
            log.info("Loading " + MOD_ID + "/" + SETTINGS_JSON + " completed");
        } catch (IOException | JSONException e) {
            LOADING_FAILED = true;
            log.error("Could not load " + MOD_ID + "/" + SETTINGS_JSON + ": problem loading " + attemptingToLoad, e);
        }
    }
}