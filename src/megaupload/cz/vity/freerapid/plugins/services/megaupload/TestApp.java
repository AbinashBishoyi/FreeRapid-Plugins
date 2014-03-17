package cz.vity.freerapid.plugins.services.megaupload;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://www.megaupload.com/?d=XDRWHKBQ"));
            //httpFile.setNewURL(new URL("http://www.megaupload.com/?f=054OTS7Y"));//folder
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081);
            final MegauploadShareServiceImpl serviceImpl = new MegauploadShareServiceImpl();
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("***");
            config.setPassword("***");
            //serviceImpl.setConfig(config);
            testRun(serviceImpl, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}