package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import java.io.IOException;
import org.jdesktop.application.Application;

import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek & Tomáš Procházka <to.m.p@atomsoft.cz>
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {

        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://www.uloz.to/xkNQ3TJ/madonna-frozen-mp3"));
            testRun(new UlozToServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) throws IOException {
        Handler fh = new FileHandler("./TestApp.xml");
        Logger.getLogger("").addHandler(fh);
        Application.launch(TestApp.class, args);
    }
}
