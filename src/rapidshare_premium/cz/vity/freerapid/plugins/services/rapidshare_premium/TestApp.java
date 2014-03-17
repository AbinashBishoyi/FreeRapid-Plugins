package cz.vity.freerapid.plugins.services.rapidshare_premium;

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
            //httpFile.setNewURL(new URL("http://rapidshare.com/files/145378634/DSCF5628.JPG.html"));
            httpFile.setNewURL(new URL("http://rapidshare.com/files/141950563/pb.04.02.720.part01.rar"));

            testRun(new RapidShareServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.exit();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
