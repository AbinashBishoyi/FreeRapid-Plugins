package cz.vity.freerapid.plugins.services.alfafiles;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;

import java.util.logging.Logger;
import java.util.regex.Matcher;


/**
 * Class which contains main code
 *
 * @author RickCL
 */
class AlfaFilesFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(AlfaFilesFileRunner.class.getName());

    private String urlCode;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = checkFileURL(fileURL);
        logger.info("New URL: " + fileURL);
    }

    private String checkFileURL(final String fileURL) throws ErrorDuringDownloadingException {
        final Matcher matcher = PlugUtils.matcher("http://(?:www\\.)?alfa-files\\.com/(?:download/free/)?([a-z0-9]+)(?:\\.html?)?", fileURL);
        if (!matcher.find()) {
            throw new PluginImplementationException("Error parsing download link");
        }
        urlCode = matcher.group(1);
        return "http://www.turbobit.net/" + urlCode + ".html";
    }

    @Override
    public void run() throws Exception {
        super.run();
        fileURL = checkFileURL(fileURL);

        synchronized (getPluginService().getPluginContext().getQueueSupport()) {
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, fileURL);
        }
    }

}