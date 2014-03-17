package cz.vity.freerapid.plugins.services.yandexdisk;

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
            //httpFile.setNewURL(new URL("http://yadi.sk/d/69CDK9xBU5qz"));
            //httpFile.setNewURL(new URL("https://disk.yandex.net/disk/public/?hash=cUQgAki%2BkmwkKht8da/UkEusqRyPZivjQcvjD%2BlUrsM%3D&final=true"));
            //httpFile.setNewURL(new URL("http://yadi.sk/d/VZXQ4AyUYa50&post=12517322_4796"));
            httpFile.setNewURL(new URL("http://yadi.sk/d/IuILKPwmclGU"));
            //httpFile.setNewURL(new URL("http://yadi.sk/d/W9-9gP2KYhAm"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final YandexDiskServiceImpl service = new YandexDiskServiceImpl();
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