package cz.vity.freerapid.plugins.services.uploadedto;

import cz.vity.freerapid.plugins.dev.PluginApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginApplication {
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setFileUrl(new URL("http://uploaded.to/file/h6gwzp/www.softarchive.net_txenl5.tar.001"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            connectionSettings.setProxy("localhost", 8081);
            run(new UploadedToShareServiceImpl(), httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
