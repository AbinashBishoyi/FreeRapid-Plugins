package cz.vity.freerapid.plugins.services.subory;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Kajda
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {

        final HttpFile httpFile = getHttpFile();
        try {

           httpFile.setNewURL(new URL("http://www.subory.sk/download/257989/Cesky.test.txt"));
            testRun(new SuboryServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}