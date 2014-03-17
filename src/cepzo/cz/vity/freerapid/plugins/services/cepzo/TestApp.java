package cz.vity.freerapid.plugins.services.cepzo;

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
            httpFile.setNewURL(new URL("http://cepzo.com/8tomjhvik02q/14a64b174c.wmv.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.setProxy("118.99.71.240", 3128); //eg we can use local proxy to sniff HTTP communication
            final CepzoServiceImpl service = new CepzoServiceImpl();
			
			//we set premium account details
            //final PremiumAccount config = new PremiumAccount();
            //config.setUsername("***");
            //config.setPassword("***");
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