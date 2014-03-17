package cz.vity.freerapid.plugins.services.videahu;

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
            //httpFile.setNewURL(new URL("http://videa.hu/videok/sport/lyon-vs-valenciennes-32-highlights-uJolYOJX7Thzv2Tc"));
            //httpFile.setNewURL(new URL("http://videa.hu/videok/film-animacio/kerekes-vica-szexjelenet-szex-IUBrRP123MMDaS4J"));
            //httpFile.setNewURL(new URL("http://videa.hu/videok/hirek-politika/swwhu30mo-8THAFaGK82YoZqzh"));
            //httpFile.setNewURL(new URL("http://videa.hu/videok/vicces/somlo-letapizta-malekot-ywTZQrhkGnq4Yetm"));
            //httpFile.setNewURL(new URL("http://videa.hu/videok/tudomany-technika/filmet-vesz-fel-a-magyar-digitalis-kamera-helikopter-s79d9SHxMeSmJC2Q"));
            httpFile.setNewURL(new URL("http://videa.hu/videok/emberek-vlogok/orosz-moka-nzrQ49rsxcH4N8fG"));

            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final VideaHuServiceImpl service = new VideaHuServiceImpl();
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