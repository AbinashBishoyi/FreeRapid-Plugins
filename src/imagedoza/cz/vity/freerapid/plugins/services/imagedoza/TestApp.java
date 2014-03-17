package cz.vity.freerapid.plugins.services.imagedoza;

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
            //httpFile.setNewURL(new URL("http://imagedoza.com/i.cc/i/17ad8f5686696c38b073e5173d7991a2"));
            //httpFile.setNewURL(new URL("http://imagedoza.com/i.cc/i/6f74fc332e2e7a42c57a1a771dd00a22"));
            //httpFile.setNewURL(new URL("http://imagedoza.com/i.cc/i/1042dca48fa86fe314ed627e3520121d"));
            httpFile.setNewURL(new URL("http://imagedoza.com/i.cc/i/53a4ed5ca4a32bd5f7ea6cc915cc10dd"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final ImageDozaServiceImpl service = new ImageDozaServiceImpl();
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