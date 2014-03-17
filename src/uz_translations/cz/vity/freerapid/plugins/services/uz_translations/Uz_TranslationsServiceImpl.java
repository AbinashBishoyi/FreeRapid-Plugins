package cz.vity.freerapid.plugins.services.uz_translations;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author birchie
 */
public class Uz_TranslationsServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "uz-translations.uz";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new Uz_TranslationsFileRunner();
    }

}