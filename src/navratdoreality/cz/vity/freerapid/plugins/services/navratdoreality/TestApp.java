package cz.vity.freerapid.plugins.services.navratdoreality;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author iki
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/?p=view&id=13972")); // regular video
            httpFile.setNewURL(new URL("http://www.navratdoreality.cz/?p=view&id=13966")); // video with html entities (spaces etc)
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/index.php?p=view&id=13977")); // zip file
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/content/external/unterwassermann/video/2012/07/nuz.mp4")); //direct link
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/content/external/unterwassermann/hnus/2012/07/fekalnik/1.jpg")); //direct link 2
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/index.php?p=view&id=13978")); // youtube video
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/?p=view&id=13987")); // standalone pictures
            //httpFile.setNewURL(new URL("http://www.navratdoreality.cz/index.php?p=view&id=13960")); // xhamster external link

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final NavratDoRealityServiceImpl service = new NavratDoRealityServiceImpl(); //instance of service - of our plugin
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