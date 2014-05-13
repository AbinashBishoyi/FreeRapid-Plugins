package cz.vity.freerapid.plugins.services.yunfile;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Stan
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://yunfile.com/file/wodeaigsq/299e4ee7/"));
            //httpFile.setNewURL(new URL("http://www.yunfile.com/file/hl01999/f28b2584"));
            //httpFile.setNewURL(new URL("http://www.yunfile.com/file/hl01999/b88b496c"));
            //httpFile.setNewURL(new URL("http://yunfile.com/file/smap2006/d42eef7d"));
            //httpFile.setNewURL(new URL("http://yfdisk.com/file/hl01999/368e6367/"));
            //httpFile.setNewURL(new URL("http://filemarkets.com/file/hl01999/a8afe973/"));
            //httpFile.setNewURL(new URL("http://page1.yunfile.com/file/adslabc/8e044660/"));
            //httpFile.setNewURL(new URL("http://page1.yunfile.com/file/bjfrj/1196f8c5/")); //File Size: 74.64 MB
            httpFile.setNewURL(new URL("http://page2.yunfile.com/fs/2a6r7s3ebnfa8lcfan9996/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("127.0.0.1", 9080, Proxy.Type.SOCKS); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final YunFileServiceImpl service = new YunFileServiceImpl(); //instance of service - of our plugin
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