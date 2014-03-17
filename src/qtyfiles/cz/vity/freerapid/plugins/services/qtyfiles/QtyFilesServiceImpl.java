package cz.vity.freerapid.plugins.services.qtyfiles;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class QtyFilesServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "QtyFiles";
    }

    @Override
    public String getName() {
        return "qtyfiles.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new QtyFilesFileRunner();
    }

}