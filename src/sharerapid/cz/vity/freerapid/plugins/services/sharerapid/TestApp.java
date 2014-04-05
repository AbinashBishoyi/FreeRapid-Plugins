package cz.vity.freerapid.plugins.services.sharerapid;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.utilities.LogUtils;
import org.jdesktop.application.Application;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Vity
 */
public class TestApp extends PluginDevApplication {
    private final static Logger logger = Logger.getLogger(TestApp.class.getName());

    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://rapids.cz/stahuj/1047774/windows-7-loader.rar"));
            //httpFile.setNewURL(new URL("http://www.share-rapid.cz/stahuj/4294279/kol-sv-90-2010.rar"));
            //httpFile.setNewURL(new URL("http://share-rapid.cz/stahuj/as081zu1"));
            httpFile.setNewURL(new URL("http://sharerapid.cz/soubor/35151/jenicek-a-marenka-lovci-carodejnic_avi"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final ShareRapidServiceImpl service = new ShareRapidServiceImpl(); //instance of service - of our plugin
            /*
            //we set premium account details
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("****");
            config.setPassword("****");
            service.setConfig(config);
            //*/
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            LogUtils.processException(logger, e); //writes error output - stack trace to console
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
