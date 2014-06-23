package cz.vity.freerapid.plugins.services.stiahnisi;

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
            //httpFile.setNewURL(new URL("http://www.stiahni.si/download.php?id=6685"));
            //httpFile.setNewURL(new URL("http://www.stiahni.si/download.php?id=17943"));
            httpFile.setNewURL(new URL("http://www.stiahni.si/file/8108619"));
            //httpFile.setNewURL(new URL("http://www.stiahni.si/download.php?id=17925")); //server error
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("159.253.237.14", 8080); //eg we can use local proxy to sniff HTTP communication
            final StiahniSiServiceImpl service = new StiahniSiServiceImpl();
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