package cz.vity.freerapid.plugins.services.kewegofr;

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
            //httpFile.setNewURL(new URL("http://www.kewego.fr/video/66a6020ac4cs.html"));
            //httpFile.setNewURL(new URL("http://www.kewego.fr/video/3c07e83c05cs.html"));
            //httpFile.setNewURL(new URL("http://www.kewego.fr/video/aab6739137fs.html"));
            httpFile.setNewURL(new URL("http://www.kewego.fr/video/0023848d413s.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final KewegoFrServiceImpl service = new KewegoFrServiceImpl();
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