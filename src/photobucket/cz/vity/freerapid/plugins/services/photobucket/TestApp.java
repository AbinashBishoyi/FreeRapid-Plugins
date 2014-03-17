package cz.vity.freerapid.plugins.services.photobucket;

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
            httpFile.setNewURL(new URL("http://s265.photobucket.com/albums/ii203/mm_vr/ntoskrnl/?action=view&current=logovq.jpg"));//image
            //httpFile.setNewURL(new URL("http://media.photobucket.com/video/water/HalusRen/NC_Trip1/Pictures207.flv"));//video
            //httpFile.setNewURL(new URL("http://gs174.photobucket.com/groups/w118/JX009ZP8UE/"));//album1
            //httpFile.setNewURL(new URL("http://s562.photobucket.com/albums/ss70/mango-star/Photography/"));//album2
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final PhotoBucketServiceImpl service = new PhotoBucketServiceImpl(); //instance of service - of our plugin
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