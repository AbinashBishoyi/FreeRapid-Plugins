package cz.vity.freerapid.plugins.services.embedupload;

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
            //httpFile.setNewURL(new URL("http://www.embedupload.com/?d=9QAVQ4QVVJ"));
            //httpFile.setNewURL(new URL("http://www.embedupload.com/?d=6HKAAWEUU4"));
            //httpFile.setNewURL(new URL("http://www.embedupload.com/?FR=0XD5E1MIIN"));
            httpFile.setNewURL(new URL("http://www.embedupload.com/?d=0XD5E1MIIN"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final EmbedUploadServiceImpl service = new EmbedUploadServiceImpl();

            final EmbedUploadSettingsConfig config = new EmbedUploadSettingsConfig();
            config.setQueueAllLinks(true);
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