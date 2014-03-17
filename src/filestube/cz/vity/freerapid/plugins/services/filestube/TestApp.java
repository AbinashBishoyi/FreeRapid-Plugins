package cz.vity.freerapid.plugins.services.filestube;

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
            //httpFile.setNewURL(new URL("http://www.filestube.com/4shared/Haxb3Tm5reLR6I6V3Sz0B/test.html"));
            //httpFile.setNewURL(new URL("http://www.filestube.com/turbobit/9W8J1yF3SivqO4tk4pMQvQ/www-NewAlbumReleases-net-Chromatics-Kill-For-Love-Drumless-2012.html"));
            //httpFile.setNewURL(new URL("http://www.filestube.com/netload/dog6tuR5KFqGwh9b5SDklj/Star-Trek-TNG-S01E10-UNRATED-720p-BluRay-x264-INQUISITION-1500.html"));
            httpFile.setNewURL(new URL("http://www.filestube.to/700WKTuWrFeQiAOOssSOod"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final FilesTubeServiceImpl service = new FilesTubeServiceImpl(); //instance of service - of our plugin
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