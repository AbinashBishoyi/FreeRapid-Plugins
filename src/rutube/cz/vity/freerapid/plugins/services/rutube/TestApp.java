package cz.vity.freerapid.plugins.services.rutube;

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
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://rutube.ru/video/b8dfb3ce7cb2f608a2dabefceaa710db/"));
            //httpFile.setNewURL(new URL("http://rutube.ru/video/f4c224291c2a1e83947d042ba34b2504/"));
            httpFile.setNewURL(new URL("http://rutube.ru/video/6a3a3c7cf020f5af8113398ff266118a/"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final RuTubeServiceImpl service = new RuTubeServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            //new Packet().decode(IoBuffer.wrap(Hex.decodeHex("080000000000911401000000020004706c61790040100000000000000502006b6d70343a766f6c32312f6d6f766965732f30632f61642f30636164363365663365616236633964643738383335663530383134636231632e6d70343f653d3133323934393435313826733d613063323964346635343061343262376434353065366439313136373338666100000000000000000000c000000000000000".toCharArray())), new RtmpSession());
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
            //all output goes to the console
        } catch (Exception e) {//catch possible exception
            e.printStackTrace(); //writes error output - stack trace to console
        }
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