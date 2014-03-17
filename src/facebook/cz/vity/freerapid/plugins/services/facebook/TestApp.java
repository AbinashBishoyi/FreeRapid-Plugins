package cz.vity.freerapid.plugins.services.facebook;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
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
            //httpFile.setNewURL(new URL("http://www.facebook.com/video/video.php?v=10150223186041807"));// video, no login
            //httpFile.setNewURL(new URL("http://www.facebook.com/photo.php?v=135683543216195&set=vb.40796308305&type=2&theater")); //video, no login
            //httpFile.setNewURL(new URL("http://www.facebook.com/photo.php?fbid=10150924761158306&set=a.10150924761143306.439166.40796308305&type=3")); //pic, no login
            //httpFile.setNewURL(new URL("http://www.facebook.com/media/set/?set=a.10150924761143306.439166.40796308305&type=3")); //album, no login
            //httpFile.setNewURL(new URL("http://www.facebook.com/media/set/?set=vb.141884481557&type=2")); //album, no login
            //httpFile.setNewURL(new URL("http://www.facebook.com/PeterJacksonNZ/videos")); //album
            httpFile.setNewURL(new URL("http://www.facebook.com/video/?id=7677942180")); //album
            //httpFile.setNewURL(new URL("http://www.facebook.com/video/video.php?v=1603402488187"));//requires login
            //httpFile.setNewURL(new URL("http://www.facebook.com/photo.php?fbid=1906729784134&set=a.1892908198603.2097158.1118473909&type=3")); //requires login
            httpFile.setNewURL(new URL("http://www.facebook.com/media/set/?set=a.1892908198603.2097158.1118473909&type=3")); //requires login
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final FaceBookServiceImpl service = new FaceBookServiceImpl(); //instance of service - of our plugin
            /*
            PremiumAccount config = new PremiumAccount();
            config.setUsername("***");
            config.setPassword("***");
            service.setConfig(config);
            */
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