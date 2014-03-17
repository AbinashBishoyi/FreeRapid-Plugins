package cz.vity.freerapid.plugins.services.wetransfer;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class WeTransferServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "wetransfer.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new WeTransferFileRunner();
    }

}