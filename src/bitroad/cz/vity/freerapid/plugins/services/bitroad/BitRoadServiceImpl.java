package cz.vity.freerapid.plugins.services.bitroad;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class BitRoadServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "bitroad.net";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new BitRoadFileRunner();
    }
}