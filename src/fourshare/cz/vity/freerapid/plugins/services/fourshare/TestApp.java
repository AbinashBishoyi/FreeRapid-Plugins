package cz.vity.freerapid.plugins.services.fourshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://up.4share.vn/f/3405030602040407/Dragon%20Ball%20Kai%20-%20Ep%2001.avi.file"));
            //httpFile.setNewURL(new URL("http://up.4share.vn/f/3b090a0c080f0a0f/Xilisoft%20Video%20Converter%20Ultimate%207.0.1build%201219.rar"));
            //httpFile.setNewURL(new URL("http://up.4share.vn/d/6b5a5c595e595a5d"));
            httpFile.setNewURL(new URL("http://up.4share.vn/d/5a6b6d686c696a69"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final FourShareServiceImpl service = new FourShareServiceImpl(); //instance of service - of our plugin
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