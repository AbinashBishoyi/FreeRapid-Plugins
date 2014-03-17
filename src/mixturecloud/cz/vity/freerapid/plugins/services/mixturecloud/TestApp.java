package cz.vity.freerapid.plugins.services.mixturecloud;

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
            //httpFile.setNewURL(new URL("http://www.mixturecloud.com/video=S3aBiJ"));
            httpFile.setNewURL(new URL("http://www.mixturecloud.com/download=NcqGQF"));
            //httpFile.setNewURL(new URL("http://www.mixturecloud.com/media/download/PFzTy3a8"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final MixtureCloudServiceImpl service = new MixtureCloudServiceImpl();
            //we set account details
            //final PremiumAccount config = new PremiumAccount();
            //config.setUsername("flashgordon3928372@spamdecoy.net");
            //config.setPassword("flashgordon");
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