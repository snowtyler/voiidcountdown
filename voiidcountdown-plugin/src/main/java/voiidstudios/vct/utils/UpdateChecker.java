package voiidstudios.vct.utils;

import org.bukkit.event.Listener;
import voiidstudios.vct.api.UpdateCheckerResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker implements Listener {
    private static final int RESOURCE_ID = 127616;
    private String version;
    private String latestVersion;

    public UpdateChecker(String version){
        this.version = version;
    }

    public UpdateCheckerResult check(){
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + RESOURCE_ID).openConnection();
            int time_out = 2000;
            connection.setConnectTimeout(time_out);
            connection.setReadTimeout(time_out);
            latestVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
            if (latestVersion.length() <= 12) { // The version should not be as long as “1.4.0.12-rc1”
                if(!version.equals(latestVersion)){
                    return UpdateCheckerResult.noErrors(latestVersion);
                }
            }
            return UpdateCheckerResult.noErrors(null);
        } catch (Exception ex) {
            return UpdateCheckerResult.error();
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
