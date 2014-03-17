package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for indowebster.com
 *
 * @author zid
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://v5.indowebster.com//marumo_no_okite_ep01_704x396x264_mp4_005_.html"));
            //httpFile.setNewURL(new URL("http://indowebster.com/ogon_no_buta_ep03__704x396_xvid_.avi.001_.html"));
            httpFile.setNewURL(new URL("http://www.indowebster.com/download/video/godofstudye08hdtvx264450pdokgodieavi001"));
            //httpFile.setNewURL(new URL("http://www.indowebster.com/download/video/ogon_no_buta_ep02__704x396_xvid_.avi.001"));

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final IndowebsterServiceImpl service = new IndowebsterServiceImpl(); //instance of service - of our plugin
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}
