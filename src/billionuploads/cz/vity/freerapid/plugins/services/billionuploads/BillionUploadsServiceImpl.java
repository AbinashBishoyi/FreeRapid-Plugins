package cz.vity.freerapid.plugins.services.billionuploads;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class BillionUploadsServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "BillionUploads";
    }

    @Override
    public String getName() {
        return "billionuploads.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BillionUploadsFileRunner();
    }

}