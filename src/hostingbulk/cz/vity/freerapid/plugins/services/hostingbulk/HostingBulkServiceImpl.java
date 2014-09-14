package cz.vity.freerapid.plugins.services.hostingbulk;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class HostingBulkServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "HostingBulk";
    }

    @Override
    public String getName() {
        return "hostingbulk.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new HostingBulkFileRunner();
    }

}