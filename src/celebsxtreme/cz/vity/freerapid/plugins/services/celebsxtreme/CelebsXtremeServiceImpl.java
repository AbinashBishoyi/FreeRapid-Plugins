package cz.vity.freerapid.plugins.services.celebsxtreme;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author tong2shot
 */
public class CelebsXtremeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "celebsxtreme.com";
    }


    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CelebsXtremeFileRunner();
    }

}