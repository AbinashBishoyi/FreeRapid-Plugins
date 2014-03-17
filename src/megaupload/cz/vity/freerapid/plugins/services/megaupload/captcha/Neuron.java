
package cz.vity.freerapid.plugins.services.megaupload.captcha;

/**
 * This class represents one neuron in the net
 *
 * @author JPEXS
 * Based on script by Shaun Friedle 2009
 * http://userscripts.org/scripts/show/38736
 */
public class Neuron {
    public double activation=0.0;
    public double bias=-1;
    public double threshold=0.0;
    public double weights[]=new double[0];

    public void feed(double[] inputs){
        this.activation = 0;
        for (int i = 0; i < inputs.length; i++)
        {
            this.activation += inputs[i] * this.weights[i];
         }
        this.activation += this.bias*this.threshold;
    }

    public double getOutput(){
        return 1/(1+Math.exp(-this.activation));
    }

}
