package cz.vity.freerapid.plugins.services.sharexxpgcombr;

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
            //httpFile.setNewURL(new URL("http://sharex.xpg.com.br/files/2389317510/narutoPROJECT_-_Shippuuden_228.rmvb.html"));
            //httpFile.setNewURL(new URL("http://sharex.xpg.com.br/files/8024086009/PikaMucha_Itu_Exterminator2_mpgh.net.rar.html"));
            httpFile.setNewURL(new URL("http://sharex.xpg.com.br/files/5108722382/golclub_20-05-12.rar.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.setProxy("200.253.158.131", 3128); //brazilian proxy is a must
            final SharexXpgComBrServiceImpl service = new SharexXpgComBrServiceImpl();
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