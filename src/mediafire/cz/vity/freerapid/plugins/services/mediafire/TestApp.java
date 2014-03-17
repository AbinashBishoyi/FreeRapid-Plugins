package cz.vity.freerapid.plugins.services.mediafire;

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
            //httpFile.setNewURL(new URL("http://www.mediafire.com/?ofll36ptol2g3x0"));  //direct download
            httpFile.setNewURL(new URL("http://www.mediafire.com/?4nby1h7o71eoweg"));
            ///httpFile.setNewURL(new URL("http://www.mediafire.com/download/u6cor1a8lpjjbuf/test.rar"));
            ///httpFile.setNewURL(new URL("http://www.mediafire.com/?7m4fjgk7p4ea2"));
            //httpFile.setNewURL(new URL("http://www.mediafire.com/?bjegtrkfv9inn5f"));//password is "coplay"
            //httpFile.setNewURL(new URL("http://www.mediafire.com/?7m4fjgk7p4ea2"));//folder
            //httpFile.setNewURL(new URL("http://www.mediafire.com/?uvn6ot51q5vco"));//folder of folders
            //httpFile.setNewURL(new URL("http://www.mediafire.com/?lrr6mv33a67s8pb,ogxiipdtc34gzh7,kte3dr6d62i3p83,cnk1yaandyuder7,dssonwaaclcwopw,g74urguvuvjb0a1,7dxzppwfrne38i2,yzynzwzdyfe7gkz,q3bcstafuccq4ng,eyb2ktbzxr5ales,c0d8qfnv7j6l101,ehmasgbwgirdiik,yo85f5payma55ws,un6ebgiovase89a,whcdo271dx7dtx7,bkp6e507b1rpj22"));//multi link
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final MediafireServiceImpl service = new MediafireServiceImpl(); //instance of service - of our plugin
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