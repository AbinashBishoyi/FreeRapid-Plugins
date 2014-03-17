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
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10084897100-kluci-v-akci/211562221900012/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/loh/videoarchiv/sporty/serm/189028-protest-korejske-sermirky-sin-a-lam/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1143638030-ct-live/20754215404-ct-live-vlasta-redl/video/"));
            httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1126672097-otazky-vaclava-moravce/213411030510609-otazky-vaclava-moravce-2-cast/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10172720715-pripad-z-poodle-springs/20838145838/")); //error
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