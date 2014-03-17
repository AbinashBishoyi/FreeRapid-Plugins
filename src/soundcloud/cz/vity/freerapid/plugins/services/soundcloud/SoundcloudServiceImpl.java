package cz.vity.freerapid.plugins.services.soundcloud;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Vity
 */
public class SoundcloudServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "soundcloud.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SoundcloudFileRunner();
    }

}