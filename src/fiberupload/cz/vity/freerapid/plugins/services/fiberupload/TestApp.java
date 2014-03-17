package cz.vity.freerapid.plugins.services.fiberupload;

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
            //httpFile.setNewURL(new URL("http://fiberupload.com/lnqp1xl8y6cp/LECGVIDBTS.zip"));
            //httpFile.setNewURL(new URL("http://fiberupload.com/tdwnzoiwci72/1852334312_Prob.rar"));
            //httpFile.setNewURL(new URL("http://fiberupload.com/8b213iacf623/0486449688_MathEarnest.rar"));
            httpFile.setNewURL(new URL("http://fiberupload.com/1dbwycuuv1ia/0486207897_MathFun.rar"));
            //httpFile.setNewURL(new URL("http://bulletupload.com/kjex768f3nfv"));
            //httpFile.setNewURL(new URL("http://bulletupload.com/lnqp1xl8y6cp/LECGVIDBTS.zip"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.setProxy("118.97.107.82", 8080); //eg we can use local proxy to sniff HTTP communication
            final FiberUploadServiceImpl service = new FiberUploadServiceImpl();

            //for testing purpose
            /*
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("freerapid");
            config.setPassword("freerapid");
            service.setConfig(config);
            */
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