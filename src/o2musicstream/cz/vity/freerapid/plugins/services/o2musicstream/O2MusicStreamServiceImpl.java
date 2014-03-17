package cz.vity.freerapid.plugins.services.o2musicstream;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Kajda
 */
public class O2MusicStreamServiceImpl extends AbstractFileShareService {
    private final static String SERVICE_NAME = "o2musicstream.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new O2MusicStreamFileRunner();
    }
}
