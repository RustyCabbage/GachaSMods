package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import static data.scripts.GachaSMods_Utils.REMOVE_SMOD_ID;
import static data.scripts.GachaSMods_Utils.getString;

public class GachaSMods_PLACEHOLDER extends BaseHullMod {

    private final Logger log = Global.getLogger(GachaSMods_PLACEHOLDER.class);

    // for save compatibility
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ShipVariantAPI variant = ship.getVariant();
        if (!variant.getNonBuiltInHullmods().contains(REMOVE_SMOD_ID)) {
            variant.removePermaMod(spec.getId());
            variant.removeMod(spec.getId());
        }
    }

    // tip changes every time you mouseover, which is probably the best way to handle it anyways
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        String SECTION_HEADING = getString("placeholderSectionHeading"); // "Blink and you'll miss me"
        String TIPS_PATH = "data/strings/tips.json";
        String STARSECTOR_CORE = "starsector-core";
        float PAD = 5f;
        try {
            JSONObject tips = Global.getSettings().getMergedJSONForMod(TIPS_PATH, STARSECTOR_CORE);
            WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
            JSONArray array = tips.getJSONArray("tips");
            for (int i = 0; i < array.length(); i++) {
                String tip = null;
                float freq = 1;
                if (array.get(i) instanceof JSONObject) {
                    JSONObject row = array.getJSONObject(i);
                    freq = (float) row.getDouble("freq");
                    tip = row.getString("tip");
                } else if (array.get(i) instanceof String) {
                    tip = array.getString(i);
                }
                if (tip != null && freq > 0) {
                    picker.add(tip, freq);
                }
            }
            String pick = picker.pick();
            tooltip.addSectionHeading(SECTION_HEADING, Alignment.MID, -20f);
            tooltip.addPara(pick, PAD);
        } catch (IOException | JSONException e) {
            log.error("Could not load " + TIPS_PATH, e);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }

    @Override
    public int getDisplaySortOrder() {
        return -1;
    } // shows before the real s-mods
}
