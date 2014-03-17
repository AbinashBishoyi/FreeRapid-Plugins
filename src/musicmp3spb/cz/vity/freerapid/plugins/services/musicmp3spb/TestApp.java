package cz.vity.freerapid.plugins.services.musicmp3spb;

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
            httpFile.setNewURL(new URL("http://musicmp3spb.org/artist/steelhouse_lane.html"));
            //httpFile.setNewURL(new URL("http://musicmp3spb.org/allsongs/steelhouse_lane.html"));
            //httpFile.setNewURL(new URL("http://musicmp3spb.org/album/slaves_of_the_new_world.html"));
            //httpFile.setNewURL(new URL("http://musicmp3spb.org/covers/slaves_of_the_new_world.html"));
            //httpFile.setNewURL(new URL("http://musicmp3spb.org/song/slaves_of_the_new_world_turnaround.html"));
            //httpFile.setNewURL(new URL("http://musicmp3spb.org/download/slaves_of_the_new_world_turnaround/play/3e39078b4a9bb63519d3a92f1c6ce1cc1369106406"));
            //httpFile.setNewURL(new URL("http://musicmp3spb.org/download/slaves_of_the_new_world_turnaround/3e39078b4a9bb63519d3a92f1c6ce1cc1369106406"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MusicMp3SpbServiceImpl service = new MusicMp3SpbServiceImpl(); //instance of service - of our plugin
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