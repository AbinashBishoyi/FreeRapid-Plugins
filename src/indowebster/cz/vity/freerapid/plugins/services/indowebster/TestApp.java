package cz.vity.freerapid.plugins.services.indowebster;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for indowebster.com
 *
 * @author zid
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            //httpFile.setNewURL(new URL("http://www.indowebster.com/download/files/lm_indofiles_org_tuneuputilities2012_with_key"));
            //httpFile.setNewURL(new URL("http://www.indowebster.com/idfl_us_bluecrush_bm72_nitro_part1.html"));
            //httpFile.setNewURL(new URL("http://www.indowebster.com/download/files/tendres_cousines1980_avi_001")); //pass : Tangerine
            //httpFile.setNewURL(new URL("http://www.indowebster.com/IHeartHuckabeesDVDRip.html"));
            //httpFile.setNewURL(new URL("http://www.indowebster.com/idfl_us_the_station_agent_2003_dvdrip_h264_aac_antix.html"));
            //httpFile.setNewURL(new URL("http://files.indowebster.com/utux_idfl_us_babymakersdvr_ganool.html"));
            //httpFile.setNewURL(new URL("http://files.indowebster.com/download/files/wmmib3482012"));
            //httpFile.setNewURL(new URL("http://files.indowebster.com/download/video/_t_n_kamen_rider_ooo_junction06_2665979c_dvd"));
            //httpFile.setNewURL(new URL("http://files.indowebster.com/wmrsdntevl5482012_part1.html"));
            //httpFile.setNewURL(new URL("http://files.indowebster.com/download/files/wmrsdntevl5482012_part1"));
            httpFile.setNewURL(new URL("http://files.indowebster.com/wmnysm482013_part1.html"));

            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final IndowebsterServiceImpl service = new IndowebsterServiceImpl(); //instance of service - of our plugin
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
