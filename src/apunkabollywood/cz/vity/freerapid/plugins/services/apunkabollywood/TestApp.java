package cz.vity.freerapid.plugins.services.apunkabollywood;

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
            //httpFile.setNewURL(new URL("http://www.apunkabollywood.net/browser/category/view/7532/ek-tha-tiger-(2012)"));
            httpFile.setNewURL(new URL("http://www.apunkabollywood.us/browser/download/get/71512/01%20-%20Mashallah%20-%20Wajid%20&%20Shreya%20Ghoshal%20(ApunKaBollywood.com).html"));
            //httpFile.setNewURL(new URL("http://www.apunkabollywood.us/browser/category/view/1476/arth-(1982)"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final ApunKaBollywoodServiceImpl service = new ApunKaBollywoodServiceImpl();
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