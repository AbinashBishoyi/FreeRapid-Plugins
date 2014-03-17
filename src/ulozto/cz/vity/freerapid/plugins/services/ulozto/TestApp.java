package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.io.IOException;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek & Tomáš Procházka <to.m.p@atomsoft.cz>
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {

        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://www.uloz.to/x52oVtE/rihanna-feat-tonic-we-found-love-dj-elon-matana-mash-up-mp3"));
            //httpFile.setNewURL(new URL("http://www.ulozto.cz/xtSTrvJ/mother-daughter-exchange-club-19-dvdrip-2011-cd2-avi"));
            //httpFile.setNewURL(new URL("http://uloz.to/xf5ZyF7/london-2012-rar"));
            httpFile.setNewURL(new URL("http://www.ulozto.cz/xda1xMy/penthouse-sex-obsessed-xxx-dvdrip-xvid-qualitx-avi"));
            //httpFile.setNewURL(new URL("http://uloz.to/xda1xMy/penthouse-sex-obsessed-xxx-dvdrip-xvid-qualitx-avi"));
            //httpFile.setNewURL(new URL("http://www.ulozto.cz/x1AyWgwJ/kriminalka-stare-mesto-ii-1-dil-12-9-2013-dudiss-avi"));
            //httpFile.setNewURL(new URL("http://uloz.to/xrgawztg/sample-txt"));           //password: "1234"
            //httpFile.setNewURL(new URL("http://www.ulozto.cz/xFU8sN2G/test-docx")); //password : "password"
            //httpFile.setNewURL(new URL("http://www.ulozto.cz/xE6PNASA/hercule-poirot-smrt-v-oblacich-ts"));
            final ConnectionSettings settings = new ConnectionSettings();
            //settings.setProxy("localhost", 8081);
            testRun(new UlozToServiceImpl(), httpFile, settings);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) throws IOException {
        Handler fh = new FileHandler("./TestApp.xml");
        Logger.getLogger("").addHandler(fh);
        Application.launch(TestApp.class, args);
    }
}
