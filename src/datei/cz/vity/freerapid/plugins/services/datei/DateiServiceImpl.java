package cz.vity.freerapid.plugins.services.datei;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author ntoskrnl
 */
public class DateiServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "datei.to";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new DateiFileRunner();
    }

}