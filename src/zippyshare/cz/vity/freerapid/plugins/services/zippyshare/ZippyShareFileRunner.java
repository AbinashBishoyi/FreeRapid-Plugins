package cz.vity.freerapid.plugins.services.zippyshare;

import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.Utils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Vity+ntoskrnl
 * @since 0.82
 */
class ZippyShareFileRunner extends AbstractRunner {
    private static final Logger logger = Logger.getLogger(ZippyShareFileRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod httpMethod = getMethodBuilder().setAction(fileURL).toGetMethod();

        if (makeRedirectedRequest(httpMethod)) {
            checkProblems();
            checkNameAndSize();

            final String contentAsString = getContentAsString();
            String var = PlugUtils.getStringBetween(contentAsString, "var pong = '", "';");
            final String unescape = getStringBetween(contentAsString, "= unescape(", ");", 2);
            logger.info("unescape = " + unescape);
            var = applyReplace(var, unescape);

            httpMethod = getMethodBuilder().setReferer(fileURL).setAction(URLDecoder.decode(var, "UTF-8")).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Error starting download");
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private String applyReplace(String var, String content) {
        final Matcher matcher = PlugUtils.matcher(".replace\\((/.+?/g?), \"(.+?)\"", content);
        int start = 0;
        while (matcher.find(start)) {
            final String g1 = matcher.group(1);
            final String g2 = matcher.group(2);
            if (g1.endsWith("g")) {
                final String find = g1.substring(1, g1.length() - 2);
                var = var.replaceAll(Pattern.quote(find), g2);
            } else {
                final String find = g1.substring(1, g1.length() - 1);
                var = var.replaceFirst(Pattern.quote(find), g2);
            }
            start = matcher.end();
        }
        return var;
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("The requsted file does not exist on this server")
                || contentAsString.contains("<h1>HTTP Status")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
    }

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        PlugUtils.checkName(httpFile, contentAsString, "<strong>Name: </strong>", "<");
        PlugUtils.checkFileSize(httpFile, contentAsString, "Size: </strong>", "<");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }


    /**
     * Returns string between 2 other strings.
     *
     * @param content      searched content
     * @param stringBefore string before searched string  - without white space characters on the RIGHT side
     * @param stringAfter  string after searched string  - without white space characters on the LEFT side
     * @param count        what item in row is the right result
     * @return found string - result is trimmed
     * @throws cz.vity.freerapid.plugins.exceptions.PluginImplementationException
     *          No string between stringBefore and stringAfter
     * @since 0.84
     */
    private static String getStringBetween(final String content, final String stringBefore, final String stringAfter, int count) throws PluginImplementationException {
        if (count < 1) {
            throw new IllegalArgumentException("Finding count is less than 1");
        }
        final String before = Pattern.quote(Utils.rtrim(stringBefore));
        final String after = Pattern.quote(Utils.ltrim(stringAfter));
        final Matcher matcher = PlugUtils.matcher(before + "\\s*(.+?)\\s*" + after, content);
        int start = 0;
        for (int i = 1; i <= count; ++i) {
            if (matcher.find(start)) {
                if (i == count) {
                    return matcher.group(1);
                } else
                    start = matcher.end();
            } else {
                throw new PluginImplementationException(String.format("No string between '%s' and '%s' was found - attempt %s", stringBefore, stringAfter, count));
            }
        }
        throw new PluginImplementationException();
    }

}