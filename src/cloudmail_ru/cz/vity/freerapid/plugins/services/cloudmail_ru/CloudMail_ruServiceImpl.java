package cz.vity.freerapid.plugins.services.cloudmail_ru;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author tong2shot
 */
public class CloudMail_ruServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "cloudmail_ru.ru";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new CloudMail_ruFileRunner();
    }

}