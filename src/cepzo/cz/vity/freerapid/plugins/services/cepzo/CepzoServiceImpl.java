package cz.vity.freerapid.plugins.services.cepzo;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingServiceImpl;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CepzoServiceImpl extends XFileSharingServiceImpl {
	
	@Override
    public String getServiceTitle() {
        return "Cepzo";
    }
	
    @Override
    public String getName() {
        return "cepzo.com";
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CepzoFileRunner();
    }
}