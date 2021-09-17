package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    public static String GACHA_HULLMOD_ID = "GachaSMods_randomSMod";

    @Override
    public void onGameLoad(boolean newGame) {
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) {
            if (!hullmod.hasTag(Tags.HULLMOD_NO_BUILD_IN) && !hullmod.getId().equals(GACHA_HULLMOD_ID)) {
                hullmod.addTag(Tags.HULLMOD_NO_BUILD_IN);
            }
        }
    }
}