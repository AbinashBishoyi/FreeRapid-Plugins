package cz.vity.freerapid.plugins.services.ddlstorage;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class DdlStorageServiceImpl extends XFileSharingServiceImpl {
    @Override
    public String getServiceTitle() {
        return "DDLStorage";
    }

    @Override
    public String getName() {
        return "ddlstorage.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DdlStorageFileRunner();
    }
}