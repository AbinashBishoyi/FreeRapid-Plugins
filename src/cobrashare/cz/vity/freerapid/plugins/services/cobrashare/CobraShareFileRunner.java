package cz.vity.freerapid.plugins.services.cobrashare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek
 */
class CobraShareFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(CobraShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            final Matcher matcher = getMatcherAgainstContent("url=(http.+?)\"");
            if (matcher.find()) {
                client.setReferer(fileURL);
                getMethod = getGetMethod(matcher.group(1));
                if (makeRedirectedRequest(getMethod)) {
                    checkNameAndSize(getContentAsString());
                } else {
                    logger.warning("Cannot find redirection page");
                    throw new PluginImplementationException();
                }
            } else throw new PluginImplementationException();
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException, IOException {
        checkProblems();
        Matcher matcher = PlugUtils.matcher("File name :&nbsp;<\\/td>\\s*<td class=\"data\">(.+?)<", content);
        if (matcher.find()) {

            final String fileName = matcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);
            //: <strong>204800</strong>KB<br>
            matcher = PlugUtils.matcher("Size :&nbsp;<\\/td>\\s*<td class=\"data\">(.+?)<", content);
            if (matcher.find()) {
                final String stringSize = matcher.group(1);
                logger.info("String size:" + stringSize);
                final String removedEntities = PlugUtils.unescapeHtml(stringSize).replaceAll("(\\s|\u00A0)*", "");
                logger.info("Entities:" + removedEntities);
                final long size = PlugUtils.getFileSizeFromString(removedEntities);
                httpFile.setFileSize(size);
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else {
                checkProblems();
                logger.warning("File size was not found\n:");
                throw new PluginImplementationException();
            }
        } else {
            checkProblems();
            logger.warning("File name was not found");
            throw new PluginImplementationException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        runCheck();

        final Matcher matcher = getMatcherAgainstContent("form action=\"(http.+?)\"");
        if (matcher.find()) {
            client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(matcher.group(1));
            PlugUtils.addParameters(postMethod, getContentAsString(), new String[]{"id"});

            if (!tryDownloadAndSaveFile(postMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new IOException("File input stream is empty.");
            }
        } else throw new PluginImplementationException("Download URL not found");
    }

    private void checkProblems() throws ErrorDuringDownloadingException, IOException {
        String contentAsString = getContentAsString();
        if (contentAsString.contains("open(\"http://www.cobrashare.sk/cantDownload.php")) {
            final GetMethod getMethod = getGetMethod("http://www.cobrashare.sk/cantDownload.php");
            if (!makeRedirectedRequest(getMethod)) {
                throw new PluginImplementationException();
            } else contentAsString = getContentAsString();
        }

        if (contentAsString.contains("sa na serveri nenach")) {
            throw new URLNotAvailableAnymoreException("Požadovaný súbor sa na serveri nenachádza");
        }

        if (contentAsString.contains("prebieha prenos")) {
            throw new ServiceConnectionProblemException("Práve prebieha prenos (download) z vašej IP adresy");
        }

//        if (PlugUtils.find("te stiahnu. zadarmo", contentAsString)) {
//            throw new ServiceConnectionProblemException("Záaž 100% - nemôžte stiahnu zadarmo");
//        }

//        if (contentAsString.contains("Neplatny download")) {
//            throw new YouHaveToWaitException("Neplatny download", 2);
//        }
    }

}
