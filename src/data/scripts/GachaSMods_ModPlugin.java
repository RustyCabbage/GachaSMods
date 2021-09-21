package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(GachaSMods_ModPlugin.class);

    public static String GACHA_HULLMOD_ID = "GachaSMods_randomSMod";
    public static String SETTINGS_JSON = "data/config/settings.json";
    public static String MOD_ID = "GachaSMods";
    public static String LOAD_JSON_ERROR = "Could not load " + MOD_ID + "/" + SETTINGS_JSON;

    @Override
    public void onGameLoad(boolean newGame) {
        /* todo: oops this absolutely not what I wanted but I can't fix it right at this moment
        try {
            Global.getSettings().loadJSON(SETTINGS_JSON, MOD_ID);
        } catch (IOException | JSONException e) {
            log.error(LOAD_JSON_ERROR);
        }
        */
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) {
            if (!hullmod.hasTag(Tags.HULLMOD_NO_BUILD_IN) && !hullmod.getId().equals(GACHA_HULLMOD_ID)) {
                hullmod.addTag(Tags.HULLMOD_NO_BUILD_IN);
            }
        }
    }

    @Override
    public void beforeGameSave() {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (faction.getKnownHullMods().contains(GACHA_HULLMOD_ID)) {
                faction.removeKnownHullMod(GACHA_HULLMOD_ID);
            }
        }
    }

    @Override
    public void afterGameSave() {
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            if (!faction.getKnownHullMods().contains(GACHA_HULLMOD_ID)) {
                faction.addKnownHullMod(GACHA_HULLMOD_ID);
            }
        }
    }
}