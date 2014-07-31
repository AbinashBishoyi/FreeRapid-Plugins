package cz.vity.freerapid.plugins.services.adf;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://adf.ly/5XR"));
            //httpFile.setNewURL(new URL("http://adf.ly/DA73i"));
//            httpFile.setNewURL(new URL("http://adf.ly/1409722/http://www.wowebook.be/download/4314/"));
            //httpFile.setNewURL(new URL("http://adf.ly/PPqGu"));  //http://www.putlocker.com/file/A2A1F1C572C8C542
            httpFile.setNewURL(new URL("http://adf.ly/Rp4za"));
            //httpFile.setNewURL(new URL("http://adf.acb.im/iO"));
            //httpFile.setNewURL(new URL("http://goo.gl/efMsK"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final AdfServiceImpl service = new AdfServiceImpl(); //instance of service - of our plugin
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
