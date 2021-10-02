package data.scripts;

import com.fs.starfarer.api.Global;

import java.util.HashSet;
import java.util.Set;

public class GachaSMods_Utils {

    // just gonna save any variables I use multiple times in here, whatever
    public static final String
            MOD_ID = "GachaSMods",
            PROPORTIONAL_CODE = "PR",
            TRUE_GACHA_CODE = "TG";
    // keys saved to sector.getPersistentData()
    public static final String
            SAVED_SEED = MOD_ID + "_" + "SAVED SEED",
            SHOULD_LOAD_SEED = MOD_ID + "_" + "SHOULD_LOAD_SEED",
            TEMP_SEED = MOD_ID + "_" + "TEMP_SEED";
    // settings in data/config/settings.json
    public static final String
            NO_SAVE_SCUMMING = MOD_ID + "_" + "noSaveScumming",
            DISABLE_RANDOM_SMODS = MOD_ID + "_" + "disableRandomSMods",
            DISABLE_REMOVE_SMODS = MOD_ID + "_" + "disableRemoveSMods",
            ALLOW_STANDARD_SMODS = MOD_ID + "_" + "allowStandardSMods",
            SELECTION_MODE = MOD_ID + "_" + "selectionMode",
            TRUE_RANDOM = MOD_ID + "_" + "trueRandomMode",
            BLACKLISTED_HULLMODS_ARRAY = MOD_ID + "_" + "blacklistedHullmods",
            ONLY_KNOWN_HULLMODS = MOD_ID + "_" + "onlyKnownHullmods",
            ONLY_NOT_HIDDEN_HULLMODS = MOD_ID + "_" + "onlyNotHiddenHullmods",
            ONLY_APPLICABLE_HULLMODS = MOD_ID + "_" + "onlyApplicableHullmods",
            PR_INCR_FOR_FREE_OR_HIDDEN_MODS = MOD_ID + "_" + PROPORTIONAL_CODE + "_" + "incrForFreeOrHiddenMods",
            TG_COST_FOR_RARE2 = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "costForRare2",
            TG_RARE1_MULT = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare1Mult",
            TG_RARE2_MULT = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare2Mult",
            TG_RARE3_MULT = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare3Mult",
            TG_RARE4_MULT = MOD_ID + "_" + TRUE_GACHA_CODE + "_" + "rare4Mult",
            MIN_REMOVED_SMODS = MOD_ID + "_" + "minRemovedSMods",
            MAX_REMOVED_SMODS = MOD_ID + "_" + "maxRemovedSMods";
    // hullmod ids
    public static final String
            RANDOM_SMOD_ID = MOD_ID + "_" + "randomSMod",
            REMOVE_SMOD_ID = MOD_ID + "_" + "removeSMods",
            PLACEHOLDER_ID = MOD_ID + "_" + "PLACEHOLDER";
    public static final Set<String> BLACKLISTED_HULLMODS = new HashSet<>(); // blacklisted hullmods

    public static String getString(String id) {
        return Global.getSettings().getString(MOD_ID, id);
    }
}
