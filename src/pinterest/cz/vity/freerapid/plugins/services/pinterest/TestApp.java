package cz.vity.freerapid.plugins.services.pinterest;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author bircie
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.pinterest.com/pin/275423333434060623/"));  // pin
            //httpFile.setNewURL(new URL("http://www.pinterest.com/carlrice/inspiration/"));    // board  <25
            //httpFile.setNewURL(new URL("http://www.pinterest.com/freetattoo/lion-tattoo/"));  // board  =25
            //httpFile.setNewURL(new URL("http://www.pinterest.com/carlrice/old/"));            // board  >25
            //httpFile.setNewURL(new URL("http://www.pinterest.com/carlrice/bikes/"));          // board  >>25
            httpFile.setNewURL(new URL("http://www.pinterest.com/rmkmj2/i-lift-things-up-and-put-them-down/"));  // board >>>>>25
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final PinterestServiceImpl service = new PinterestServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
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