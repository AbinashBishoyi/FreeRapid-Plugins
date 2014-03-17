package cz.vity.freerapid.plugins.services.sharephile;

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
            //httpFile.setNewURL(new URL("http://sharephile.com/95520lwzo3aq.html"));
            httpFile.setNewURL(new URL("http://sharephile.com/f88g8gxzjm56.html"));
            
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final SharephileServiceImpl service = new SharephileServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            /*
            boolean ok=false;
            while(!ok)
            {
               try{
                  testRun(service, httpFile, connectionSettings);//download file with service and its Runner
                  ok=true;
               }
               catch(YouHaveToWaitException ye){
                  System.out.println("Sleeping "+ye.getHowManySecondsToWait()+" seconds...");
                  Thread.sleep(1000*ye.getHowManySecondsToWait());
               }
            } */
            testRun(service, httpFile, connectionSettings);
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