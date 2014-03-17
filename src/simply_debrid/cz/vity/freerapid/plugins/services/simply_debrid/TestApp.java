package cz.vity.freerapid.plugins.services.simply_debrid;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author birchie
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.filefactory.com/file/1cfblf4u7v8v/n/%5BACX%5DBlade_of_the_Immortal_-_11_-_Wings_%5BSaintDeath%5D_%5B40407F56%5D.mkv"));
            httpFile.setNewURL(new URL("http://uptobox.com/ndowcvrxt4m3/Blackknight_Anifecta__Sekirei_PE_BD_-_13.mkv"));
            //httpFile.setNewURL(new URL("http://www.speedyshare.com/SuXtj/Dragonball-038-Five-Murasakis.mkv"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final Simply_DebridServiceImpl service = new Simply_DebridServiceImpl(); //instance of service - of our plugin
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