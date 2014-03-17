package cz.vity.freerapid.plugins.services.kupload;

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
            httpFile.setNewURL(new URL("http://kupload.org/zwl1zkw8f6u6.html"));
            //httpFile.setNewURL(new URL("http://kupload.org/qeyf52zziac6.html"));
            //httpFile.setNewURL(new URL("http://kupload.org/p3wfchwz3c2b.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("202.137.22.183", 8080); //eg we can use local proxy to sniff HTTP communication
            final KUploadServiceImpl service = new KUploadServiceImpl();
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