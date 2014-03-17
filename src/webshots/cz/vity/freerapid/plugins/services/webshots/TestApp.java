package cz.vity.freerapid.plugins.services.webshots;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://entertainment.webshots.com/photo/2391989120075568609cEJdlf"));//single photo
            //httpFile.setNewURL(new URL("http://home-and-garden.webshots.com/photo/2386045520040618918MDkECi"));//single photo with no name
            //httpFile.setNewURL(new URL("http://outdoors.webshots.com/video/3043292790000305411MQfqEl"));//video
            //httpFile.setNewURL(new URL("http://rides.webshots.com/album/574558879SovaQH"));//album
            //httpFile.setNewURL(new URL("http://community.webshots.com/user/obelix_studio"));//user
            //httpFile.setNewURL(new URL("http://community.webshots.com/slideshow/575465878TZfRwO"));//slideshow

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final WebshotsServiceImpl service = new WebshotsServiceImpl(); //instance of service - of our plugin
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