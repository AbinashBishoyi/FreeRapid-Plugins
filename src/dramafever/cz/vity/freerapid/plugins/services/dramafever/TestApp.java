package cz.vity.freerapid.plugins.services.dramafever;

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
            //httpFile.setNewURL(new URL("http://www.dramafever.com/drama/728/1/Personal_Taste/"));
            //httpFile.setNewURL(new URL("http://imgdf-a.akamaihd.net/st/sub/PersonalTaste_01_new.srt")); //subtitle
            //httpFile.setNewURL(new URL("http://www.dramafever.com/drama/19/1/Jewel_in_the_Palace/")); //Hulu
            //httpFile.setNewURL(new URL("http://www.dramafever.com/drama/3915/1/The_Relation_of_Face%2C_Mind_and_Love/"));
            httpFile.setNewURL(new URL("http://www.dramafever.com/es/drama/4451/5/Star_in_My_Heart_-_Doblado_al_Espa%C3%B1ol/"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 9060, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            final DramaFeverServiceImpl service = new DramaFeverServiceImpl();
            SettingsConfig config = new SettingsConfig();
            config.setVideoQuality(VideoQuality._1000);
            //config.setDownloadSubtitle(false);
            service.setConfig(config);
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