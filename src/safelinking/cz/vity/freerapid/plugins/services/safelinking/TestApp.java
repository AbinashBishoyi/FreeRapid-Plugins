package cz.vity.freerapid.plugins.services.safelinking;

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
            //  httpFile.setNewURL(new URL("https://safelinking.net/d/58f0591008"));  //direct link                                 :)
            //  httpFile.setNewURL(new URL("https://safelinking.net/p/657d8f1754"));  //protected                                   :)
            //  httpFile.setNewURL(new URL("https://safelinking.net/p/0823b407fe"));  //protected with captcha                      :)
            //  httpFile.setNewURL(new URL("https://safelinking.net/p/e1d27210f7"));  //protected with password='test'              :)
            //  httpFile.setNewURL(new URL("https://safelinking.net/p/342260e9a9"));  //protected with captcha and password='test'  :)
              httpFile.setNewURL(new URL("https://safelinking.net/p/a466480f22"));  // list of direct links with captcha          :)
            //httpFile.setNewURL(new URL("http://safelinking.net/p/2dd0649362"));   // list of live links with captcha            :)
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final SafeLinkingServiceImpl service = new SafeLinkingServiceImpl(); //instance of service - of our plugin
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