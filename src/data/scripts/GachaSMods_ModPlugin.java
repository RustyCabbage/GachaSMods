package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.apache.log4j.Logger;

import java.util.ArrayList;

import static data.scripts.GachaSMods_Utils.*;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(GachaSMods_ModPlugin.class);

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
        // todo: should these hullmods require a spaceport?
        // block building in all other hullmods
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) {
            if (!hullmod.hasTag(Tags.HULLMOD_NO_BUILD_IN)
                    && !hullmod.hasTag(MOD_ID)) {
                hullmod.addTag(Tags.HULLMOD_NO_BUILD_IN);
            }
        }
        // remove hullmods from being known by other factions for save compatibility
        for (FactionAPI faction : Global.getSector().getAllFactions()) {
            ArrayList<String> hullModsToRemove = new ArrayList<>();
            if (faction.getId().equals(Factions.PLAYER)) {
                continue;
            }
            for (String hullModId : faction.getKnownHullMods()) {
                if (Global.getSettings().getHullModSpec(hullModId).hasTag(MOD_ID)) {
                    hullModsToRemove.add(hullModId);
                }
            }
            for (String hullModId : hullModsToRemove) {
                faction.removeKnownHullMod(hullModId);
            }
        }
    }

    // for save compatibility
    @Override
    public void beforeGameSave() {
        FactionAPI faction = Global.getSector().getPlayerFaction();
        ArrayList<String> hullModsToRemove = new ArrayList<>();
        for (String hullModId : faction.getKnownHullMods()) {
            if (Global.getSettings().getHullModSpec(hullModId).hasTag(MOD_ID)) {
                hullModsToRemove.add(hullModId);
            }
        }
        for (String hullModId : hullModsToRemove) {
            faction.removeKnownHullMod(hullModId);
        }
    }

    @Override
    public void afterGameSave() {
        FactionAPI faction = Global.getSector().getPlayerFaction();
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) {
            if (hullmod.hasTag(MOD_ID)) {
                faction.addKnownHullMod(hullmod.getId());
            }
        }
    }
}