package cz.vity.freerapid.plugins.services.dailymotion;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author JPEXS
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/en/featured/channel/travel/video/x9y8kg_san-vigilio-di-marebbe-2009_travel"));
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/video/xu9bls_key-moments-to-remember-from-the-spring-summer-2013-shows-novoice_news"));
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/playlist/xy4h8_jeje2255_cinema-2000-vol-3")); //playlist
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/group/makeitSICK#video=xa2cdj")); //group
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/group/makeitSICK/1"));
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/video/xkzttp_test-subtitle-in-dailymotion_shortfilms")); //video with subtitle
            //httpFile.setNewURL(new URL("http://static2.dmcdn.net/static/video/339/362/35263933:subtitle_en.srt/test%20subtitle%20in%20dailymotion?en")); //subtitle
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/video/xuihv3_joli-but-acrobatique-au-mexique_sport"));
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/video/xupie7_zapping-tele-du-30"));
            httpFile.setNewURL(new URL("http://www.dailymotion.com/video/x11gd1n_edson-montano-marque-un-but-d-anthologie_news"));
            //httpFile.setNewURL(new URL("http://www.dailymotion.com/video/x11hdie_le-lob-magnifique-de-neymar-face-a-messi_news"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("23.29.113.100", 3128);
            final DailymotionServiceImpl service = new DailymotionServiceImpl();
            DailymotionSettingsConfig config = new DailymotionSettingsConfig();
            config.setQualitySetting(2);
            config.setSubtitleDownload(false);
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
