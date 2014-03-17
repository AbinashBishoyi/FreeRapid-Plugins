package cz.vity.freerapid.plugins.services.divshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Vity, ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://www.divshare.com/download/16236750-bec"));//regular file
            //httpFile.setNewURL(new URL("http://www.divshare.com/download/9127502-5bc"));//image
            //httpFile.setNewURL(new URL("http://www.divshare.com/download/2280157-d49"));//video
            //httpFile.setNewURL(new URL("http://www.divshare.com/download/9637954-b5f"));//audio
            //httpFile.setNewURL(new URL("http://www.divshare.com/folder/572568-fdf"));//folder
            //httpFile.setNewURL(new URL("http://www.divshare.com/playlist/596628-66a"));//playlist
            //httpFile.setNewURL(new URL("http://www.divshare.com/gallery/625909-90a"));//gallery

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final DivshareServiceImpl service = new DivshareServiceImpl(); //instance of service - of our plugin
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