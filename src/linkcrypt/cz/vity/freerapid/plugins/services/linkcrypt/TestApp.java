package cz.vity.freerapid.plugins.services.linkcrypt;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //   httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/v452559zi726o4l"));//no captcha
            //   httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/w2m63xda7y88jj6"));//keycaptcha
            //   httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/5onop46s332ch62"));//captx
            //
            //   httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/z3x2hwx54mp94rs"));//CaptX        pass:freerapid
            //httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/y46eb59cy0i26p6"));//TextX        pass:freerapid
            //   httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/p2u821563msl3nr"));//Key-Captcha  pass:freerapid
            httpFile.setNewURL(new URL("http://linkcrypt.ws/dir/gy21isbik5bm8y2"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final LinkCryptServiceImpl service = new LinkCryptServiceImpl(); //instance of service - of our plugin
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