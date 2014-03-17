package cz.vity.freerapid.plugins.services.ceskatelevize;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author JPEXS
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/10084897100-kluci-v-akci/211562221900012/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/loh/videoarchiv/sporty/serm/189028-protest-korejske-sermirky-sin-a-lam/"));
            //httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/porady/1143638030-ct-live/20754215404-ct-live-vlasta-redl/video/"));
            httpFile.setNewURL(new URL("http://www.ceskatelevize.cz/ivysilani/1126672097-otazky-vaclava-moravce/213411030510609-otazky-vaclava-moravce-2-cast/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("195.122.213.61", 3128); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final CeskaTelevizeServiceImpl service = new CeskaTelevizeServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
        this.exit();//exit application
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);//starts the application - calls startup() internally
    }
}