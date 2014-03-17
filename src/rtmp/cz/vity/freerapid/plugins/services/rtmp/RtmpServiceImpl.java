package cz.vity.freerapid.plugins.services.rtmp;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpFileDownloadTask;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;
import cz.vity.freerapid.plugins.webclient.interfaces.ShareDownloadService;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class RtmpServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "rtmp";
    }

    @Override
    public int getMaxDownloadsFromOneIP() {
        return 0;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PluginRunner() {
            @Override
            public void init(ShareDownloadService shareDownloadService, HttpFileDownloadTask downloadTask) throws Exception {
            }

            @Override
            public void runCheck() throws Exception {
            }

            @Override
            public void run() throws Exception {
            }
        };
    }

}