package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.apache.log4j.Logger;

import static data.scripts.GachaSMods_Utils.MOD_ID;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(GachaSMods_ModPlugin.class);

    public static String RANDOM_SMOD_ID = MOD_ID + "_" + "randomSMod";
    public static String REMOVE_SMOD_ID = MOD_ID + "_" + "removeSMods";
    //public static String SETTINGS_JSON = "data/config/settings.json"; // todo: Idk does this need to be externalized?
    //public static String LOAD_JSON_ERROR = "Could not load " + MOD_ID + "/" + SETTINGS_JSON;

    @Override
    public void onGameLoad(boolean newGame) {
        /* todo: oops this absolutely not what I wanted but I don't feel like fixing it right at this moment
        try {
            Global.getSettings().loadJSON(SETTINGS_JSON, MOD_ID);
        } catch (IOException | JSONException e) {
            log.error(LOAD_JSON_ERROR);
        }
        */
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) {
            if (!hullmod.hasTag(Tags.HULLMOD_NO_BUILD_IN)
                    && !((hullmod.getId().equals(RANDOM_SMOD_ID) || hullmod.getId().equals(REMOVE_SMOD_ID)))) {
                hullmod.addTag(Tags.HULLMOD_NO_BUILD_IN);
            }
        }
        // todo: should these hullmods require a spaceport?
    }

    @Override
    public void beforeGameSave() {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.getKnownHullMods().contains(RANDOM_SMOD_ID)) {
                faction.removeKnownHullMod(RANDOM_SMOD_ID);
            }
        }
    }

    @Override
    public void afterGameSave() {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!faction.getKnownHullMods().contains(RANDOM_SMOD_ID)) {
                faction.addKnownHullMod(RANDOM_SMOD_ID);
            }
        }
    }
}