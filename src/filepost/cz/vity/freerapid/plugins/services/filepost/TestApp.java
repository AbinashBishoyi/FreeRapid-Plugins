package cz.vity.freerapid.plugins.services.filepost;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author CrazyCoder
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("https://filepost.com/files/3514d318/Standard_Deviants_-_Statistics.part01.rar"));
            httpFile.setNewURL(new URL("http://filepost.com/files/3744a9dd/505w.avi")); //pass: 12ab
            //httpFile.setNewURL(new URL("https://filepost.com/folder/2b7157ef/"));//folder
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            final FilePostServiceImpl service = new FilePostServiceImpl();
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
