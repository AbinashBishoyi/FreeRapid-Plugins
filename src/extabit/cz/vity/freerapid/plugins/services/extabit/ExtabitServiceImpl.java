package cz.vity.freerapid.plugins.services.extabit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 *
 * @author Thumb
 */
public class ExtabitServiceImpl extends AbstractFileShareService {

    @Override
    public String getName() {
        return "extabit.com";
    }

    @Override
    public boolean supportsRunCheck() {
        return true;
    }

    @Override
    protected PluginRunner getPluginRunnerInstance() {
        return new ExtabitFileRunner();
    }

}