package cz.vity.freerapid.plugins.services.hulu;

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
        final HttpFile httpFile = getHttpFile();
        try {
            //log everything
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.hulu.com/profiles/user/public_queue"));//user queue
            //httpFile.setNewURL(new URL("http://www.hulu.com/watch/137629#s-p1-so-i0"));
            //httpFile.setNewURL(new URL("http://www.hulu.com/watch/245224")); // non episode
            //httpFile.setNewURL(new URL("http://www.hulu.com/watch/280815")); //has subtitle
            //httpFile.setNewURL(new URL("http://www.hulu.com/captions.xml?content_id=40039219/Jewel in the Palace - S01E01 - Episode 1")); //subtitle
            httpFile.setNewURL(new URL("http://www.hulu.com/watch/422886"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8118); //eg we can use local proxy to sniff HTTP communication
            final HuluServiceImpl service = new HuluServiceImpl();

            final HuluSettingsConfig config = new HuluSettingsConfig();
            //config.setUsername("***");
            //config.setPassword("***");
            config.setQualityHeightIndex(10); //highest
            config.setVideoFormatIndex(1); //h264
            service.setConfig(config);

            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
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