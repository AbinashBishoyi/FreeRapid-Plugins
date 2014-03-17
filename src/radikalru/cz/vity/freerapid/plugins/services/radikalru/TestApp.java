package cz.vity.freerapid.plugins.services.radikalru;

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
            //httpFile.setNewURL(new URL("http://radikal.ru/F/s52.radikal.ru/i136/0809/c7/03ccc9ac7043.jpg.html"));
            //httpFile.setNewURL(new URL("http://foto.radikal.ru/f.aspx?c06117c49feb2db08jpg"));
            httpFile.setNewURL(new URL("http://s60.radikal.ru/i168/1208/d6/9d8804d0225c.jpg"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final RadikalruShareServiceImpl service = new RadikalruShareServiceImpl();
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