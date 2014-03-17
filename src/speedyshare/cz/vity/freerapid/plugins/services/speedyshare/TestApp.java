package cz.vity.freerapid.plugins.services.speedyshare;

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
            //httpFile.setNewURL(new URL("http://www.speedyshare.com/files/20762507/logovq.zip"));//single
            //httpFile.setNewURL(new URL("http://www.speedyshare.com/712851541.html"));//multiple
            //httpFile.setNewURL(new URL("http://www.speedyshare.com/w4EYf/DPnF-Phi-Brain-2-03-animelv.net.mp4"));
            //httpFile.setNewURL(new URL("http://www.speedyshare.com/files/19332630/the_fish_that_saved_Pittsburgh.rar"));
            //httpFile.setNewURL(new URL("http://speedy.sh/yBVJx/HimitSubs-Haiyore-Nyaruko-san-03-animelv.net.mp4"));
            httpFile.setNewURL(new URL("http://www.speedyshare.com/file/tfV4v/SolSuite-2012-12.50-Setup.rar"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final SpeedyShareServiceImpl service = new SpeedyShareServiceImpl(); //instance of service - of our plugin
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