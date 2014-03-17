package cz.vity.freerapid.plugins.services.hulkshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class HulkshareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "hulkshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HulkshareFileRunner();
    }

}