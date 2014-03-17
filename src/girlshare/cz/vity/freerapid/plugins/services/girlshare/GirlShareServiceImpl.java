package cz.vity.freerapid.plugins.services.girlshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author TROJal.exe
 */
public class GirlShareServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "girlshare.ro";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new GirlShareFileRunner();
    }

}