package cz.vity.freerapid.plugins.services.pbskids;

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
            //httpFile.setNewURL(new URL("http://pbskids.org/curiousgeorge/video/"));
            //httpFile.setNewURL(new URL("http://pbskids.org/sid/videoplayer.html"));
            httpFile.setNewURL(new URL("http://pbskids.org/cyberchase/videos/#!/seasons-1-8/1?guid=601ad933-d277-463c-b9e0-5ec3bb0ef795"));
            //httpFile.setNewURL(new URL("http://pbskids.org/curiousgeorge/video/?guid=46d5620a-b232-4202-a070-0cbcccad2d7a"));
            //httpFile.setNewURL(new URL("http://pbskids.org/sid/videoplayer.html?guid=7bc6d1c3-74a8-4c92-8c56-2212109aab55"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final PbsKidsServiceImpl service = new PbsKidsServiceImpl();
            SettingsConfig config = new SettingsConfig();
            config.setVideoQuality(VideoQuality._800);
            service.setConfig(config);
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