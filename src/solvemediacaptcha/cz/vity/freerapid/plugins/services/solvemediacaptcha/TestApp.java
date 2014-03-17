package cz.vity.freerapid.plugins.services.solvemediacaptcha;

import cz.vity.freerapid.plugins.dev.PluginDevApplication;
import cz.vity.freerapid.plugins.dev.plugimpl.DevDialogSupport;
import cz.vity.freerapid.plugins.webclient.ConnectionSettings;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import org.jdesktop.application.Application;

import java.util.logging.Logger;

/**
 * @author ntoskrnl
 */
public class TestApp extends PluginDevApplication {
    private final static Logger logger = Logger.getLogger(TestApp.class.getName());

    @Override
    protected void startup() {
        try {
            final HttpDownloadClient client = new DownloadClient();
            final ConnectionSettings settings = new ConnectionSettings();
            //settings.setProxy("localhost", 8118);
            client.initClient(settings);
            final CaptchaSupport captchaSupport = new CaptchaSupport(client, new DevDialogSupport(null));

            // see http://solvemedia.com/publishers/captcha-type-in
            final SolveMediaCaptcha captcha = new SolveMediaCaptcha("5rpD0bf9RBZ.lyWs.lIKdv8bohgdaCBW", client, captchaSupport);
            final MethodBuilder methodBuilder = new MethodBuilder(client).setAction("https://portal.solvemedia.com/portal/public/demo-1?fmt=jsonp&callback=human_update&demo_type=secure");
            captcha.modifyResponseMethod(methodBuilder);
            client.makeRequest(methodBuilder.toGetMethod(), true);
            logger.info(client.getContentAsString());
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Application.launch(TestApp.class, args);
    }
}
