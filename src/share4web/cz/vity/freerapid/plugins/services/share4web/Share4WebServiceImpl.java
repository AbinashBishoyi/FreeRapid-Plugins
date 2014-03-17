package cz.vity.freerapid.plugins.services.share4web;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class Share4WebServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "share4web.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Share4WebFileRunner();
    }

}