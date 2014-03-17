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
            httpFile.setNewURL(new URL(" http://netload.in/dateihQrwIMZOii/nnkrkonose2.part01.rar.htm"));
            //   httpFile.setNewURL(new URL("http://netload.in/dateiMTgzMjUwMj/The.IT.Crowd.S03E05.WS.PDTV.XviD-RiVER.rar.htm"));
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
