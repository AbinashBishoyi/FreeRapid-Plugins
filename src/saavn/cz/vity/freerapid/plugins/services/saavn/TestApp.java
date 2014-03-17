package cz.vity.freerapid.plugins.services.saavn;

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
            //httpFile.setNewURL(new URL("http://www.saavn.com/s/#!/s/song/hindi/Commando+-+A+One+Man+Army/Saawan+Bairi/G1kIZTF2UQA"));
            //httpFile.setNewURL(new URL("http://www.saavn.com/s/#!/p/album/hindi/Commando+-+A+One+Man+Army-2013/toW1RtiMfQg_"));
            //httpFile.setNewURL(new URL("http://www.saavn.com/s/#!/s/album/hindi/Ek+Thi+Daayan-2013/q1hxqLqbrv4_"));
            //httpFile.setNewURL(new URL("http://www.saavn.com/s/#!/p/song/hindi/Ek+Thi+Daayan/Sapna+Re+Sapna/JkUHeiQFGnQ"));
            //httpFile.setNewURL(new URL("http://www.saavn.com/s/album/hindi/Sangam-1964/VWRNpIxHl-g_"));
            httpFile.setNewURL(new URL("http://www.saavn.com/p/song/hindi/Sangam/Bol-Radha-Bol/PxlaXR9pDwA"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final SaavnServiceImpl service = new SaavnServiceImpl();
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