package cz.vity.freerapid.plugins.services.filebitnet;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author RickCL
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://file-bit.net/3upw6g7e7zsj/PIER999-081031-Selena_Veronica-Set_01-125.rar.html"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            // connectionSettings.setProxy("localhost", 8081);
            testRun(new FilebitNetFilesShareServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}