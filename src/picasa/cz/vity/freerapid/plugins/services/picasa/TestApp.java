package cz.vity.freerapid.plugins.services.picasa;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Vity
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("https://picasaweb.google.com/CZVity/VysokyVrch#"));
            //httpFile.setNewURL(new URL("http://lh4.ggpht.com/-qeRx1x4w8zQ/SmzBG1nE25I/AAAAAAAABDY/wEsq5KmW4ZA/d/P7260064.jpg"));
            httpFile.setNewURL(new URL("http://lh3.ggpht.com/-gzpLFOl0YeA/SmzAh40nqJI/AAAAAAAABC4/Hmf0y61AI_Y/d/Vysok%2525C3%2525BD%252520vrch.jpg"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final PicasaServiceImpl service = new PicasaServiceImpl(); //instance of service - of our plugin
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