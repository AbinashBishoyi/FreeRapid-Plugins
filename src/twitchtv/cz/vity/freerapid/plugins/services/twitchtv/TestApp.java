package cz.vity.freerapid.plugins.services.twitchtv;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://cs.twitch.tv/wries/b/319029303"));
            //httpFile.setNewURL(new URL("http://www.twitch.tv/paradoxinteractive/b/326746473"));
            //httpFile.setNewURL(new URL("http://www.twitch.tv/tsm_chaox/b/328855029"));
            //httpFile.setNewURL(new URL("http://media6.justin.tv/archives/2012-8-30/live_user_wries_1346350462.flv/česká kvalifikace na ESWC!! 2/4_1346350462")); //videoUrl
            //httpFile.setNewURL(new URL("http://www.justin.tv/wries/b/330488455"));
            httpFile.setNewURL(new URL("http://www.justin.tv/tsm_chaox/b/328855029"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final TwitchTvServiceImpl service = new TwitchTvServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}