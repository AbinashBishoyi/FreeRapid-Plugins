package cz.vity.freerapid.plugins.services.socadvnet;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class ProtectedSocadvnetServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "protected.socadvnet.com";

	@Override
	protected PluginRunner getPluginRunnerInstance() {
		return new ProtectedSocadvnetRunner();
	}

	@Override
	public String getName() {
		return SERVICE_NAME;
	}

	@Override
    public boolean supportsRunCheck() {
        return false;
    }

}
