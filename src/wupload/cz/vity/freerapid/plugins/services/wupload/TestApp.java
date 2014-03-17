package cz.vity.freerapid.plugins.services.wupload;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Coloss
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://www.wupload.com/file/2530316127/logovq.jpg"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8118);
            testRun(new WuploadShareServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}