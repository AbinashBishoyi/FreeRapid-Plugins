package cz.vity.freerapid.plugins.services.linkto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class LinkToServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "linkto.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LinkToFileRunner();
    }

}