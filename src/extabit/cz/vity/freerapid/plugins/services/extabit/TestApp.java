package cz.vity.freerapid.plugins.services.extabit;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Thumb
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://extabit.com/file/28dhkgg6qwcpq"));
            //    httpFile.setNewURL(new URL("http://extabit.com/file/28de1e1prnzxh"));
            httpFile.setNewURL(new URL("http://extabit.com/file/2cq2pbr5qc9kb/"));
            //httpFile.setNewURL(new URL("http://extabit.com/file/2dvq5ibmdftqa"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("180.250.129.184", 8080); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final ExtabitServiceImpl service = new ExtabitServiceImpl(); //instance of service - of our plugin
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