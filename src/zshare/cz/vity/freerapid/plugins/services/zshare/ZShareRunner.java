package cz.vity.freerapid.plugins.services.zshare;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Prathap
 */
class ZShareRunner extends AbstractRunner {

    private final static Logger logger = Logger.getLogger(ZShareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        fileURL = processURL(fileURL);
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
    }

    private void checkNameandSize(String contentAsString) throws Exception {

        if (!contentAsString.contains("form name=\"form1\" method=\"post\" action=")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        if (contentAsString.contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }

        if (contentAsString.contains("File Name")) {
            Matcher matcher = PlugUtils.matcher("\\-\\s*([^-]+)</title>", contentAsString);
            if (matcher.find()) {
                String fn = matcher.group(1);
                fn = fn.replace(".html", "");//why did you removed it? it's OK with it

                fn = fn.replace("\"", "");
                fn = fn.replace(";", "");

                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else {
                logger.warning("File name was not found" + contentAsString);
            }
            matcher = PlugUtils.matcher("([0-9.]+.B)</font>", contentAsString);
            if (matcher.find()) {
                Long a = PlugUtils.getFileSizeFromString(matcher.group(1));
                logger.info("File size " + a);
                httpFile.setFileSize(a);
                httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
            } else {
                logger.warning("File size was not found" + contentAsString);
            }
        } else {
            logger.warning("File name was not found" + contentAsString);
        }
    }

    @Override
    public void run() throws Exception {
        super.run();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        fileURL = processURL(fileURL);
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            //no need to check again :-)
//            String contentAsString = getContentAsString();
//            checkNameandSize(contentAsString);
            Matcher matcher = getMatcherAgainstContent("form name=\"form1\" method=\"post\" action=");
            if (!matcher.find()) {
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
            String s = matcher.group(0);
            logger.info("Found Download button - " + s);
            client.setReferer(fileURL);
            final PostMethod postMethod = getPostMethod(fileURL);
            postMethod.addParameter("download", "1");
            if (makeRequest(postMethod)) {
                matcher = getMatcherAgainstContent("var link_enc=new Array(.*)");
                if (matcher.find()) {
                    String link = matcher.group(1);
                    link = this.processDownloadLink(link);
                    if (link != null) {//link found write down data to the specified file.
                        logger.info("Download URL: " + link);
                        //zShare allows users to download unlimited downloads (isn't it?)
                        final PostMethod method = getPostMethod(link);
                        if (!tryDownloadAndSaveFile(method)) {
                            checkProblems();
                            throw new IOException("File input stream is empty.");
                        }
                    }
                } else {
                    throw new PluginImplementationException("Cannot find requested page content");
                }
            } else {
                throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
            }
        } else {
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
        }
    }

    private String processURL(String mURL) throws Exception {
        String tURL = mURL;
        if (tURL.contains("www.zshare.net/audio")) {
            tURL = tURL.replaceFirst("www.zshare.net/audio", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/image")) {
            tURL = tURL.replaceFirst("www.zshare.net/image", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/audio")) {
            tURL = tURL.replaceFirst("www.zshare.net/audio", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/video")) {
            tURL = tURL.replaceFirst("www.zshare.net/video", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/downloadlink")) {
            tURL = tURL.replaceFirst("www.zshare.net/downloadlink", "www.zshare.net/download");

        } else if (tURL.contains("www.zshare.net/download/")) {
            tURL = tURL.replaceFirst("www.zshare.net/download/", "www.zshare.net/download/");
        } else {
            throw new InvalidURLOrServiceProblemException("Invalid URL");
        }
        return tURL;
    }

    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }
    }

    /**
     * This will find out the hidden URL stored in the .html page.
     * Generally ZShare displays the hidden links after 20 secs.
     * Using this we can find out the hidden link and can save that 20 secs of time.
     *
     * @param link
     * @return
     * @throws java.lang.Exception
     */
    private String processDownloadLink(String link) throws Exception {
        String tmp = "";
        if (link != null) {
            for (int i = 0; i < link.length(); i++) {
                String chr = link.charAt(i) + "";
                if (chr.equals(";")) {
                    break;
                }
                //replace all the special characters
                if (!chr.equals(",") && !chr.equals("'") && !chr.equals("(") &&
                        !chr.equals(")") /*|| !chr.equals("%")*/) {
                    tmp += link.charAt(i);
                }
            }
        } else {
            throw new InvalidURLOrServiceProblemException("Cant find download link");
        }
        return tmp;
    }
}