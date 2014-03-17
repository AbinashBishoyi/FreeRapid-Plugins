package cz.vity.freerapid.plugins.services.iskladka;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Ladislav Vitasek
 */
public class IskladkaServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "iskladka.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return false;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new IskladkaRunner();
    }

}