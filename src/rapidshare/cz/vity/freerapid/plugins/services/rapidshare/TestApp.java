package cz.vity.freerapid.plugins.services.rapidshare;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFile;
import cz.vity.freerapid.utilities.LogUtils;
import org.jdesktop.application.Application;

import java.net.URL;
import java.util.logging.Logger;

/**
 * @author Ladislav Vitasek
 */
public class TestApp extends PluginDevApplication {
    private final static Logger logger = Logger.getLogger(TestApp.class.getName());

    @Override
    protected void startup() {
        final HttpFile httpFile = getHttpFile();
        try {
            httpFile.setNewURL(new URL("http://rapidshare.com/files/285367824/Two.and.a.half.men.1x01.Pilot.dvd-rip.xvid.multidub.sportboom.part1.rar"));
            testRun(new RapidShareServiceImpl(), httpFile, new ConnectionSettings());
        } catch (Exception e) {
            LogUtils.processException(logger, e);
        }

//        try {
//            testOptions();
//        } catch (Exception e) {
//            LogUtils.processException(logger, e);
//        }
        this.exit();

    }

    private void testOptions() throws Exception {
        final RapidShareServiceImpl service = new RapidShareServiceImpl();
        service.setPluginContext(super.getPluginContext());
        service.showOptions();
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
