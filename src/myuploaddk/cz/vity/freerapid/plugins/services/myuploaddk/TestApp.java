package cz.vity.freerapid.plugins.services.myuploaddk;

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
            //httpFile.setNewURL(new URL("http://www.myupload.dk/showfile/1bPQ4lWzR.pdf"));
            httpFile.setNewURL(new URL("http://www.myupload.dk/showfile/111Q4lWzR.pdf"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final MyUploadDkServiceImpl service = new MyUploadDkServiceImpl();
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