package cz.vity.freerapid.plugins.services.wowebookcom;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author CrazyCoder
 */
public class WowEbookComServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "wowebook.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WowEbookComFileRunner();
    }

}
