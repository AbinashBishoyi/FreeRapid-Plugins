package cz.vity.freerapid.plugins.services.ceskatelevize;

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
            httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1143638030-ct-live/20754215404-ct-live-vlasta-redl/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1095875447-cestomanie/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10588743864-denik-dity-p/213562260300003-piknik/video/281044"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/specialy/hydepark-civilizace/14.9.2013/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10084897100-kluci-v-akci/211562221900012/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10084897100-kluci-v-akci/211562221900012/obsah/155251-pastiera-napoletana/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1126672097-otazky-vaclava-moravce/213411030510609-otazky-vaclava-moravce-2-cast/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1126672097-otazky-vaclava-moravce/213411030510609-otazky-vaclava-moravce-2-cast/obsah/265416-pokracovani-debaty-z-1-hodiny-poradu/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1104873554-nadmerne-malickosti/video/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1183909575-tyden-v-regionech-ostrava/413231100212014-tyden-v-regionech/obsah/252368-majiteli-reznictvi-v-centru-ostravy-hrozi-az-milionova-pokuta/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10306517828-mala-farma/313292320310028/video/"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("195.122.213.61", 3128); //eg we can use local proxy to sniff HTTP communication
            final CeskaTelevizeServiceImpl service = new CeskaTelevizeServiceImpl();
            CeskaTelevizeSettingsConfig config = new CeskaTelevizeSettingsConfig();
            config.setVideoQuality(VideoQuality._404);
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