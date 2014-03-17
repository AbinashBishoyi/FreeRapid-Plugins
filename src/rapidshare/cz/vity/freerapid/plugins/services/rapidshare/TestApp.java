package cz.vity.freerapid.plugins.services.rapidshare;

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
            httpFile.setFileUrl(new URL("http://rapidshare.com/files/137292919/Alice_-_Sexy_Teenie.wmv.002"));
            run(new RapidShareServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
