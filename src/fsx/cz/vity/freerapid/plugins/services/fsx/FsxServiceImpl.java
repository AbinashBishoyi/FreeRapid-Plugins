package cz.vity.freerapid.plugins.services.fsx;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Javi
 */
public class FsxServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "fsx.hu";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FsxFileRunner();
    }

}