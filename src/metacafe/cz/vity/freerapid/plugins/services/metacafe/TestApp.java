package cz.vity.freerapid.plugins.services.metacafe;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www.metacafe.com/watch/4725852/vanquish_trailer_e3_2010/"));
            httpFile.setNewURL(new URL("http://www.metacafe.com/watch/5950276/caroline_tillette_1/")); //18+
            //httpFile.setNewURL(new URL("http://www.metacafe.com/watch/yt-hCPbOsfRaD4/we_are_never_ever_getting_back_together_taylor_swift_official_music_cover_by_tiffany_alvord/")); //youtube
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final MetaCafeServiceImpl service = new MetaCafeServiceImpl();
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