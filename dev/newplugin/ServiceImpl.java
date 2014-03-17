package cz.vity.freerapid.plugins.services.

#shortsmall#;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.interfaces.PluginRunner;

/**
 * Class that provides basic info about plugin
 * @author #author#
 */
public class#fullWithoutDot#ServiceImpl extends AbstractFileShareService{

@Override
public String getName(){
        return"#fulllower#";
}

@Override
public int getMaxDownloadsFromOneIP(){
        //TODO don't forget to update this value, in plugin.xml don't forget to update this value too
        return 1;
}

@Override
public boolean supportsRunCheck(){
        return true;//ok
}

@Override
protected PluginRunner getPluginRunnerInstance(){
        return new #fullWithoutDot#FileRunner();
}

        }