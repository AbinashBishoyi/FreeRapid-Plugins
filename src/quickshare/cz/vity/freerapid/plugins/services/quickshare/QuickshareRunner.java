package cz.vity.freerapid.plugins.services.quickshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
class QuickshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(QuickshareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (makeRequest(getMethod)) {
            if (getContentAsString().contains("var server")) {
                checkNameAndSize(getContentAsString());
                downloadTask.sleep(5);
                String server = getVar("server", getContentAsString());
                String id1 = getVar("ID1", getContentAsString());
                String id2 = getVar("ID2", getContentAsString());
                String id3 = getVar("ID3", getContentAsString());
                String id4 = getVar("ID4", getContentAsString());

                client.setReferer(fileURL);
                final String fn = server + "/download.php";
                logger.info("Found file URL " + fn);

                final PostMethod method = getPostMethod(fn);
                method.addParameter("ID1", id1);
                method.addParameter("ID2", id2);
                method.addParameter("ID3", id3);
                method.addParameter("ID4", id4);

                if (!tryDownloadAndSaveFile(method)) {
                    checkProblems();
                    logger.info(getContentAsString());
                    throw new ServiceConnectionProblemException("Všechny volné sloty jsou obsazeny nebo se z této IP již stahuje");
                }
            } else {
                checkProblems();
                logger.info(getContentAsString());
                throw new PluginImplementationException();
            }
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws Exception {
        if (!content.contains("QuickShare")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (content.contains("location.href='/chyba'")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Chyba! Soubor zřejmě neexistuje</b><br>"));
        }
        Matcher matcher = PlugUtils.matcher("zev: <strong>([^<]*)</strong>", content);
        if (matcher.find()) {
            String fn = matcher.group(matcher.groupCount());
            logger.info("File name " + fn);
            httpFile.setFileName(fn);
        }
        matcher = PlugUtils.matcher("([0-9.]+)</strong>( .B)", content);
        if (matcher.find()) {
            Long a = PlugUtils.getFileSizeFromString(matcher.group(1) + matcher.group(2));
            logger.info("File size " + a);
            httpFile.setFileSize(a);
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        }
    }

    private String getVar(String s, String contentAsString) throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("var " + s + " = '([^']*)'", contentAsString);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            throw new PluginImplementationException("Parameter " + s + " was not found");
    }

    private void checkProblems() throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException {
        String content = getContentAsString();
        if (content.contains("location.href='/chyba'")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Chyba! Soubor zřejmě neexistuje</b><br>"));
        }
        if (content.contains("obsazen na 100 %")) {
            throw new YouHaveToWaitException(String.format("<b>Chyba! Volné sloty obsazeny</b><br>"), 60);
        }
        if (content.contains("Pokud chcete stahovat bez")) {
            throw new ServiceConnectionProblemException(String.format("<b>Chyba! Momentálně je z Vaší IP adresy již jedno stahování</b><br>"));
        }

    }
}