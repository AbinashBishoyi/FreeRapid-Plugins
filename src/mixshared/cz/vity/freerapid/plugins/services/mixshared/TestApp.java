package cz.vity.freerapid.plugins.services.mixshared;

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
            //httpFile.setNewURL(new URL("http://mixshared.com/zzj7dyuyenxt"));
            httpFile.setNewURL(new URL("http://mixshared.com/dzl4ivo9m8pz"));   //rar pswd : rizkyd@erdent.com
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final MixSharedServiceImpl service = new MixSharedServiceImpl();
			
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