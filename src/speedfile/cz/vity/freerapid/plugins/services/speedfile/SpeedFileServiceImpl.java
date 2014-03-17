package cz.vity.freerapid.plugins.services.speedfile;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Vity
 */
public class SpeedFileServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "speedfile.cz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SpeedFileFileRunner();
    }

}