package cz.vity.freerapid.plugins.services.streamcz;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;


/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class StreamCzServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "stream.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new StreamCzRunner();
    }


}