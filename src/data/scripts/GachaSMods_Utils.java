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
    // hullmod ids
    public static final String
            RANDOM_SMOD_ID = MOD_ID + "_" + "randomSMod",
            REMOVE_SMOD_ID = MOD_ID + "_" + "removeSMods",
            PLACEHOLDER_ID = MOD_ID + "_" + "PLACEHOLDER",
            HIDDEN_FIX_ID = MOD_ID + "_" + "hiddenHullModFix"; // todo find a way to make this work
    public static final Set<String> BLACKLISTED_HULLMODS = new HashSet<>(); // blacklisted hullmods

    public static String getString(String id) {
        return Global.getSettings().getString(MOD_ID, id);
    }
}
