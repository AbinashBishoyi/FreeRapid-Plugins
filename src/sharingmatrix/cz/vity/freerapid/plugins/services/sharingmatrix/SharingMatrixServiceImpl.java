package cz.vity.freerapid.plugins.services.sharingmatrix;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class SharingMatrixServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "sharingmatrix.com";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new SharingMatrixFileRunner();
    }
}
