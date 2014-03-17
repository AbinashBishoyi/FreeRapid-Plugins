
package cz.vity.freerapid.plugins.services.ifile.recaptcha;

import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Class for manipulating ReCaptcha
 * @author JPEXS
 *
 *
 * Example Usage:
 *  ReCaptcha r=new ReCaptcha("...api key...",client);
 *  String imageUrl=r.getImageURL();
 *  ...display image, user enters text...
 *  r.setRecognized(textUserEntered);
 *  ... create new method for sending form...
 *  Method m=r.modifyResponseMethod(getMethodBuilder()).toHttpMethod();
 *  ...
 */
public class ReCaptcha {
    private String challenge;
    private String publicKey;
    private HttpDownloadClient client;
    private String lastType="image";
    private String recognized="";

    /**
     * Constructor of ReCaptcha
     * @param publicKey Public API key for recaptcha (Included in every page which uses Recaptcha)
     * @param client Client to do requests with
     * @throws Exception When something goes wrong
     */
    public ReCaptcha(String publicKey,HttpDownloadClient client) throws Exception {
        HttpMethod httpMethod = new MethodBuilder(client).setAction("http://api.recaptcha.net/challenge?k="+publicKey).toGetMethod();
        client.makeRequest(httpMethod, true);
        challenge=PlugUtils.getStringBetween(client.getContentAsString(), "challenge : '", "',");
    }


    /**
     * Gets url of captcha image
     * @return String with url
     * @throws Exception When something goes wrong
     */
    public String getImageURL() throws Exception{
        if(lastType.equals("audio")){
            reloadImage();
        }
        return "http://api.recaptcha.net/image?c="+challenge;
    }

    /**
     * Gets url of audio captcha (audio/mpeg)
     * @return String with url
     * @throws Exception When something goes wrong
     */
    public String getAudioURL() throws Exception{
        if(lastType.equals("image")){
            reloadAudio();
        }
        return "http://api.recaptcha.net/image?c="+challenge;
    }

    private void reload(String type) throws Exception {
        HttpMethod httpMethod = new MethodBuilder(client).setAction("http://api.recaptcha.net/reload?c="+challenge+"&k="+publicKey+"&reason=r&type="+type+"&lang=en").toGetMethod();
        client.makeRequest(httpMethod, true);
        challenge=PlugUtils.getStringBetween(client.getContentAsString(), ".finish_reload ('", "', '");
    }

    /**
     * Generates new captcha image
     * @throws Exception When something goes wrong
     */
    public void reloadImage() throws Exception{
        reload("image");
        lastType="image";
    }

    /**
     * Generates new audio captcha
     * @throws Exception
     */
    public void reloadAudio() throws Exception{
        reload("audio");
        lastType="audio";
    }

    /**
     * Sets text/words which user read
     * @param recognized Text/words
     */
    public void setRecognized(String recognized){
        this.recognized=recognized;
    }

    /**
     * Modifies the method of sending form with captcha parameters
     * @param methodBuilder MethodBuilder to modify
     * @return Modified MethodBuilder
     */
    public MethodBuilder modifyResponseMethod(MethodBuilder methodBuilder){
        return methodBuilder.setParameter("recaptcha_challenge_field", challenge).setParameter("recaptcha_response_field", recognized);
    }

    /**
     * Gets response params for sending form separated with &
     * @return String with params
     */
    public String getResponseParams(){
        try {
            return "recaptcha_challenge_field=" + challenge + "&recaptcha_response_field=" + URLEncoder.encode(recognized, "UTF8");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }

}
