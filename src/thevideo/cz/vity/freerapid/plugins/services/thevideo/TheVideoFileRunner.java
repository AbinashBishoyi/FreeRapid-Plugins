package cz.vity.freerapid.plugins.services.thevideo;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class TheVideoFileRunner extends XFilePlayerRunner {

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        MethodBuilder builder = getXFSMethodBuilder(getContentAsString());
        final Matcher match = PlugUtils.matcher("name\\s*?:\\s*?'(.+?)',\\s*?value:\\s*?'(.+?)'\\s*?\\}\\).prependTo", getContentAsString());
        while (match.find()) {
            builder.setParameter(match.group(1), match.group(2));
        }
        return builder;
    }

}