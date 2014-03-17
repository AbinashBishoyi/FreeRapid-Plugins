package cz.vity.freerapid.plugins.services.exclusivefaile;

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
            httpFile.setNewURL(new URL("http://exclusivefaile.com/bhkbik6zguge/SmartStay_Ex_Pro_v1.3.3.1.zip.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final ExclusiveFaileServiceImpl service = new ExclusiveFaileServiceImpl();
			
			//we set premium account details
            //final PremiumAccount config = new PremiumAccount();
            //config.setUsername("freerapid");
            //config.setPassword("freerapid");
            //service.setConfig(config);
			
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