package cz.vity.freerapid.plugins.services.linksafe;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author hanakus
 */
public class LinkSafeServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "linksafe.me";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkSafeFileRunner();
    }

}