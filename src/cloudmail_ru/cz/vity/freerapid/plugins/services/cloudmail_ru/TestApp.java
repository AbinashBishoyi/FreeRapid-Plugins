package cz.vity.freerapid.plugins.services.cloudmail_ru;

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
            //httpFile.setNewURL(new URL("https://cloud.mail.ru/public/0935c647b4e3/L-Mako-b5-bunny-stew-and-kool-aid.zip"));
            //httpFile.setNewURL(new URL("https://cloud.mail.ru/public/af00345913d7/.LenovoTheme.rar"));
            //httpFile.setNewURL(new URL("https://cloud.mail.ru/public/fd9a52a42e25/V%20serdze%20vesna.mp3"));
            //httpFile.setNewURL(new URL("https://cloud.mail.ru/public/da5bcccf73df/04")); //folder
            httpFile.setNewURL(new URL("https://cloud.mail.ru/public/da5bcccf73df/04/06.JPG"));
            //httpFile.setNewURL(new URL("https://cloclo20.datacloudmail.ru/weblink/get/da5bcccf73df/04/00.JPG"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final CloudMail_ruServiceImpl service = new CloudMail_ruServiceImpl();
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