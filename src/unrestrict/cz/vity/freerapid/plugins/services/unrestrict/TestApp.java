package cz.vity.freerapid.plugins.services.unrestrict;

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
            //httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=LBit_XKdWgY"));                      // free user ++
            httpFile.setNewURL(new URL("http://www.youtube.com/watch?v=R1-BTf3_Mys"));                      // free user ++
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/9A007E6F8D946187"));                // registered user +
            //httpFile.setNewURL(new URL("http://rapidgator.net/file/d5f481e93a5daee9dcd2bac1f9d89b7e/"));  // VIP user (unable to test)
            //httpFile.setNewURL(new URL("http://www.kingfiles.net/bo054yqdblpy/"));                        // unsupported link

            //httpFile.setNewURL(new URL("http://unrestrict.li/dl/2b1f2f55b55c/"));                         // pre-generated link free
            //httpFile.setNewURL(new URL("http://unrestrict.li/dl/77f40c79935d/"));                         // pre-generated link free (by registerd)
            //httpFile.setNewURL(new URL("http://unrestrict.li/dl/eaa6555d4b29/"));                         // pre-generated link registered

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final cz.vity.freerapid.plugins.services.unrestrict.UnRestrictServiceImpl service = new cz.vity.freerapid.plugins.services.unrestrict.UnRestrictServiceImpl(); //instance of service - of our plugin
            /*
            //we set premium account details
            final PremiumAccount config = new PremiumAccount();
            config.setUsername("****");
            config.setPassword("****");
            service.setConfig(config);
            //*/
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