package cz.vity.freerapid.plugins.services.ifolder;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
//            httpFile.setNewURL(new URL("http://v1.ifolder.ru/16513897"));
            httpFile.setNewURL(new URL("http://rusfolder.com/37668254"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081);
            testRun(new IFolderServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}