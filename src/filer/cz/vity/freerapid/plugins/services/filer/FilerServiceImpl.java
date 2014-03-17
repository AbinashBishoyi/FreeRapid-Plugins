package cz.vity.freerapid.plugins.services.filer;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author benpicco
 */
public class FilerServiceImpl extends AbstractFileShareService {

    public String getName() {
        return "filer.net";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new FilerFileRunner();
    }
}
