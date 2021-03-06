package cz.vity.freerapid.plugins.services.googledocs;

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
            //httpFile.setNewURL(new URL("https://drive.google.com/file/d/0B-UHA1n9QxiwYUVORmRPaXRsOHc/edit?usp=sharing&confirm=no_antivirus"));
            httpFile.setNewURL(new URL("https://docs.google.com/file/d/0Bwp_ES8pPMZBMUViNTRjMG5tbEU/edit"));
            //httpFile.setNewURL(new URL("https://drive.google.com/file/d/0B18_0-CUw0vISFBLbWY5bDd3akk/edit?usp=sharing")); // small file, direct
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final GoogleDocsServiceImpl service = new GoogleDocsServiceImpl();
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