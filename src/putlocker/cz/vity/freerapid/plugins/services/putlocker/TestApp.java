package cz.vity.freerapid.plugins.services.putlocker;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author tong2shot
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/A391FE603855950F"));
            //httpFile.setNewURL(new URL("http://www.sockshare.com/file/C979D1BB2F4387D3"));
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/65AADB926B140666"));
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/B982BF0B0F93EA0D"));
            //httpFile.setNewURL(new URL("http://www.sockshare.com/file/815F22F09D74B38F"));
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/359B64625F4CBEA4")); //hi=low=714616832, mobile=128611192
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/7777D7AA580C02A4")); //mobile failed
            //httpFile.setNewURL(new URL("http://www.putlocker.com/file/249AA5C3E5093044")); //filesize hi is same as low = 421852611, mobile=failed
            //httpFile.setNewURL(new URL("http://www.sockshare.com/file/4UCF3DXJDNK084")); //hi=734791680, low not available, mobile=210841596
            httpFile.setNewURL(new URL("http://putlocker.com/file/B47E2C82F63DB5A4")); //strange case, without www
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final PutLockerServiceImpl service = new PutLockerServiceImpl(); //instance of service - of our plugin
            PutLockerSettingsConfig config = new PutLockerSettingsConfig();
            config.setVideoQuality(VideoQuality.Low);
            service.setConfig(config);
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