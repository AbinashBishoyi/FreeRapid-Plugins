package cz.vity.freerapid.plugins.services.leteckaposta;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author Vity
 */
public class LeteckaPostaServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "leteckaposta.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new LeteckaPostaFileRunner();
    }

}
