package cz.vity.freerapid.plugins.services.nowdownload;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class NowDownloadServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "nowdownload.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NowDownloadFileRunner();
    }

}