package cz.vity.freerapid.plugins.services.pimpandhost;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author tong2shot
 */
public class PimpAndHostServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "pimpandhost.com";
    }


    @Override
    public boolean supportsRunCheck() {
        return false;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new PimpAndHostFileRunner();
    }

}