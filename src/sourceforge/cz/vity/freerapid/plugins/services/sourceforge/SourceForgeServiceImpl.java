package cz.vity.freerapid.plugins.services.sourceforge;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Abinash Bishoyi
 */
public class SourceForgeServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "sourceforge.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SourceForgeFileRunner();
    }

}