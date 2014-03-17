package cz.vity.freerapid.plugins.services.filejungle;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author CrazyCoder
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://www.filejungle.com/f/Xdzjt3/Syngress.Migrating.to.the.Cloud.Oct.2011.rar"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
//            connectionSettings.setProxy("127.0.0.1", 8888);
            final FileJungleServiceImpl service = new FileJungleServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
