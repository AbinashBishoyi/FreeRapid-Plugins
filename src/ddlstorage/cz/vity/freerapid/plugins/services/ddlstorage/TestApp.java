package cz.vity.freerapid.plugins.services.ddlstorage;

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
            //httpFile.setNewURL(new URL("http://www.ddlstorage.com/8gaqzz5s43jt"));
            httpFile.setNewURL(new URL("http://www.ddlstorage.com/it6sajo9ksm1/DRK.SHDWS.12.80.TRL.part069.rar.htm"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("118.99.80.98", 8080); //eg we can use local proxy to sniff HTTP communication
            final DdlStorageServiceImpl service = new DdlStorageServiceImpl();
            /*
            PremiumAccount config = new PremiumAccount();
            config.setUsername("***");
            config.setPassword("***");
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