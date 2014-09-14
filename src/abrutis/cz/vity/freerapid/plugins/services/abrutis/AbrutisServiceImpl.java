package cz.vity.freerapid.plugins.services.abrutis;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class AbrutisServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "abrutis.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new AbrutisFileRunner();
    }

}