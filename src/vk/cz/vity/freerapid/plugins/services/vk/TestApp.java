package cz.vity.freerapid.plugins.services.vk;

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
        final HttpFile httpFile = getHttpFile();
        try {
            //httpFile.setNewURL(new URL("http://vk.com/video_ext.php?oid=17211115&id=165308093&hash=9080acdaadf272d3"));
            //httpFile.setNewURL(new URL("http://vk.com/video_ext.php?oid=-36880507&id=165376600&hash=38b5d44a4e26d0ea"));
            httpFile.setNewURL(new URL("http://vk.com/video193844286_165427099")); //requires login, redirect to biqle as workaround
            //httpFile.setNewURL(new URL("http://vk.com/video193844286_165616118"));
            final ConnectionSettings connectionSettings = new ConnectionSettings();
            //connectionSettings.setProxy("localhost", 8081); //eg we can use local proxy to sniff HTTP communication
            final VkServiceImpl service = new VkServiceImpl();
            VkSettingsConfig config = new VkSettingsConfig();
            config.setVideoQuality(VideoQuality._360);
            service.setConfig(config);
            testRun(service, httpFile, connectionSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.exit();
    }

    /**
     * Main start method for running this application
     * Called from IDE
     *
     * @param args arguments for application
     */
    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}