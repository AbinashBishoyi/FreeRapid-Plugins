package cz.vity.freerapid.plugins.services.dynaupload;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author birchie
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://bitbull.me/9gzqm8ki0d2o/Vixen_-_Room_For_More.wmv"));
            //httpFile.setNewURL(new URL("http://dynaupload.com/9gzqm8ki0d2o/Vixen_-_Room_For_More.wmv"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final DynaUploadServiceImpl service = new DynaUploadServiceImpl(); //instance of service - of our plugin

            //we set premium account details
            //final PremiumAccount config = new PremiumAccount();
            //config.setUsername("****");
            //config.setPassword("****");
            //service.setConfig(config);

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