package cz.vity.freerapid.plugins.services.filesonic_premium;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author valankar
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            httpFile.setNewURL(new URL("http://www.filesonic.com/file/1557504161/Richard.Hammonds.Journey.To.The.S01E02.Bottom.Of.The.Ocean.480p.HDTV.x264-mSD.part1.rar"));
            final FileSonicServiceImpl service = new FileSonicServiceImpl();
            PremiumAccount config = new PremiumAccount();
            config.setUsername("***");
            config.setPassword("***");
            service.setConfig(config);
            final ConnectionSettings settings = new ConnectionSettings();
            //settings.setProxy("localhost", 8118);
            testRun(service, httpFile, settings);
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