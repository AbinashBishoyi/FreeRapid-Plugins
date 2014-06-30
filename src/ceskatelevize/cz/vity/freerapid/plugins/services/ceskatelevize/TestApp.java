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
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1143638030-ct-live/20754215404-ct-live-vlasta-redl/video/"));
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
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10361564316-sanitka-2/210512120330009/"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/mazalove"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/player?width=560&IDEC=211+513+13003%2F0010&fname=Mazalov%C3%A9+-+Je%C5%BE%C3%AD%C5%A1ek%3F+Existuje%21"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/bludiste"));
            //httpFile.setNewURL(new URL("http://decko.ceskatelevize.cz/player?width=560&IDEC=409+234+10001%2F1007&fname=Bludi%C5%A1t%C4%9B+-+19.+2.+2009"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10213448544-evropska-liga-ve-fotbalu/214471291124205-fc-viktoria-plzen-olympique-lyon")); //multiparts
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10213448544-evropska-liga-ve-fotbalu/214471291124205-fc-viktoria-plzen-olympique-lyon/?switchitemid=2-214+471+29112%2F4205"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10213448544-evropska-liga-ve-fotbalu/214471291124205-fc-viktoria-plzen-olympique-lyon/?switchitemid=2-214+471+29112%2F4205&fname=Evropsk%C3%A1+liga+ve+fotbalu+-+FC+Viktoria+Plze%C5%88+-+Olympique+Lyon-3"));
            httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/10639901181-trabantem-jizni-amerikou/213562260150012/bonus/16881"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("213.192.26.6", 8080); //eg we can use local proxy to sniff HTTP communication
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