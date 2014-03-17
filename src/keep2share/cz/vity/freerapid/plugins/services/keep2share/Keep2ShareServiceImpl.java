package cz.vity.freerapid.plugins.services.keep2share;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Keep2ShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "keep2share.cc";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Keep2ShareFileRunner();
    }

}