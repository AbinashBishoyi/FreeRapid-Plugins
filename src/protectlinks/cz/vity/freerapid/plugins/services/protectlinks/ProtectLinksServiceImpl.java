package cz.vity.freerapid.plugins.services.protectlinks;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * @author RickCL
 */
public class ProtectLinksServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "protectlinks.com";

	@Override
	protected PluginRunner getPluginRunnerInstance() {
		return new ProtectLinksRunner();
	}

	@Override
	public int getMaxDownloadsFromOneIP() {
		return 1;
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
