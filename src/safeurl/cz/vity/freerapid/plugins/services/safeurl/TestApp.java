package cz.vity.freerapid.plugins.services.safeurl;

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
            httpFile.setNewURL(new URL("http://safeurl.me/d/v9m0qxp3"));     //direct
            //httpFile.setNewURL(new URL("http://safeurl.me/p/5pcjmos9"));     //protected
            //httpFile.setNewURL(new URL("http://safeurl.me/p/wubdpvkh"));     //  +recaptcha
            //httpFile.setNewURL(new URL("http://safeurl.me/p/s729ie0f"));     //  +solvemedia
            //httpFile.setNewURL(new URL("http://safeurl.me/p/abvomsk3"));     //  +fancycaptcha
            //httpFile.setNewURL(new URL("http://safeurl.me/p/dxew4b76"));     //  +password:freerapid
            //httpFile.setNewURL(new URL("http://safeurl.me/p/mlwjfv0n"));     //  +recaptcha  +password:freerapid
            //httpFile.setNewURL(new URL("http://safeurl.me/p/u93yrwx7"));     // list of links
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final SafeUrlServiceImpl service = new SafeUrlServiceImpl(); //instance of service - of our plugin
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