package cz.vity.freerapid.plugins.services.keycaptcha;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.dev.plugimpl.DevDialogSupport;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.jdesktop.application.Application;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    @Override
    protected void startup() {
        try {
            final HttpDownloadClient client = new DownloadClient();
            final ConnectionSettings settings = new ConnectionSettings();
            //settings.setProxy("localhost", 8118);
            client.initClient(settings);
            final String url = "https://www.keycaptcha.com/demo-magnetic/";
            //final String url = "http://linkcrypt.ws/dir/w2m63xda7y88jj6";
            client.makeRequest(client.getGetMethod(url), true);
            final KeyCaptcha kc = new KeyCaptcha(new DevDialogSupport(null), client);
            kc.recognize(client.getContentAsString(), url);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) {
        Application.launch(TestApp.class, args);
    }
}