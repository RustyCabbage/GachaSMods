package data.scripts;

import com.fs.starfarer.api.Global;

public class GachaSMods_Utils {

    // just gonna save any variables I use multiple times in here, whatever
    public static final String MOD_ID = "GachaSMods";
    // keys saved to sector.getPersistentData()
    public static final String SAVED_SEED = MOD_ID + "_" + "SAVED SEED";
    public static final String SHOULD_LOAD_SEED = MOD_ID + "_" + "SHOULD_LOAD_SEED";
    public static final String TEMP_SEED = MOD_ID + "_" + "TEMP_SEED";
    // settings in data/config/settings.json
    public static final String TRUE_RANDOM_SETTING = MOD_ID + "_" + "trueRandomMode";
    public static final String NO_SAVE_SCUMMING_SETTING = MOD_ID + "_" + "noSaveScumming";
    public static final String ONLY_KNOWN_HULLMODS_SETTING = MOD_ID + "_" + "onlyKnownHullmods";
    public static final String ONLY_NOT_HIDDEN_HULLMODS_SETTING = MOD_ID + "_" + "onlyNotHiddenHullmods";
    public static final String ONLY_APPLICABLE_HULLMODS_SETTING = MOD_ID + "_" + "onlyApplicableHullmods";
    public static final String DISABLE_RANDOM_SMODS = MOD_ID + "_" + "disableRandomSMods";
    public static final String DISABLE_REMOVE_SMODS = MOD_ID + "_" + "disableRemoveSMods";
    public static final String ALLOW_STANDARD_SMODS = MOD_ID + "_" + "allowStandardSMods";
    public static final String MIN_REMOVED_SMODS = MOD_ID + "_" + "minRemovedSMods";
    public static final String MAX_REMOVED_SMODS = MOD_ID + "_" + "maxRemovedSMods";
    // hullmod ids
    public static final String RANDOM_SMOD_ID = MOD_ID + "_" + "randomSMod";
    public static final String REMOVE_SMOD_ID = MOD_ID + "_" + "removeSMods";
    public static final String PLACEHOLDER_ID = MOD_ID + "_" + "PLACEHOLDER";

    public static String getString(String id) {
        return Global.getSettings().getString(MOD_ID, id);
    }

}
