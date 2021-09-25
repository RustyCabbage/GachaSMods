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

import java.util.ArrayList;

import static data.scripts.GachaSMods_Utils.MOD_ID;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(GachaSMods_ModPlugin.class);

    //public static String SETTINGS_JSON = "data/config/settings.json"; // todo: Idk does this need to be externalized?
    //public static String LOAD_JSON_ERROR = "Could not load " + MOD_ID + "/" + SETTINGS_JSON;

    @Override
    public void onGameLoad(boolean newGame) {
        /* todo: oops this absolutely not what I wanted but I don't feel like fixing it right at this moment
        // basically just change settings so they work on game load instead of application load but like who even cares
        try {
            JSONObject settings = Global.getSettings().loadJSON(SETTINGS_JSON, MOD_ID);
        } catch (IOException | JSONException e) {
            log.error(LOAD_JSON_ERROR);
        }
        */
        // todo: should these hullmods require a spaceport? no Tags.REQUIRES_SPACEPORT so I am too lazy
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
        long tick = System.currentTimeMillis();
        // check for ships in storage / sold to markets
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            for (SubmarketAPI submarket : market.getSubmarketsCopy()) {
                for (FleetMemberAPI m : submarket.getCargo().getMothballedShips().getMembersListCopy()) {
                    for (String hullModId : m.getVariant().getHullMods()) {
                        if (Global.getSettings().getHullModSpec(hullModId).hasTag(MOD_ID)) {
                            m.getVariant().removeMod(hullModId);
                        }
                    }
                }
            }
        }
        // frick what if the Cabal steals one or something
        for (LocationAPI loc : Global.getSector().getAllLocations()) {
            for (CampaignFleetAPI f : loc.getFleets()) {
                for (FleetMemberAPI m : f.getFleetData().getMembersListCopy()) {
                    for (String hullModId : m.getVariant().getHullMods()) {
                        if (Global.getSettings().getHullModSpec(hullModId).getId().startsWith(MOD_ID)) {
                            m.getVariant().removeMod(hullModId);
                            m.getVariant().removePermaMod(hullModId);
                        }
                    }
                }
            }
        }
        long tock = System.currentTimeMillis();
        log.info("Time to clean save of hullmods (ms): " + (tock - tick)); //in my test this took 1 ms
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