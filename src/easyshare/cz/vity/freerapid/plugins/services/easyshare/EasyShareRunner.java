package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.HttpFile;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import cz.vity.freerapid.plugins.webclient.PlugUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpMethodParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek, Ludek Zika
 *         <p/>
 *         History: detekce ve strance The file you are trying to access is temporarily unavailable.
 */

class EasyShareRunner {
    private final static Logger logger = Logger.getLogger(EasyShareRunner.class.getName());
    private HttpDownloadClient client;
    private HttpFileDownloader downloader;
    private String httpSite;
    private HttpFile httpFile;
    private String baseURL;

    public void run(HttpFileDownloader downloader) throws Exception {
        this.downloader = downloader;
        httpFile = downloader.getDownloadFile();
        client = downloader.getClient();
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        final String fileURL = httpFile.getFileUrl().toString();
        baseURL = fileURL;
        httpSite = fileURL.substring(0, fileURL.lastIndexOf('/'));

        logger.info("Starting download in TASK " + fileURL);

        GetMethod getMethod = client.getGetMethod(fileURL);
        getMethod.setFollowRedirects(true);
        if (client.makeRequest(getMethod) == HttpStatus.SC_OK) {

           Matcher matcher = Pattern.compile("Download ([^,]+), upload", Pattern.MULTILINE).matcher(client.getContentAsString());
            if (matcher.find()) {
                final String fn = matcher.group(1);
                logger.info("File name " + fn);
                httpFile.setFileName(fn);
            } else logger.warning("File name was not found" + client.getContentAsString());
                //TODO
                if (client.getContentAsString().contains("trying to access is temporarily unavailable"))
                    throw new YouHaveToWaitException("The file you are trying to access is temporarily unavailable.", 2 * 60);
        if (client.getContentAsString().contains("w=")) {
          matcher = PlugUtils.matcher("w='([0-9]*?)';", client.getContentAsString());
            if (matcher.find()) {
                downloader.sleep(Integer.parseInt(matcher.group(1)));
            } else {
                logger.warning(client.getContentAsString());
                throw new PluginImplementationException("Plugin implementation problem");
            }


            matcher = PlugUtils.matcher("u='(/.*?)';", client.getContentAsString());
            if (matcher.find()) {
                final String link = matcher.group(1);
                getMethod = client.getGetMethod(httpSite + link);
                if (client.makeRequest(getMethod) != HttpStatus.SC_OK) {
                    logger.warning(client.getContentAsString());
                    throw new ServiceConnectionProblemException("Unknown error");
                }
            }

           }  else if (!client.getContentAsString().contains("Please enter")) {
                logger.warning(client.getContentAsString());
                throw new PluginImplementationException("Plugin implementation problem");
            }
           matcher = PlugUtils.matcher("File size: ([0-9.]+( )?.B).?</div>", client.getContentAsString());
            if (matcher.find()) {
                logger.info("File size " + matcher.group(1));
                httpFile.setFileSize(PlugUtils.getFileSizeFromString(matcher.group(1)));
            }
            while (client.getContentAsString().contains("Please enter")) {
                logger.info(client.getContentAsString());
               if(stepCaptcha(client.getContentAsString())) break;
            }


        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }


    private void checkProblems() throws ServiceConnectionProblemException, URLNotAvailableAnymoreException {
        Matcher matcher;
        matcher = Pattern.compile("Download limit exceeded", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {

            throw new ServiceConnectionProblemException(String.format("Download limit exceeded."));
        }
        matcher = Pattern.compile("All download slots", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {

            throw new ServiceConnectionProblemException(String.format("No free slot for your country."));
        }
        matcher = Pattern.compile("Unfortunately, the link you have clicked is not available", Pattern.MULTILINE).matcher(client.getContentAsString());
        if (matcher.find()) {
            throw new URLNotAvailableAnymoreException(String.format("<b>The file is not available</b><br>"));

        }

    }

    private boolean stepCaptcha(final String contentAsString) throws Exception {
        if (contentAsString.contains("Please enter")) {

            final Matcher m = PlugUtils.matcher("type=\"hidden\" name=\"id\" value=\"(.*?)\"", contentAsString);
            String id;
            if (m.find()) {
                id = m.group(1);
                logger.info("ESRunner - file id is " + id);
            } else throw new PluginImplementationException("ID was not found");

            Matcher matcher = PlugUtils.matcher("src=\"(/kaptchacluster[^\"]*)\"", contentAsString);
            if (matcher.find()) {
                String s = matcher.group(1);
                logger.info(httpSite + s);
                    client.setReferer(baseURL);
                String captcha = downloader.getCaptcha(httpSite + s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    matcher = PlugUtils.matcher("<form action=\"([^\"]*file_contents[^\"]*)\"", contentAsString);
                      if (matcher.find()) {
                       s = matcher.group(1);
                        logger.info( s);

                    final PostMethod method = client.getPostMethod(s);
                  client.getHTTPClient().getParams().setParameter(HttpMethodParams.SINGLE_COOKIE_HEADER, true);
                    method.addParameter("id", id);
                    method.addParameter("captcha", captcha);

                    try {
                        final InputStream inputStream = client.makeFinalRequestForFile(method, httpFile);
                        if (inputStream != null) {
                            downloader.saveToFile(inputStream);
                            return true;
                        } else {
                            checkProblems();
                            if (client.getContentAsString().contains("Please enter"))
                                return false;
                            logger.warning(client.getContentAsString());
                            throw new IOException("File input stream is empty.");
                        }

                    } finally {
                        method.abort();
                        method.releaseConnection();
                    }
                    } else {
                        logger.warning(client.getContentAsString());
                          throw new PluginImplementationException("Action was not found");
                          }
                 }

            } else throw new PluginImplementationException("Captcha picture was not found");
        }
        return false;
    }

}