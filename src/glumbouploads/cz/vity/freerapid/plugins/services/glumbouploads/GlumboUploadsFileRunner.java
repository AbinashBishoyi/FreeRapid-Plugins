package cz.vity.freerapid.plugins.services.glumbouploads;

import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author tong2shot
 */
class GlumboUploadsFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(0, new GlumboUploadsFileNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected int getWaitTime() throws Exception {
        final Matcher matcher = getMatcherAgainstContent("id=\"countdown_str\".*?<span id=\".*?\">.*?(\\d+).*?</span|<div class=\"countdownnum\".*?>.*?(\\d+).*?</div");
        if (matcher.find()) {
            if (matcher.group(0).contains("countdownnum")) {
                return Integer.parseInt(matcher.group(2)) + 1;
            } else {
                return Integer.parseInt(matcher.group(1)) + 1;
            }
        }
        return 0;
    }
}