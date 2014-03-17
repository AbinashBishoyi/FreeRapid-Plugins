package cz.vity.freerapid.plugins.services.edisk;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class EdiskServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "edisk.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new EdiskRunner();
    }


}