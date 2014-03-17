package cz.vity.freerapid.plugins.services.movzap;

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
            httpFile.setNewURL(new URL("http://www.movzap.com/mvqc0k1nln35.html"));
            //httpFile.setNewURL(new URL("http://movzap.com/knsvdyf7y6hz")); //HD
            //httpFile.setNewURL(new URL("http://movzap.com/mml83qi1yzp5")); //HD
            //httpFile.setNewURL(new URL("http://movzap.com/34gn5gnent7w.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final MovZapServiceImpl service = new MovZapServiceImpl();
            /*
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("freerapid");
            config.setPassword("freerapid");
            service.setConfig(config);
            */
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