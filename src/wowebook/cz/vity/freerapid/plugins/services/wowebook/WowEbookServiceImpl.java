package cz.vity.freerapid.plugins.services.wowebook;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Abinash Bishoyi
 */
public class WowEbookServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "wowebook.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WowEbookFileRunner();
    }

}