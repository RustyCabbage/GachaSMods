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

import java.util.Iterator;

import static data.scripts.GachaSMods_Utils.*;

public class GachaSMods_ModPlugin extends BaseModPlugin {

    private static final Logger log = Global.getLogger(GachaSMods_ModPlugin.class);

    //public static String SETTINGS_JSON = "data/config/settings.json";

    @Override
    public void onGameLoad(boolean newGame) {
        /* todo: oops this absolutely not what I wanted but I don't feel like fixing it right at this moment
        // basically just change settings so they work on game load instead of application load but like who even cares
        try {
            JSONObject settings = Global.getSettings().loadJSON(SETTINGS_JSON, MOD_ID);
        } catch (IOException | JSONException e) {
            log.error("Could not load " + MOD_ID + "/" + SETTINGS_JSON);
        }
        */
        // load blacklisted hullmods
        try {
            JSONArray array = Global.getSettings().getJSONArray(BLACKLISTED_HULLMODS_ARRAY);
            for (int i = 0; i < array.length(); i++) {
                BLACKLISTED_HULLMODS.add((String) array.get(i));
            }
            //log.info("Blacklisted hullmods: " + BLACKLISTED_HULLMODS);
        } catch (JSONException e) {
            log.error("Could not load " + BLACKLISTED_HULLMODS_ARRAY);
        }
        // disables the random s-mods hullmod, which kinda makes this whole mod moot but whatever maybe they just want the s-mod removing features
        boolean disableRandomSMods = Global.getSettings().getBoolean(DISABLE_RANDOM_SMODS);
        Global.getSettings().getHullModSpec(RANDOM_SMOD_ID).setHiddenEverywhere(disableRandomSMods);
        Global.getSettings().getHullModSpec(RANDOM_SMOD_ID).setHidden(disableRandomSMods); // you have to set both hidden and hiddenEverywhere to true, TIL
        // disables the remove s-mods hullmod, as god intended
        boolean disableRemoveSMods = Global.getSettings().getBoolean(DISABLE_REMOVE_SMODS);
        Global.getSettings().getHullModSpec(REMOVE_SMOD_ID).setHidden(disableRemoveSMods);
        Global.getSettings().getHullModSpec(REMOVE_SMOD_ID).setHiddenEverywhere(disableRemoveSMods);
        // block building-in all the other hullmods
        if (!(Global.getSettings().getBoolean(ALLOW_STANDARD_SMODS) || disableRandomSMods)) {
            for (HullModSpecAPI hullModSpec : Global.getSettings().getAllHullModSpecs()) {
                if (!hullModSpec.getId().startsWith(MOD_ID)) {
                    hullModSpec.addTag(Tags.HULLMOD_NO_BUILD_IN);
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
        // should these hullmods require a spaceport? no Tags.REQUIRES_SPACEPORT, so I am too lazy
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

    @Override
    public void afterGameSave() {
        FactionAPI faction = Global.getSector().getPlayerFaction();
        for (HullModSpecAPI hullmod : Global.getSettings().getAllHullModSpecs()) { // maybe should just write the two hullmods, but future-proofing?
            if (hullmod.hasTag(MOD_ID)) {
                faction.addKnownHullMod(hullmod.getId());
            }
        }
    }
}