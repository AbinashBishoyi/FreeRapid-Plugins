package cz.vity.freerapid.plugins.services.ebooksshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Abinash Bishoyi
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://www.ebooks-share.net/redirect/b101511f110724e826baba914da8b1f7"));
            //httpFile.setNewURL(new URL("http://www.ebooks-share.net/the-thyroid-diet-revolution-manage-your-master-gland-of-metabolism-for-lasting-weight-loss/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            connectionSettings.setProxy("127.0.0.1", 8080); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final EbooksshareServiceImpl service = new EbooksshareServiceImpl(); //instance of service - of our plugin
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