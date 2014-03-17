package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Alex
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://u31019701.letitbit.net/download/89576.8bb6946fc3f7dc97eb59480c4ff2/80_HQ_Images_Charisma.rar.html"));
            //httpFile.setNewURL(new URL("http://letitbit.net/download/72807.70d2045e51ca61a150ee158a4bfb/Super_Popki_Wife_Stockings.rar.html"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("212.119.97.198", 3128);//remember to test with this Russian proxy too
            //then we tries to download
            final LetitbitShareServiceImpl service = new LetitbitShareServiceImpl(); //instance of service - of our plugin
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