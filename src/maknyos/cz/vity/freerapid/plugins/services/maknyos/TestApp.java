package cz.vity.freerapid.plugins.services.maknyos;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for maknyos.com
 *
 * @author zid
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.maknyos.com/weye04z6iran/Pics_Print_3.22.1.423.by_msmobo-maknyos.com.rar.html"));
            httpFile.setNewURL(new URL("http://www.maknyos.com/n6odko4e5bdl/A_Perfect_World_1993_hdtv_720p_x264-CHD.EN-maknyos.com.mkv.html"));
            //httpFile.setNewURL(new URL("http://www.maknyos.com/6eam3biispzx/indofiles.brscrw.2011.duasatu.com.mkv-maknyos.com.001.html"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MaknyosServiceImpl service = new MaknyosServiceImpl(); //instance of service - of our plugin
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
