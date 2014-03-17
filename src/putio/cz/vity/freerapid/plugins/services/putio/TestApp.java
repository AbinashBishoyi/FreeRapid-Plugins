package cz.vity.freerapid.plugins.services.putio;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
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
            //httpFile.setNewURL(new URL("http://put.io/file/44421846"));
            //httpFile.setNewURL(new URL("http://put.io/your-files/44420707")); //collection
            //httpFile.setNewURL(new URL("https://put.io/file/44420710"));
            //httpFile.setNewURL(new URL("https://put.io/file/44441573")); //srt
            httpFile.setNewURL(new URL("https://put.io/file/45305550"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final PutIoServiceImpl service = new PutIoServiceImpl();

            //we set premium account details
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("***");
            config.setPassword("***");
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