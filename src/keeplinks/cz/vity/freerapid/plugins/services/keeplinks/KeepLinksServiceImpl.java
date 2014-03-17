package cz.vity.freerapid.plugins.services.keeplinks;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class KeepLinksServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "keeplinks.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new KeepLinksFileRunner();
    }

}