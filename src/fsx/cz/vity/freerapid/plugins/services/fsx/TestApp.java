package cz.vity.freerapid.plugins.services.fsx;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * @author Hosszu
 */

public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            // we set file URL, e. g.:
            // http://s1.fsx.hu/2009/08/31/21/08/4811f9a7f69d126af7054218e9d4944e/Two.Guys.And.A.Girl.S01E03.HUN.DVBrip.XviD-%5BDAMON%5D.part2.rar
            // http://s1.fsx.hu/2009/08/31/21/08/712fd1c6e9b9c9f406efb2d46ee26ab0/tgaag.sfv
            // http://s2.fsx.hu/2009/09/01/19/41/31d7228297a63f100f12661048156074/Two.Guys.And.A.Girl.S01E04.HUN.TVrip.XviD-%5BDAMON%5D.part3.rar
            // http://s2.fsx.hu/2009/09/01/19/41/392f5ecc641c030e1b9eece884e3dfad/Two.Guys.And.A.Girl.S01E04.HUN.TVrip.XviD-%5BDAMON%5D.part2.rar
            // http://s3.fsx.hu/2011/01/27/09/33/3056ff897da4cbd6898ec0f175ed7a61/that70s.s01e01-tvtime.sfv
            // http://s3.fsx.hu/2011/01/27/09/33/43c521d59e8b80b610b47b53e70f4582/that70s.s01e01-tvtime.rar
            // http://s4.fsx.hu/2010/08/12/12/19/74403c63fe47f8f892b1e353f8bb3545/Kemenykalap_es_krumpliorr.r01
            // http://s4.fsx.hu/2010/08/12/12/19/74a8df6237b0af540c55bc0c3db19c49/Kemenykalap_es_krumpliorr.r48

            httpFile.setNewURL(new URL("http://s3.fsx.hu/2011/01/27/09/33/3056ff897da4cbd6898ec0f175ed7a61/that70s.s01e01-tvtime.sfv"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            //then we try to download
            final FsxServiceImpl service = new FsxServiceImpl(); //instance of service - of our plugin
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