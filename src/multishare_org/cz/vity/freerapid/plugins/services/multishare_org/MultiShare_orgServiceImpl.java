package cz.vity.freerapid.plugins.services.multishare_org;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class MultiShare_orgServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "multishare.org";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new MultiShare_orgFileRunner();
    }

}