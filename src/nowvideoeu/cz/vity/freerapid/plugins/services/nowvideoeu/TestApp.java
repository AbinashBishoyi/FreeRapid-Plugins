package cz.vity.freerapid.plugins.services.nowvideoeu;

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
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www.nowvideo.eu/video/codcwyjytoli7"));
            //httpFile.setNewURL(new URL("http://www.nowvideo.eu/video/4dvc6rqpjt2px"));
            //httpFile.setNewURL(new URL("http://www.nowvideo.co/video/wyh83opcge6at"));
            //httpFile.setNewURL(new URL("http://www.nowvideo.eu/video/yeziziv4rzkmg"));
            //httpFile.setNewURL(new URL("http://www.nowvideo.eu/video/314c2008cbc5f"));
            httpFile.setNewURL(new URL("http://www.nowvideo.sx/player.php?v=2v7o4xk0w5yil"));
            //httpFile.setNewURL(new URL("http://embed.nowvideo.eu/embed.php?v=314c2008cbc5f"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final NowVideoEuServiceImpl service = new NowVideoEuServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}