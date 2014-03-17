package cz.vity.freerapid.plugins.services.extrashare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Javi
 */
public class ExtraShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "extrashare.us";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ExtraShareFileRunner();
    }

}