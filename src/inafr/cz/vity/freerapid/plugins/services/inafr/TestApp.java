package cz.vity.freerapid.plugins.services.inafr;

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
            //httpFile.setNewURL(new URL("http://www.ina.fr/sport/football/video/CAA7600524501/glasgow-la-folle-nuit.fr.html"));
            //httpFile.setNewURL(new URL("http://www.ina.fr/economie-et-societe/vie-sociale/video/CAB93031104/la-fete-a-marseille.fr.html"));
            //httpFile.setNewURL(new URL("http://www.ina.fr/ardisson/les-integrales/video/CPB94001383/long-courrier-une-nuit-a-rio.fr.html"));
            httpFile.setNewURL(new URL("http://www.ina.fr/video/RCC8905214122"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final InaFrServiceImpl service = new InaFrServiceImpl();
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