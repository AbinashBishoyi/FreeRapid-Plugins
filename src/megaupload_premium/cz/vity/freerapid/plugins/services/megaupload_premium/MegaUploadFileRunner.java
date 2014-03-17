package cz.vity.freerapid.plugins.services.megaupload_premium;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.PremiumAccount;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import cz.vity.freerapid.utilities.LogUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * Class which contains main code
 *
 * @author ntoskrnl
 */
class MegaUploadFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(MegaUploadFileRunner.class.getName());
    private String HTTP_SITE = "http://www.megaupload.com";
    private String LINK_TYPE = "single";
    private boolean badConfig = false;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        checkURL();
        addCookie(new Cookie(".megaupload.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));
        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            if (LINK_TYPE.equals("single")) checkNameAndSize();
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        checkURL();
        logger.info("Starting download in TASK " + fileURL);
        addCookie(new Cookie(".megaupload.com", "l", "en", "/", 86400, false));
        addCookie(new Cookie(".megaporn.com", "l", "en", "/", 86400, false));

        if (LINK_TYPE.equals("folder")) {
            stepFolder();
            return;
        }

        login();

        final GetMethod method = getGetMethod(fileURL);
        if (makeRedirectedRequest(method)) {
            checkProblems();
            checkNameAndSize();

            while (getContentAsString().contains("Please enter the password")) {
                final HttpMethod passwordMethod = getMethodBuilder().setReferer(fileURL).setAction(fileURL).setParameter("filepassword", getPassword()).toPostMethod();
                if (!makeRedirectedRequest(passwordMethod)) {
                    throw new ServiceConnectionProblemException("Error posting password");
                }
            }

            if (getContentAsString().contains("Enter this")) {
                if (makeRedirectedRequest(getGetMethod(HTTP_SITE + "/?c=account"))) {
                    if (getContentAsString().contains("<b>Regular</b>")) {
                        throw new NotRecoverableDownloadException("Account is not premium!");
                    }
                }
                throw new NotRecoverableDownloadException("Problem logging in, account not premium?");
            }

            final Matcher matcher = getMatcherAgainstContent("\"(http://www\\d+?\\.mega(?:upload|porn)\\.com/files/[^\"]+?)\"");
            if (!matcher.find()) throw new PluginImplementationException("Download link not found");

            final HttpMethod httpMethod = getMethodBuilder().setReferer(fileURL).setAction(matcher.group(1)).toGetMethod();
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

    private void checkNameAndSize() throws ErrorDuringDownloadingException {
        final Matcher name = getMatcherAgainstContent("Filename:</font> <font .+?>(.+?)</font><br>");
        if (!name.find()) throw new PluginImplementationException("File name not found");
        httpFile.setFileName(name.group(1));

        final Matcher size = getMatcherAgainstContent("font-size:13px;\">([0-9.]+ .B).?</font>");
        if (!size.find()) throw new PluginImplementationException("File size not found");
        httpFile.setFileSize(PlugUtils.getFileSizeFromString(size.group(1)));

        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String content = getContentAsString();
        if (content.contains("the link you have clicked is not available") || content.contains("<h1>Not Found</h1>")) {
            throw new URLNotAvailableAnymoreException("File not found");
        }
        if (content.contains("trying to access is temporarily unavailable")) {
            throw new ServiceConnectionProblemException("The file you are trying to access is temporarily unavailable");
        }
        if (content.contains("We have detected an elevated number of requests")) {
            throw new ServiceConnectionProblemException("We have detected an elevated number of requests");
        }
    }

    private void login() throws Exception {
        synchronized (MegaUploadFileRunner.class) {
            MegaUploadServiceImpl service = (MegaUploadServiceImpl) getPluginService();
            PremiumAccount pa = service.getConfig();
            if (!pa.isSet() || badConfig) {
                pa = service.showConfigDialog();
                if (pa == null || !pa.isSet()) {
                    throw new NotRecoverableDownloadException("No MegaUpload Premium account login information!");
                }
                badConfig = false;
            }

            final HttpMethod httpMethod = getMethodBuilder()
                    .setAction(HTTP_SITE + "/?c=login")
                    .setParameter("login", "1")
                    .setParameter("username", pa.getUsername())
                    .setParameter("password", pa.getPassword())
                    .toPostMethod();
            if (!makeRedirectedRequest(httpMethod))
                throw new ServiceConnectionProblemException("Error posting login info");

            if (getContentAsString().contains("Username and password do not match"))
                throw new NotRecoverableDownloadException("Invalid MegaUpload Premium account login information!");
        }
    }

    private void checkURL() {
        final String host = httpFile.getFileUrl().getHost();
        if (host.contains("megarotic") || host.contains("sexuploader") || host.contains("megaporn")) {
            HTTP_SITE = "http://www.megaporn.com";
            fileURL = fileURL.replace("megarotic.com", "megaporn.com").replace("sexuploader.com", "megaporn.com");
        }

        if (fileURL.contains("?f=")) {
            logger.info("Link type: folder");
            LINK_TYPE = "folder";
        }
    }

    private void stepFolder() throws Exception {
        if (!makeRedirectedRequest(getGetMethod(fileURL)))
            throw new ServiceConnectionProblemException();

        final String folderid = PlugUtils.getStringBetween(getContentAsString(), "folderid\",\"", "\");");
        final String xmlURL = HTTP_SITE + "/xml/folderfiles.php?folderid=" + folderid + "&uniq=1";
        final HttpMethod folderHttpMethod = getMethodBuilder().setReferer(fileURL).setAction(xmlURL).toGetMethod();

        if (makeRequest(folderHttpMethod)) {
            if (getContentAsString().contains("<FILES></FILES>"))
                throw new URLNotAvailableAnymoreException("No files in folder. Invalid link?");

            final Matcher matcher = getMatcherAgainstContent("url=\"(.+?)\"");
            int start = 0;
            final List<URI> uriList = new LinkedList<URI>();
            while (matcher.find(start)) {
                String link = matcher.group(1);
                try {
                    uriList.add(new URI(link));
                } catch (URISyntaxException e) {
                    LogUtils.processException(logger, e);
                }
                start = matcher.end();
            }
            getPluginService().getPluginContext().getQueueSupport().addLinksToQueue(httpFile, uriList);
        }
    }

    private String getPassword() throws Exception {
        MegauploadPasswordUI ps = new MegauploadPasswordUI();
        if (getDialogSupport().showOKCancelDialog(ps, "Secured file on MegaUpload")) {
            return (ps.getPassword());
        } else throw new NotRecoverableDownloadException("This file is secured with a password");
    }

}