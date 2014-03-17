package cz.vity.freerapid.plugins.services.megacrypter;

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
            //password is "abc"
            httpFile.setNewURL(new URL("http://megacrypter.com/!SrdV6qP350Ni6c_4-LIiH9dpjUGJ0x7UgCZoKw3YK9SWq1Xw-gO498sT8EukPnC_koxnC1Wg15zKjrY9rFzc42InA2aWqxS0blXUbXibXQlw7LMAS7yoJuVroDYzl0LOrRRhuiH8Am6EM-M8ouAuRPcq_vZ6GmqFF80yh1bzru0!2db461a8"));
            //no password
            httpFile.setNewURL(new URL("http://megacrypter.com/!NcdIWElSM2xkdwdwYy3OcQTUWExKlLMUr6OwDzwZXj8Kld9eVMLZMetYxhoprSKUyGBTOIWszWLrBGPkLVQK8LI-c-ki7SYQ9flvbVFqrbpdX65yawXr2ob1VGGsmDSk!4ded9430"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MegaCrypterServiceImpl service = new MegaCrypterServiceImpl(); //instance of service - of our plugin
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