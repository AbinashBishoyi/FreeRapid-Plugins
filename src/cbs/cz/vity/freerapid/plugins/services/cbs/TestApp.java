package cz.vity.freerapid.plugins.services.cbs;

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
            //InputStream is = new BufferedInputStream(new FileInputStream("C:\\Users\\Administrator\\Desktop\\logtest.properties"));
            //LogManager.getLogManager().readConfiguration(is);
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.cbs.com/shows/48_hours/video/2241801823/secrets-of-a-marriage"));//regular
            //httpFile.setNewURL(new URL("http://www.cbs.com/shows/cbs_evening_news/video/H_TIHNUkVN_jBPNl_mG5UKVleBEMvPzE/1-15-air-force-cheating-scandal-leaves-cloud-hanging-over-missile-force-how-prepared-is-calif-for-next-big-quake-/")); //contains subtitle
            //httpFile.setNewURL(new URL("http://www.cbs.com/shows/cbs_evening_news/video/kCouvpWtR5KsA61cue2whOe_S9ul87vd/1-18-w-va-residents-skeptical-of-water-safety-reports-iraqis-pray-for-peace-brace-for-more-violence/")); //doesn't contain subtitle
            //httpFile.setNewURL(new URL("http://www.cbsnews.com/videos/captions/H_TIHNUkVN_jBPNl_mG5UKVleBEMvPzE.adb_xml?fname=CBS+Evening+News+-+The+full+episode+of+the+CBS+Evening+News+from+January+15%252C+2014")); //subtitle
            httpFile.setNewURL(new URL("http://www.cbs.com/shows/2_broke_girls/video/UVsLa0P8W2AcE7fF16Mx0wdsL_5KOSr0/2-broke-girls-and-the-french-kiss/"));
            //httpFile.setNewURL(new URL("http://www.cbsstatic.com/closedcaption/CBS_2_BROKE_GIRLS_312_CONTENT_CIAN_caption.xml?fname=2+Broke+Girls+-+And+The+French+Kiss"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final CbsServiceImpl service = new CbsServiceImpl(); //instance of service - of our plugin
            SettingsConfig config = new SettingsConfig();
            config.setDownloadSubtitles(true);
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