package cz.vity.freerapid.plugins.services.sourceforge;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Abinash Bishoyi
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://downloads.sourceforge.net/project/tortoisesvn/1.7.6/Application/TortoiseSVN-1.7.6.22632-x64-svn-1.7.4.msi?r=&amp;ts=1341692947&amp;use_mirror=citylan"));
            httpFile.setNewURL(new URL("http://sourceforge.net/projects/osmf.adobe/files/OSMF%202.0%20Release%20%28final%20source%2C%20ASDocs%2C%20pdf%20guides%20and%20release%20notes%29/OSMF-docs.zip/download"));
            //httpFile.setNewURL(new URL("http://sourceforge.net/projects/openofficeorg.mirror/files/latest/download"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final SourceForgeServiceImpl service = new SourceForgeServiceImpl(); //instance of service - of our plugin
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