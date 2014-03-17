package cz.vity.freerapid.plugins.services.ctdisk;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for ctdisk.com
 *
 * @author Tommy Yang
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.t00y.com/file/8714306"));
            //httpFile.setNewURL(new URL("http://www.ctdisk.com/file/8256005"));
            //httpFile.setNewURL(new URL("http://www.400gb.com/file/17134346"));
            //httpFile.setNewURL(new URL("http://www.bego.cc/file/17291673"));
            httpFile.setNewURL(new URL("http://www.pipipan.com/file/17397545"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
//            connectionSettings.setProxy("127.0.0.1", 8099); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final CtdiskShareServiceImpl service = new CtdiskShareServiceImpl(); //instance of service - of our plugin
//            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            testRun(service, httpFile, connectionSettings);
            //all output goes to the console
//            httpFile.setNewURL(new URL("http://www.ctdisk.com/file/2797200"));
//            testRun(service, httpFile, connectionSettings);
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
