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
            httpFile.setNewURL(new URL("http://www.filesonic.com/en/file/1485411/XPMODDEDUP2DATE.part1.rar"));
            final FileSonicServiceImpl service = new FileSonicServiceImpl();

            PremiumAccount config = new PremiumAccount();
            config.setUsername("***");
            config.setPassword("***");
            service.setConfig(config);

            testRun(service, httpFile, new ConnectionSettings());
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