package cz.vity.freerapid.plugins.services.ryushare;

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
            //httpFile.setNewURL(new URL("http://ryushare.com/5amx6a3b787o/1449329780.rar"));
            //httpFile.setNewURL(new URL("http://ryushare.com/3b35466010ca/CS101.rar"));
            httpFile.setNewURL(new URL("http://ryushare.com/1fec5b0863b7/Immersion3.part02.rar"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final RyuShareServiceImpl service = new RyuShareServiceImpl();
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
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}