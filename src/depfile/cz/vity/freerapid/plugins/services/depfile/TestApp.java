package cz.vity.freerapid.plugins.services.depfile;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://depfile.com/downloads/i/799054/f/KoOoRa.CoM_Rey_Mysterio_-_The_Life_of_a_Masked_Man_DVD_3_Discs_By_MASHA_ERA.part01.rar.html"));
            //httpFile.setNewURL(new URL("http://depfile.com/a1Ek8mPCz"));
            httpFile.setNewURL(new URL("http://depfile.com/cAVEDlLO"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("203.176.183.198", 8080); //eg we can use local proxy to sniff HTTP communication
            final DepFileServiceImpl service = new DepFileServiceImpl();
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}