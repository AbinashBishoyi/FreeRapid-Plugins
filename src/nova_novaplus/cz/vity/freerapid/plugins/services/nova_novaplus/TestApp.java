package cz.vity.freerapid.plugins.services.nova_novaplus;

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
            //httpFile.setNewURL(new URL("http://novaplus.nova.cz/porad/comeback/video/2517-comeback-34-dil-zlata-ledvina"));
            //httpFile.setNewURL(new URL("http://novaplus.nova.cz/porad/velka-kucharka-ladi-hrusky/video/1976-levne-a-chutne-domaci-malinovka"));
            httpFile.setNewURL(new URL("http://novaplus.nova.cz/porad/televizni-noviny/video/2711-televizni-noviny-30-9-2014"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final Nova_NovaPlusServiceImpl service = new Nova_NovaPlusServiceImpl();
            SettingsConfig config = new SettingsConfig();
            config.setVideoQuality(VideoQuality.HQ);
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