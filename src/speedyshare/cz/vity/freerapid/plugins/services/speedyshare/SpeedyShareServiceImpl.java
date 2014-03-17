package cz.vity.freerapid.plugins.services.speedyshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class SpeedyShareServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "speedyshare.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;//ok
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpeedyShareFileRunner();
    }

}