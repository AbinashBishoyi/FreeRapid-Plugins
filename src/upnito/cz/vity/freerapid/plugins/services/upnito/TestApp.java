package cz.vity.freerapid.plugins.services.upnito;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.upnito.sk/download.php?dwToken=8ab899bc3c0ddcbb7d8a4620a9a13b2e"));
            //httpFile.setNewURL(new URL("http://www.upnito.sk/download.php?dwToken=3fcc2d384254fb058e514834a6160c0b"));
            httpFile.setNewURL(new URL("http://www.upnito.sk/subor/8cf2ee9d80ab993d83fdf7f799b1e0de.html"));
            //musi fungovat take
            //   httpFile.setNewURL(new URL("http://www.upnito.sk/subor/3fcc2d384254fb058e514834a6160c0b.html"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final UpnitoShareServiceImpl service = new UpnitoShareServiceImpl(); //instance of service - of our plugin
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}
