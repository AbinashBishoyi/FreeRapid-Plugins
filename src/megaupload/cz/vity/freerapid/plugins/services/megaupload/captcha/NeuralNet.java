package cz.vity.freerapid.plugins.services.megaupload.captcha;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This Class represents Neural network
 *
 * @author JPEXS
 *
 * Based on script by Shaun Friedle 2009
 * http://userscripts.org/scripts/show/38736
 */
public class NeuralNet {

    public Neuron[] h_layer = new Neuron[0];
    public Neuron[] o_layer = new Neuron[0];

    public NeuralNet(String fileName) {
        try {
            InputStream is = this.getClass().getResourceAsStream("/" + fileName);
            DataInputStream dis = new DataInputStream(is);
            byte header[] = new byte[2];
            dis.read(header);
            if (!"NN".equals(new String(header))) {
                throw new IOException("Invalid neural network data");
            }
            h_layer = readLayer(dis);
            o_layer = readLayer(dis);
            dis.close();
            is.close();
            /*SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(false);
            SAXParser saxLevel1 = spf.newSAXParser();
            XMLReader parser = saxLevel1.getXMLReader();
            MyHandler handler = new MyHandler();
            parser.setContentHandler(handler);
            parser.parse(new InputSource(is));*/
        } catch (Exception ex) {
            
        }
    }

    public void feed(double[] inputs) {
        double[] h_outputs = new double[this.h_layer.length];

        for (int i = 0; i < this.h_layer.length; i++) {
            this.h_layer[i].feed(inputs);
            h_outputs[i] = this.h_layer[i].getOutput();
        }

        for (int i = 0; i < this.o_layer.length; i++) {
            this.o_layer[i].feed(h_outputs);
        }

    }

    public double[] getOutput() {
        double output[] = new double[this.o_layer.length];

        for (int i = 0; i < this.o_layer.length; i++) {
            output[i] = this.o_layer[i].getOutput();
        }

        return output;

    }

    public double[] test(double[] inputs) {
        this.feed(inputs);
        return this.getOutput();
    }

    /*
    //Reading from XML:

    private class MyHandler extends DefaultHandler {

    private String currentLayerType = null;
    private ArrayList<Double> weights = new ArrayList<Double>();
    private ArrayList<Neuron> neurons = new ArrayList<Neuron>();
    private boolean inWeight = false;
    private double threshold = 0;
    private String temp = "";

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if (qName.equals("layer")) {
    currentLayerType = attributes.getValue("type");
    }

    if (qName.equals("neuron")) {
    threshold = Double.parseDouble(attributes.getValue("threshold"));
    }

    if (qName.equals("weight")) {
    inWeight = true;
    }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
    if (inWeight) {
    String s = new String(ch, start, length);
    temp += s;
    }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
    if (qName.equals("weight")) {
    weights.add(Double.parseDouble(temp));
    inWeight = false;
    temp = "";
    }
    if (qName.equals("neuron")) {
    Neuron n = new Neuron();
    n.threshold = threshold;
    n.weights = new double[weights.size()];
    for (int i = 0; i < weights.size(); i++) {
    n.weights[i] = weights.get(i).doubleValue();
    }
    neurons.add(n);
    weights.clear();
    }

    if (qName.equals("layer")) {
    if (currentLayerType.equals("h")) {
    h_layer = new Neuron[neurons.size()];
    for (int i = 0; i < neurons.size(); i++) {
    h_layer[i] = neurons.get(i);
    }
    }
    if (currentLayerType.equals("o")) {
    o_layer = new Neuron[neurons.size()];
    for (int i = 0; i < neurons.size(); i++) {
    o_layer[i] = neurons.get(i);
    }
    }


    neurons.clear();
    }

    }
    }
     */
    private Neuron[] readLayer(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        Neuron[] layer = new Neuron[size];
        for (int i = 0; i < layer.length; i++) {
            layer[i] = new Neuron();
            layer[i].threshold = dis.readDouble();
            size = dis.readInt();
            layer[i].weights = new double[size];
            for (int j = 0; j < layer[i].weights.length; j++) {
                layer[i].weights[j] = dis.readDouble();
            }
        }
        return layer;
    }

    private void writeLayer(DataOutputStream dos, Neuron[] layer) throws IOException {
        dos.writeInt(layer.length);
        for (int i = 0; i < layer.length; i++) {
            dos.writeDouble(layer[i].threshold);
            dos.writeInt(layer[i].weights.length);
            for (int j = 0; j < layer[i].weights.length; j++) {
                dos.writeDouble(layer[i].weights[j]);
            }
        }
    }

    public void saveNet(String fileName) {
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileName));
            dos.write("NN".getBytes());
            writeLayer(dos, h_layer);
            writeLayer(dos, o_layer);
        } catch (IOException ex) {
            System.out.println("Error while saving net");
        }
    }
}
