package cz.vity.freerapid.plugins.services.rayfile;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for Rayfile.com
 *
 * @author Tommy Yang
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://ul.to/3uiloc6x/dlbb.wir.part1.rar"));
            httpFile.setNewURL(new URL("http://www.rayfile.com/en/files/00ce64f8-be16-11df-b2d0-0015c55db73d/dsadas/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8118); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final RayfileServiceImpl service = new RayfileServiceImpl(); //instance of service - of our plugin
//            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            testRun(service, httpFile, connectionSettings);
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
