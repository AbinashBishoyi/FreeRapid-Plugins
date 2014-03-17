package cz.vity.freerapid.plugins.services.beemp3;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BeeMP3ServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "beemp3.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BeeMP3FileRunner();
    }

}