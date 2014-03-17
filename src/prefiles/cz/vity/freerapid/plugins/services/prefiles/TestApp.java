package cz.vity.freerapid.plugins.services.prefiles;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author CrazyCoder, Abinash Bishoyi
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://prefiles.com/lcwluokw8h5q/Packtpub.Visual.Studio.2010.Best.Practices.Aug.2012.rar"));
            httpFile.setNewURL(new URL("http://prefiles.com/ykauetqx0mh4/MK.Analyzing.the.Social.Web.Mar.2013.rar"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            final PreFilesServiceImpl service = new PreFilesServiceImpl();
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
