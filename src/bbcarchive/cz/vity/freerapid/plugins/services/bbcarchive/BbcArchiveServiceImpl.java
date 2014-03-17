package cz.vity.freerapid.plugins.services.bbcarchive;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class BbcArchiveServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "bbc.co.uk/archive";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BbcArchiveFileRunner();
    }

}