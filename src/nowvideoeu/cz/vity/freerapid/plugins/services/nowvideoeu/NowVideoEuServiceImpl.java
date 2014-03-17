package cz.vity.freerapid.plugins.services.nowvideoeu;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class NowVideoEuServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "nowvideo.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new NowVideoEuFileRunner();
    }

}