package cz.vity.freerapid.plugins.services.videocopilotnet;

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
            //httpFile.setNewURL(new URL("http://www.videocopilot.net/tutorials/explosive_training/")); //main tutorial
            //httpFile.setNewURL(new URL("http://www.videocopilot.net/tutorial/explosive_training/p3/")); // tutorial video page
            //httpFile.setNewURL(new URL("http://video9.videocopilot.net/efc5e19e65fe9ca2cbd9818d45ac92a1/videotutorials/projects/137.zip?fname=137.+Explosive+Training.zip")); //project
            //httpFile.setNewURL(new URL("http://www.videocopilot.net/tutorials/colorful_universe/"));
            //httpFile.setNewURL(new URL("http://www.videocopilot.net/tutorials/scopes/"));
            //httpFile.setNewURL(new URL("http://www.videocopilot.net/tutorials/basic_sky_replacement/"));
            httpFile.setNewURL(new URL("http://www.videocopilot.net/tutorials/depth_compositing/"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final VideoCoPilotNetServiceImpl service = new VideoCoPilotNetServiceImpl();

            VideoCoPilotNetSettingsConfig config = new VideoCoPilotNetSettingsConfig();
            config.setVideoQuality(VideoQuality.SD);
            config.setDownloadProject(true);
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