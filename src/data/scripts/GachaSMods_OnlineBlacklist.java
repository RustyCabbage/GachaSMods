package data.scripts;

import com.fs.starfarer.api.Global;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Scanner;

import static data.scripts.GachaSMods_Utils.BLACKLISTED_HULLMODS;
import static data.scripts.GachaSMods_Utils.CUSTOM_WEIGHTS_MAP;

public class GachaSMods_OnlineBlacklist {

    private final Logger log = Global.getLogger(GachaSMods_OnlineBlacklist.class);

    // Stole basically all of this from LazyWizard's Version Checker
    public static void loadOnlineBlackList(String url, String arrayKey, Logger log) {
        System.setProperty("https.protocols", "SSLv3,TLSv1,TLSv1.1,TLSv1.2");
        try {
            InputStream stream = new URL(url).openStream();
            Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
            JSONObject settings = sanitizeJSON(scanner.next());
            JSONArray array = settings.getJSONArray(arrayKey);
            for (int i = 0; i < array.length(); i++) {
                BLACKLISTED_HULLMODS.add((String) array.get(i));
            }
            log.info("Loading from online list was successful");
        } catch (MalformedURLException ex) {
            log.error("Invalid URL \"" + url + "\"", ex);
        } catch (IOException ex) {
            log.error("Failed to load JSON file from URL \"" + url + "\"", ex);
        } catch (JSONException ex) {
            log.error("Malformed JSON at URL \"" + url + "\"", ex);
        }
    }

    // JSONObject instead
    public static void loadOnlineWeightMults(String url, String objKey, Logger log) {
        System.setProperty("https.protocols", "SSLv3,TLSv1,TLSv1.1,TLSv1.2");
        try {
            InputStream stream = new URL(url).openStream();
            Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
            JSONObject settings = sanitizeJSON(scanner.next());
            JSONObject classToWeightsObject = settings.getJSONObject(objKey);
            JSONArray effectClassArray = classToWeightsObject.names();
            for (int i = 0; i < effectClassArray.length(); i++) {
                String effectClass = effectClassArray.getString(i);
                float weightMult = (float) classToWeightsObject.getDouble(effectClass);
                CUSTOM_WEIGHTS_MAP.put(effectClass, weightMult);
            }
            log.info("Loading online weights was successful");
        } catch (MalformedURLException ex) {
            log.error("Invalid URL \"" + url + "\"", ex);
        } catch (IOException ex) {
            log.error("Failed to load JSON file from URL \"" + url + "\"", ex);
        } catch (JSONException ex) {
            log.error("Malformed JSON at URL \"" + url + "\"", ex);
        }
    }

    // Stole basically all of this from LazyWizard's Version Checker
    private static JSONObject sanitizeJSON(final String rawJSON) throws JSONException {
        StringBuilder result = new StringBuilder(rawJSON.length());
        // Remove elements that default JSON implementation can't parse
        for (final String str : rawJSON.split("\n")) {
            // Strip out whole-line comments
            if (str.trim().startsWith("#")) {
                continue;
            }

            // Strip out end-line comments
            if (str.contains("#")) {
                result.append(str, 0, str.indexOf('#'));
            } else {
                result.append(str);
            }
        }
        return new JSONObject(result.toString());
    }


}