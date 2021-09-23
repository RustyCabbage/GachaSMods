package data.scripts;

import com.fs.starfarer.api.Global;

public class GachaSMods_Utils {

    // just gonna save any variables I use multiple times in here, whatever
    public static final String MOD_ID = "GachaSMods";
    // keys saved to sector.getPersistentData()
    public static final String SEED_KEY = MOD_ID + "_" + "SEED_KEY";
    public static final String LOAD_KEY = MOD_ID + "_" + "LOAD_KEY";
    public static final String TEMP_SEED_KEY = MOD_ID + "_" + "TEMP_SEED_KEY";
    // settings in data/config/settings.json
    public static final String TRUE_RANDOM_SETTING = MOD_ID + "_" + "trueRandomMode";
    public static final String NO_SAVE_SCUMMING_SETTING = MOD_ID + "_" + "noSaveScumming";
    public static final String ONLY_KNOWN_HULLMODS_SETTING = MOD_ID + "_" + "onlyKnownHullmods";
    public static final String ONLY_NOT_HIDDEN_HULLMODS_SETTING = MOD_ID + "_" + "onlyNotHiddenHullmods";
    public static final String ONLY_APPLICABLE_HULLMODS_SETTING = MOD_ID + "_" + "onlyApplicableHullmods";

    public static String getString(String id) {
        return Global.getSettings().getString(MOD_ID, id);
    }

}
