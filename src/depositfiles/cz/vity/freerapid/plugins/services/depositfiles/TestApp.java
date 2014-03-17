package cz.vity.freerapid.plugins.services.depositfiles;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://dfiles.eu/files/g2drb2zoq"));
            // httpFile.setNewURL(new URL("http://depositfiles.com/de/files/7845416"));
            //httpFile.setNewURL(new URL("http://depositfiles.com/en/folders/K50LZLHAY"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            // connectionSettings.setProxy("localhost", 8081);
            testRun(new DepositFilesShareServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}