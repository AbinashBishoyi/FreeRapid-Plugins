package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Ladislav Vitasek & Tomáš Procházka <to.m.p@atomsoft.cz>
 */
public class TestApp extends PluginDevApplication {
    protected void startup() {

        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://uloz.to/340444/Brut%C3%A1ln%C3%AD%20Nikita%201%2010%20CHOICE%20%28Volba%29.part2.rar"));
            testRun(new UlozToServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
