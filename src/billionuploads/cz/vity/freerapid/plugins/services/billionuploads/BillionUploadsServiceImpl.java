package cz.vity.freerapid.plugins.services.billionuploads;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BillionUploadsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "billionuploads.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BillionUploadsFileRunner();
    }

}