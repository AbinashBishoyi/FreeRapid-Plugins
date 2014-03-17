package cz.vity.freerapid.plugins.services.raagfm;

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
            //httpFile.setNewURL(new URL("http://music.raag.fm/UK_Punjabi/songs-37065-Nain_Nashiley-Binnie_Marwa"));
            //httpFile.setNewURL(new URL("http://player.raag.fm/player/flash/flash-hd.asx?pick%5B%5D=414620&sec1Code=robol111@123"));
            //httpFile.setNewURL(new URL("http://music.raag.fm/Punjabi/songs-37064-Punjabi_Sher_(Single)-Bakshi_Billa"));
            //httpFile.setNewURL(new URL("http://player.raag.fm/player/flash/flash.asx?pick%5B%5D=414619&sec1Code=robol111@123"));
            httpFile.setNewURL(new URL("http://music.raag.fm/Punjabi/songs-99964-Punjabi_Sher_(Single)-Bakshi_Billa"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final RaagFmServiceImpl service = new RaagFmServiceImpl();
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