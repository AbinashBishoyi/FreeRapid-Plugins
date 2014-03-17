package cz.vity.freerapid.plugins.services.recaptcha;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import cz.vity.freerapid.plugins.webclient.interfaces.ShareDownloadService;

/**
 * Class that provides basic info about plugin
 * @author Vity+Team
 */
public class ReCaptchaServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "recaptcha.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PluginRunner() {
            public void init(ShareDownloadService shareDownloadService, HttpFileDownloadTask downloadTask) throws Exception {

            }

            public void runCheck() throws Exception {

            }

            public void run() throws Exception {

            }
        };
    }

}