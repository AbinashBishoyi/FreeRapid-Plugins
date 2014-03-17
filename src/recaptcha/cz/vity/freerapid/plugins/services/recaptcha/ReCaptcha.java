package cz.vity.freerapid.plugins.services.recaptcha;

import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;

/**
 * Class for manipulating ReCaptcha.<p/>
 * Example Usage:
 * <pre>
 *     ReCaptcha r = new ReCaptcha("...api key...", client);
 *     String imageUrl = r.getImageURL();
 *     ...display image, user enters text...
 *     r.setRecognized(textUserEntered);
 *     ... create new method for sending form...
 *     HttpMethod m = r.modifyResponseMethod(getMethodBuilder()).toHttpMethod();
 *     ...
 * </pre>
 *
 * @author JPEXS
 */
public class ReCaptcha {
    private static final String RECAPTCHA_URL = "http://www.google.com/recaptcha/api";

    private String challenge;
    private String publicKey;
    private HttpDownloadClient client;
    private String lastType = "image";
    private String recognized = "";

    /**
     * Constructor of ReCaptcha
     *
     * @param publicKey Public API key for ReCaptcha (Included in every page which uses ReCaptcha)
     * @param c         Client to do requests with
     * @throws Exception When something goes wrong
     */
    public ReCaptcha(String publicKey, HttpDownloadClient c) throws Exception {
        this.publicKey = publicKey;
        this.client = c;
        HttpMethod httpMethod = new MethodBuilder(client).setAction(RECAPTCHA_URL + "/challenge?k=" + publicKey).toGetMethod();
        client.makeRequest(httpMethod, true);
        Matcher matcher = PlugUtils.matcher("challenge\\s?:\\s?'(.+?)'", client.getContentAsString());
        if (!matcher.find()) throw new PluginImplementationException("ReCaptcha challenge not found");
        challenge = matcher.group(1);
    }


    /**
     * Gets url of captcha image
     *
     * @return String with url
     * @throws Exception When something goes wrong
     */
    public String getImageURL() throws Exception {
        if (lastType.equals("audio")) {
            reloadImage();
        }
        return RECAPTCHA_URL + "/image?c=" + challenge;
    }

    /**
     * Gets url of audio captcha (audio/mpeg)
     *
     * @return String with url
     * @throws Exception When something goes wrong
     */
    public String getAudioURL() throws Exception {
        if (lastType.equals("image")) {
            reloadAudio();
        }
        return RECAPTCHA_URL + "/image?c=" + challenge;
    }

    private void reload(String type) throws Exception {
        HttpMethod httpMethod = new MethodBuilder(client).setAction(RECAPTCHA_URL + "/reload?c=" + challenge + "&k=" + publicKey + "&reason=r&type=" + type + "&lang=en").toGetMethod();
        client.makeRequest(httpMethod, true);
        challenge = PlugUtils.getStringBetween(client.getContentAsString(), ".finish_reload ('", "', '");
    }

    /**
     * Generates new captcha image
     *
     * @throws Exception When something goes wrong
     */
    public void reloadImage() throws Exception {
        reload("image");
        lastType = "image";
    }

    /**
     * Generates new audio captcha
     *
     * @throws Exception When something goes wrong
     */
    public void reloadAudio() throws Exception {
        reload("audio");
        lastType = "audio";
    }

    /**
     * Sets text/words which user read
     *
     * @param recognized text/words
     */
    public void setRecognized(String recognized) {
        this.recognized = recognized;
    }

    /**
     * Modifies the method of sending form with captcha parameters
     *
     * @param methodBuilder MethodBuilder to modify
     * @return Modified MethodBuilder
     */
    public MethodBuilder modifyResponseMethod(MethodBuilder methodBuilder) {
        return methodBuilder.setParameter("recaptcha_challenge_field", challenge).setParameter("recaptcha_response_field", recognized);
    }

    /**
     * Gets response params for sending form separated with &
     *
     * @return String with params
     */
    public String getResponseParams() {
        try {
            return "recaptcha_challenge_field=" + challenge + "&recaptcha_response_field=" + URLEncoder.encode(recognized, "UTF8");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }

    public String getChallenge() {
        return challenge;
    }

    public String getRecognized() {
        return recognized;
    }

}
