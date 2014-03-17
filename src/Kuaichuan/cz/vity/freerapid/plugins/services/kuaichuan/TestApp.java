package cz.vity.freerapid.plugins.services.kuaichuan;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import org.jdesktop.application.Application;

import java.net.URL;

/**
 * Test application for kuai.xunlei.com
 *
 * @author Tommy Yang
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile(); //creates new test instance of HttpFile
        try {
            //we set file URL
            httpFile.setNewURL(new URL("http://kuai.xunlei.com/d/BQJMDYRXGSAL"));
            //the way we connect to the internet
            final ConnectionSettings connectionSettings = new ConnectionSettings();// creates default connection
            connectionSettings.setProxy("218.247.129.35", 80); //eg we can use local proxy to sniff HTTP communication
            //then we tries to download
            final KuaiServiceImpl service = new KuaiServiceImpl(); //instance of service - of our plugin
            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
//            testRunCheck(service, httpFile, connectionSettings);
            //all output goes to the console

            // URL may change.
//            httpFile.setNewURL(new URL("http://dl1.c13.sendfile.vip.xunlei.com:8000/%5BTPimage%5D%202012%2D06%2D27%20No%2E337%20Opal%20%5B49P228M%5D%2Epart1%2Erar?key=1c279d4a12de1cd8bbe29f8baba25e99&file_url=%2Fgdrive%2Fresource%2FF5%2F75%2FF54AD27795F2367B7BD4200E46C005FC3511AC75&file_type=1&authkey=FC852D7D0AA4681764C592F22C03C50A7E4E6BDEA43B57787EC218D60CD72090&exp_time=1346130179&from_uid=134660501&task_id=5758398589475870466&get_uid=1004556171&f=lixian.vip.xunlei.com&fid=Rc2z0hCtbrLHylEkSypDy5m/PXUAAIAMAAAAAPVK0neV8jZ7e9QgDkbABfw1Eax1&mid=666&threshold=150&tid=401C4E580B78B6B10E15AC626FF08DDF&srcid=7&verno=1"));
//            testRun(service, httpFile, connectionSettings);//download file with service and its Runner
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
