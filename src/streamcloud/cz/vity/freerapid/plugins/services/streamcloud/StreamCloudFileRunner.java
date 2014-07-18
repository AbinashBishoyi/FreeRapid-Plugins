package cz.vity.freerapid.plugins.services.streamcloud;

import cz.vity.freerapid.plugins.services.xfileplayer.XFilePlayerRunner;

import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class StreamCloudFileRunner extends XFilePlayerRunner {

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("count.*?=.*?(\\d+);");
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) + 1;
        }
        return 0;
    }
}