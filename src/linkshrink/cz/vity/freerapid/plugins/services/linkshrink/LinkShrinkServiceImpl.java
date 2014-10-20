package cz.vity.freerapid.plugins.services.linkshrink;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LinkShrinkServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "linkshrink.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkShrinkFileRunner();
    }

}