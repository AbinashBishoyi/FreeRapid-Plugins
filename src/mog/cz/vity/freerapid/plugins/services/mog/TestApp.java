package cz.vity.freerapid.plugins.services.mog;

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
            //httpFile.setNewURL(new URL("https://mog.com/m#album/82613449"));
            //httpFile.setNewURL(new URL("https://mog.com/m#album/81710335"));
            //httpFile.setNewURL(new URL("https://mog.com/m#track/82613453"));
            httpFile.setNewURL(new URL("https://mog.com/m#track/81710433"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("23.30.90.105", 8080); //eg we can use local proxy to sniff HTTP communication
            final MogServiceImpl service = new MogServiceImpl();
            /*
            PremiumAccount pa = new PremiumAccount();
            pa.setUsername("***");
            pa.setPassword("***");
            service.setConfig(pa);
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