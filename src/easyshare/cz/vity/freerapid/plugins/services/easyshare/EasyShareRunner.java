package cz.vity.freerapid.plugins.services.easyshare;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */

class EasyShareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(EasyShareRunner.class.getName());
    private String httpSite;
    private String baseURL;

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        addCookie(new Cookie(".easy-share.com", "language", "en", "/", null, false));
        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();
        if (makeRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    private void checkNameAndSize(String contentAsString) throws Exception {

        if (!contentAsString.contains("Share")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }

        checkProblems();

        PlugUtils.checkName(httpFile, contentAsString, "requesting ", " (");
        PlugUtils.checkFileSize(httpFile, contentAsString, " (", ")</p>");

    }

    @Override
    public void run() throws Exception {
        super.run();
        addCookie(new Cookie(".easy-share.com", "language", "en", "/", null, false));
        client.getHTTPClient().getParams().setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS, true);
        baseURL = fileURL;
        httpSite = fileURL.substring(0, fileURL.indexOf('/', 10));
        logger.info("httpSite set to " + httpSite);
        logger.info("Starting download in TASK " + fileURL);
        HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toHttpMethod();

        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
            if (!(getContentAsString().contains("kaptchacluster") || 
            		  getContentAsString().contains("<th class=\"last\">") || 
            		  getContentAsString().contains("u='") || 
            		  getContentAsString().contains("class=\"captcha1\""))) {
                checkProblems();
                logger.warning(getContentAsString());
                throw new PluginImplementationException("Plugin implementation problem");

            }
            if (getContentAsString().contains("<th class=\"last\">")) {

                if (getContentAsString().contains(" w=")) {
                    Matcher matcher = getMatcherAgainstContent("w='([0-9]+?)';");
                    if (matcher.find()) {
                        downloadTask.sleep(Integer.parseInt(matcher.group(1)));
                    } else {
                        logger.warning(getContentAsString());
                        throw new PluginImplementationException("Plugin implementation problem");
                    }
                }

                getMethod = getMethodBuilder().setAction(skipEnterPageUrl()).toHttpMethod();
                if (!makeRequest(getMethod)) {
                    logger.warning(getContentAsString());
                    throw new ServiceConnectionProblemException("Unknown error");
                }
            }
            if (!getContentAsString().contains("kaptchacluster") && getContentAsString().contains("Download the file")) {
                stepNoCaptcha(getContentAsString());
            } else while (true) {
                if (!(getContentAsString().contains("kaptchacluster") || 
                		  getContentAsString().contains("u='") ||
                		  getContentAsString().contains("class=\"captcha1\""))) {
                    checkProblems();
                    logger.warning(getContentAsString());
                    throw new PluginImplementationException("Plugin implementation problem");
                }

                if (stepCaptcha(getContentAsString())) break;
            }


        } else
            throw new ServiceConnectionProblemException("Problem with a connection to service.\nCannot find requested page content");
    }

    private String skipEnterPageUrl() throws PluginImplementationException {
        Matcher matcher = PlugUtils.matcher("com/([0-9]+)", fileURL);
        if (matcher.find()) {
            return "http://www.easy-share.com/c/" + matcher.group(1);
        } else {
            logger.warning("Cannot get number from url " + fileURL);
            throw new PluginImplementationException("Plugin implementation problem");
        }
    }


    private void checkProblems() throws Exception {
        if (getContentAsString().contains("File not found")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>File not found</b><br>"));
        }
        if (getContentAsString().contains("Requested file is deleted")) {
            throw new URLNotAvailableAnymoreException(String.format("<b>Requested file is deleted</b><br>"));
        }
        if (getContentAsString().contains("Error 404: Page not found")) {
            throw new InvalidURLOrServiceProblemException(String.format("<b>Error 404: Page not found</b><br>"));
        }
        if (getContentAsString().contains("You have downloaded ")) {
            throw new YouHaveToWaitException(String.format("<b>You have downloaded to much during last hour. You have to wait</b><br>"), 20 * 60);
        }

    }

    private boolean stepCaptcha(String contentAsString) throws Exception {
        if (contentAsString.contains("kaptchacluster")) {
            try {
                String s = httpSite + getMethodBuilder(contentAsString).setReferer(baseURL).
                        setActionFromImgSrcWhereTagContains("kaptchacluster").getAction();
                logger.info("Captcha image url: " + s);
                String captcha = getCaptchaSupport().getCaptcha(s);

                if (captcha == null) {
                    throw new CaptchaEntryInputMismatchException();
                } else {
                    logger.info("Entered captcha: " + captcha);
                    return finalAction(contentAsString, captcha);
                }

            } catch (BuildMethodException e) {
                checkProblems();
                logger.warning(e.getMessage());
                throw new PluginImplementationException("Captcha picture was not found");
            }

        }
        else if(contentAsString.contains("class=\"captcha1\"")) {
        	HttpMethod finalMethod=getMethodBuilder()
        		.setActionFromFormWhereTagContains("class=\"captcha1\"", true)
        		.toHttpMethod();
          if (tryDownloadAndSaveFile(finalMethod)) return true;
          else {
              checkProblems();
              if (getContentAsString().contains("kaptchacluster"))
                  return false;
              logger.warning(getContentAsString());
              throw new IOException("File input stream is empty.");
          }
        } else if (contentAsString.contains("u='")) {

          loadCaptchaPage();
      }
        return false;
    }


    private boolean stepNoCaptcha(final String contentAsString) throws Exception {
        if (contentAsString.contains("Download the file")) {
            logger.info("Captcha not needed ");


            return finalAction(contentAsString, "");
        }


        return false;
    }

    private boolean finalAction(String contentAsString, String captcha) throws Exception {

        MethodBuilder mb = getMethodBuilder(contentAsString).
                setActionFromFormWhereActionContains("file_contents", true);
        if (!captcha.equals("")) mb.setParameter("captcha", captcha);
        final HttpMethod method = mb.toPostMethod();
        if (tryDownloadAndSaveFile(method)) return true;
        else {
            checkProblems();
            if (getContentAsString().contains("kaptchacluster"))
                return false;
            logger.warning(getContentAsString());
            throw new IOException("File input stream is empty.");
        }

    }

    private void loadCaptchaPage() throws Exception {
        if (getContentAsString().contains("u='")) {
            logger.info("Loading subpage with captcha ");
            HttpMethod getMethod = getMethodBuilder().setBaseURL(httpSite).setReferer(baseURL).setActionFromTextBetween("u='", "';").toGetMethod();
            if (!makeRedirectedRequest(getMethod)) {
                logger.warning(getContentAsString());
                throw new ServiceConnectionProblemException("Unknown error");
            }
            if (getContentAsString().contains("w=")) {
                Matcher matcher = getMatcherAgainstContent("w='([0-9]+?)';");
                if (matcher.find()) {
                    int timeToWait = Integer.parseInt(matcher.group(1));
                    if (timeToWait < 60) downloadTask.sleep(timeToWait);
                    else throw new YouHaveToWaitException("Wait", Integer.parseInt(matcher.group(1)));
                }
            }

        }


    }


}