package cz.vity.freerapid.plugins.services.myzukaru;

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
            //httpFile.setNewURL(new URL("http://www.myzuka.ru/Song/217112/The-Buzzcocks-Fast-Cars"));
            //httpFile.setNewURL(new URL("http://www.myzuka.ru/Album/377274/The-Cranberries-Bualadh-Bos-The-Cranberries-Live"));
            httpFile.setNewURL(new URL("http://www.myzuka.ru/Song/3178096/The-Cranberries-Waltzing-Back"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final MyZukaRuServiceImpl service = new MyZukaRuServiceImpl();
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