package cz.vity.freerapid.plugins.services.saavn;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class SaavnServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "saavn.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SaavnFileRunner();
    }

}