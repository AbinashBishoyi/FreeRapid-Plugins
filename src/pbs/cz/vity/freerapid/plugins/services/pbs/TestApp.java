package cz.vity.freerapid.plugins.services.pbs;

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
            //log everything
            //InputStream is = new BufferedInputStream(new FileInputStream("E:\\Stuff\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://video.pbs.org/video/2163057527/"));
            //httpFile.setNewURL(new URL("http://ga.video.cdn.pbs.org/captions/nova/77aacc41-923c-4262-a265-4c003b674319/captions/203507_Encoded.srt?fname=The+Fabric+of+the+Cosmos+-+What+Is+Space%253F"));
            //httpFile.setNewURL(new URL("http://ga.video.cdn.pbs.org/captions/nova/77aacc41-923c-4262-a265-4c003b674319/captions/203505_Encoded.sami?fname=The+Fabric+of+the+Cosmos+-+What+Is+Space%253F"));
            //httpFile.setNewURL(new URL("http://ga.video.cdn.pbs.org/captions/nova/77aacc41-923c-4262-a265-4c003b674319/captions/203506_Encoded.dfxp?fname=The+Fabric+of+the+Cosmos+-+What+Is+Space%253F"));
            httpFile.setNewURL(new URL("http://ga.video.cdn.pbs.org/captions/american-experience/453a663a-4e87-4e36-88d4-692845f27820/captions/147627_Encoded.srt?fname=The+Presidents+-+Nixon"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final PbsServiceImpl service = new PbsServiceImpl(); //instance of service - of our plugin
            //runcheck makes the validation
            SettingsConfig config = new SettingsConfig();
            config.setDownloadSubtitles(true);
            service.setConfig(config);
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