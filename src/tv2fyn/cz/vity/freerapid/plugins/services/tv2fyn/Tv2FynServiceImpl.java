package cz.vity.freerapid.plugins.services.tv2fyn;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class Tv2FynServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "tv2fyn.dk";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Tv2FynFileRunner();
    }

}