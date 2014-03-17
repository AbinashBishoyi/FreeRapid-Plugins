package cz.vity.freerapid.plugins.services.cramit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class CramitServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "cramit.in";

	@Override
	protected PluginRunner getPluginRunnerInstance() {
		return new CramitRunner();
	}	

	@Override
	public String getName() {
		return SERVICE_NAME;
	}

	@Override
    public boolean supportsRunCheck() {
        return true;
    }

}
