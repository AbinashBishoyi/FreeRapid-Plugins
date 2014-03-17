package cz.vity.freerapid.plugins.services.xfilesharingcommon;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class XFileSharingCommonServiceImpl extends AbstractFileShareService {
    public XFileSharingCommonServiceImpl() {
        super();
    }

    @Override
    public String getName() {
        return "xfilesharingcommon.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    public PluginRunner getPluginRunnerInstance() {
        return new XFileSharingCommonFileRunner();
    }

}