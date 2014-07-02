package cz.vity.freerapid.plugins.services.canalplus;

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
            //httpFile.setNewURL(new URL("http://www.canalplus.fr/c-infos-documentaires/pid1830-c-zapping.html?vid=1089249"));
            httpFile.setNewURL(new URL("http://www.canalplus.fr/c-infos-documentaires/pid1830-c-zapping.html?vid=1096418"));
            //httpFile.setNewURL(new URL("http://www.canalplus.fr/c-infos-documentaires/pid1830-c-zapping.html?vid=1085169"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final CanalPlusServiceImpl service = new CanalPlusServiceImpl();
            SettingsConfig config = new SettingsConfig();
            config.setVideoQuality(VideoQuality._200);
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