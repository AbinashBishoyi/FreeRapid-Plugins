package cz.vity.freerapid.plugins.services.easysharews;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author Thumb
 */
public class EasyShareWSServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "easyshare.ws";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EasyShareWSFileRunner();
    }

}
