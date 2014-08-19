package cz.vity.freerapid.plugins.services.netloadin;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek
 */

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://netload.in/dateiEdDdDkDhN4/Once.Upon.A.Time.S01E22.A.Land.Without.Magic.720p.WEB-DL.DD5.1.H.264-ECI.mkv.htm"));
            testRun(new NetloadInShareServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
