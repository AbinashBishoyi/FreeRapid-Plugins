package cz.vity.freerapid.plugins.services.bigshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Thumb
 */
public class BigShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "bigshare.eu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BigShareFileRunner();
    }

}
