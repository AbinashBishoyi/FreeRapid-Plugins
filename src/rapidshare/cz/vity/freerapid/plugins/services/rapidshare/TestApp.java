package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.utilities.LogUtils;
import org.jdesktop.application.Application;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    private final static Logger logger = Logger.getLogger(TestApp.class.getName());

    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://rapidshare.com/files/992071446/EP083_-_Poké_Ball_Peril.rar"));
            final ConnectionSettings settings = new ConnectionSettings();
            //settings.setProxy("localhost", 8081);
            testRun(new RapidShareServiceImpl(), httpFile, settings);
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
