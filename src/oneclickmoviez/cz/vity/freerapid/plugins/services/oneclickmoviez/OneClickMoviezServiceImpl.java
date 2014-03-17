package cz.vity.freerapid.plugins.services.oneclickmoviez;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class OneClickMoviezServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "oneclickmoviez.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new OneClickMoviezFileRunner();
    }

}