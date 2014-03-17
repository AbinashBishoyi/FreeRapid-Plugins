package cz.vity.freerapid.plugins.services.pandamemo;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.services.xfilesharing.XFileSharingRunner;
import cz.vity.freerapid.plugins.services.xfilesharing.nameandsize.FileNameHandler;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import org.apache.commons.httpclient.HttpMethod;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author birchie
 */
class PandaMemoFileRunner extends XFileSharingRunner {

    @Override
    protected List<FileNameHandler> getFileNameHandlers() {
        final List<FileNameHandler> fileNameHandlers = super.getFileNameHandlers();
        fileNameHandlers.add(new PandaMemoNameHandler());
        return fileNameHandlers;
    }

    @Override
    protected MethodBuilder getXFSMethodBuilder() throws Exception {
        MethodBuilder methodBuilder;
        if (getContentAsString().contains("title=\"Click to Download\"")) {
            Matcher match = getMatcherAgainstContent("<a href=\"(.*)\"><img src=\"[^\"]*\" title=\"Click to Download\"");
            if (!match.find()) {
                throw new PluginImplementationException("Unable to locate download link");
            }
            if (match.group(1).contains("adf.ly")) {
                final String AdFlyUrl = match.group(1);
                final HttpMethod method = getGetMethod(AdFlyUrl);
                if (!makeRedirectedRequest(method))
                    throw new ServiceConnectionProblemException("Error getting free link");
                match = getMatcherAgainstContent("var url\\s*=\\s*'([^']*)';");
                if (!match.find())
                    throw new PluginImplementationException("Unable to get download link");
                downloadTask.sleep(5);
                methodBuilder = getMethodBuilder()
                        .setAction("http://adf.ly" + match.group(1))
                        .setReferer(AdFlyUrl);
            } else {
                methodBuilder = getMethodBuilder().setAction(match.group(1));
            }

        } else {
            methodBuilder = super.getXFSMethodBuilder();
        }
        return methodBuilder;
    }

}