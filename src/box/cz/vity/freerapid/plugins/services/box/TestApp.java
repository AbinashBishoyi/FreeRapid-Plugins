package cz.vity.freerapid.plugins.services.box;

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
            //httpFile.setNewURL(new URL("https://www.box.com/s/l9rzpwfr4q2fz9nix5lr"));
            httpFile.setNewURL(new URL("https://app.box.com/s/vprlmeaz6qfkyhfvdfcn"));
            //httpFile.setNewURL(new URL("https://app.box.com/shared/static/zm151bt7fgq3kdpncnvo.pdf"));  //direct
            //httpFile.setNewURL(new URL("https://app.box.com/s/g05fqm0chp3r9s8hasby"));  //folder
            //httpFile.setNewURL(new URL("https://app.box.com/s/g05fqm0chp3r9s8hasby/1/1978549231/17246128159/1"));  //file from folder
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final BoxServiceImpl service = new BoxServiceImpl(); //instance of service - of our plugin
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