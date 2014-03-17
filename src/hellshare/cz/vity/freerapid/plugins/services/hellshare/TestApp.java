package cz.vity.freerapid.plugins.services.hellshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek & Tom� Proch�zka <to.m.p@atomsoft.cz>
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {

        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://download.hellshare.com/13unci-part2-rar/7980859/"));
            //httpFile.setNewURL(new URL("http://download.hellshare.cz/connie-my-beautiful-wife-mov/8101434"));
            //httpFile.setNewURL(new URL("http://download.hellshare.pl/richard-burns-rally/richard-burns-rally-pl-zip/6276313/"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.setProxy("77.48.30.223", 8080); //Czech proxy is a must.
            //connectionSettings.setProxy("89.187.142.176", 3128); //Czech proxy is a must.
            final HellshareServiceImpl service = new HellshareServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);

        /*try {
            ImageIO.write(ImageIO.read(new File("E:\\projects\\captchatest\\letters1.png")), "png", new File("E:\\projects\\captchatest\\letters.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}