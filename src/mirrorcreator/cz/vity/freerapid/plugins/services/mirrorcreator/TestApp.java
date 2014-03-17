package cz.vity.freerapid.plugins.services.mirrorcreator;

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
            //httpFile.setNewURL(new URL("http://mir.cr/Z2KBKXPT"));
            httpFile.setNewURL(new URL("http://www.mirrorcreator.com/files/MDGTGXGM/"));
            //httpFile.setNewURL(new URL("http://mir.cr/05UZGDWS"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081);
            final MirrorCreatorServiceImpl service = new MirrorCreatorServiceImpl();
            final MirrorCreatorSettingsConfig config = new MirrorCreatorSettingsConfig();
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