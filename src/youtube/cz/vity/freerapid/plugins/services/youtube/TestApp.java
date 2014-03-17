package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Kajda
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=IJdOcdk_J1E"));//normal
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=DVFbA1AbEUw"));//rtmp
            //httpFile.setNewURL(new URL("http://www.youtube.com/user/HDstarcraft"));//user page
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=meCIER_s7Ng"));//transcript - subtitles
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=ZiH6CDl5kII")); //age verification
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=giHIJgJS2sE")); //age & controversy verification
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?list=UUv3nmgyia2M2FU2TAIRXSLw&feature=plcp")); //user uploaded video
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?list=PLE963AD215F0C4BE5")); //playlist
            //httpFile.setNewURL(new URL("http://www.youtube.com/playlist?list=FL2pmfLm7iq6Ov1UwYrWYkZA"));// favorite list
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=Phl57XmsPQ8"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=PM4kLnRr0ZI"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=Wlrys8HvBSc"));  //possible bug in quality
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECA89DCFA6ADACE599")); //course list
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECB24BC7956EE040CD"));
            httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECD9DDFBDC338226CA"));
            //httpFile.setNewURL(new URL("http://www.youtube.com/course?list=ECBD4C7FD29B0C6D0C")); //course list
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8118); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final YouTubeServiceImpl service = new YouTubeServiceImpl(); //instance of service - of our plugin

            YouTubeSettingsConfig config = new YouTubeSettingsConfig();
            config.setQualitySetting(4);
            config.setContainer(0);
            config.setReversePlaylistOrder(false);
            config.setDownloadSubtitles(false);
            service.setConfig(config);

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