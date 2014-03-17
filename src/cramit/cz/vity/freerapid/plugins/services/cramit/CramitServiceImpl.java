package cz.vity.freerapid.plugins.services.cramit;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class CramitServiceImpl extends XFileSharingServiceImpl {

    @Override
    public String getServiceTitle() {
        return "Cramit";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CramitRunner();
    }

    @Override
    public String getName() {
        return "cramit.in";
    }

}
