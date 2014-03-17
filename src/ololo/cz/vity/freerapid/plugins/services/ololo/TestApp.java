package cz.vity.freerapid.plugins.services.ololo;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author birchie
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
//            httpFile.setNewURL(new URL("http://ololo.fm/save/313035393237373530/3634353532383736/%D0%B1%D0%BE%D0%BD%D0%B8_%D0%BD%D0%B5%D0%BC_boney_nem_%D0%B4%D0%B5%D0%BD%D1%8C_%D0%BF%D0%BE%D0%B1%D0%B5%D0%B4%D1%8B"));
            httpFile.setNewURL(new URL("http://ololo.fm/save/3730313535353438/32323234373739/metallica_nothing_else_matter"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final OloloServiceImpl service = new OloloServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
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