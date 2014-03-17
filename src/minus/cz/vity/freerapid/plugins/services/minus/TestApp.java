package cz.vity.freerapid.plugins.services.minus;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Tommy
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
//            httpFile.setNewURL(new URL("http://minus.com/m3NkzQYFn"));       // file list of 1 file
//            httpFile.setNewURL(new URL("http://minus.com/mbzWKzrOt/"));      // file list of many files
            httpFile.setNewURL(new URL("http://minus.com/lyGHOEFKYlVVa"));
//            httpFile.setNewURL(new URL("http://min.us/mIIOD4rnI/1"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("127.0.0.1", 8098); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MinusServiceImpl service = new MinusServiceImpl(); //instance of service - of our plugin
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