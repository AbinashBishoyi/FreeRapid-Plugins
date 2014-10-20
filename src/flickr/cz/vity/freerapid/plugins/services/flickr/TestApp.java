package cz.vity.freerapid.plugins.services.flickr;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Arthur
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/pgphotographs/4425994662/"));
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/sdasmarchives/8092199892/in/set-72157631786232754"));
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/sdasmarchives/sets/72157631786232754/")); //photosets
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/gsfc/galleries/72157628360143331/")); //galleries
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/gsfc/favorites/")); //favorites
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/gsfc/5958023159/in/set-72157629952998158")); //video
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/gsfc/sets/72157631454991752/")); //photosets (photo+video)
            //httpFile.setNewURL(new URL("http://www.flickr.com/photos/86616005@N08/sets/72157631451389242/"));
            //httpFile.setNewURL(new URL("https://www.flickr.com/photos/87928658@N02/sets/72157631675130544/"));
            //httpFile.setNewURL(new URL("https://www.flickr.com/photos/66122666@N05/6853902851/lightbox/"));
            httpFile.setNewURL(new URL("https://www.flickr.com/photos/gsfc/7951245596/in/set-72157631454991752"));
            //httpFile.setNewURL(new URL("https://www.flickr.com/photos/gsfc/7951245820/in/set-72157631454991752"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("195.77.62.138", 3128); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final FlickrServiceImpl service = new FlickrServiceImpl(); //instance of service - of our plugin
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