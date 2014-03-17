package cz.vity.freerapid.plugins.services.radiouolcom;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class RadioUolComServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "radio.uol.com.br";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new RadioUolComFileRunner();
    }

}