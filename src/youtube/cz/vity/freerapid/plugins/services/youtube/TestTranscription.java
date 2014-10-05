package cz.vity.freerapid.plugins.services.youtube;

import cz.vity.freerapid.plugins.services.youtube.srt.SrtItem;
import cz.vity.freerapid.plugins.services.youtube.srt.TranscriptionBinding;
import jlibs.xml.sax.binding.BindingHandler;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.util.List;

class TestTranscription {
    public static void main(String[] args) throws Exception {
        String inputXML = "<?xml version=\"1.0\" encoding=\"utf-8\" ?><transcript><text start=\"0\" dur=\"1.96\">El Nissan Sunny.</text><text start=\"3\" dur=\"1.48\">No sé por dónde empezar</text><text start=\"4.52\" dur=\"3.96\">Quiero decir que hay coches más feos y hay coches peores de conducir, pero ese no es el asunto.</text><text start=\"8.52\" dur=\"2.92\">Es su insipidez lo que les da carácter.</text><text start=\"12.28\" dur=\"4.03\">Este, este es el peor crimen de todos… la Insipidez.</text><text start=\"16.4\" dur=\"11.75\">Dios probablemente nos dio el apio como muestra de insipidez, y entonces, sorprendentemente, la humanidad consiguió mejorarlo con esta… Cosa.</text><text start=\"28.2\" dur=\"4.95\">No sé si llamarle abominación, no tiene suficiente personalidad.</text><text start=\"33.63\" dur=\"3.49\">Olvida las características redentoras, no tiene ninguna característica.</text><text start=\"38.1\" dur=\"1.36\">Y hay otra cosa que no entiendo.</text><text start=\"39.86\" dur=\"0.29\">Dragsters, los coches de aceleración.</text><text start=\"40.63\" dur=\"1.94\">Quiero decir que, bueno, tiene un motor grande.</text><text start=\"43.41\" dur=\"5.57\">Bueno, de hecho es un motor de reactor, y sí, puede pasar de cero a 320 km en 3,8 segundos.</text><text start=\"49.79\" dur=\"3.28\">Pero le cuesta mejorar los 42 litros por 100 km.</text><text start=\"53.24\" dur=\"3.16\">Y cada vez que quieres montarte tienes que partirlo por la mitad.</text><text start=\"56.91\" dur=\"2.08\">Y no creo que tenga ni marcha atrás.</text><text start=\"59.34\" dur=\"3.7\">Pero es verdad que tiene una característica útil.</text><text start=\"95.07\" dur=\"1.36\">¿Qué le pareció?</text><text start=\"98.87\" dur=\"2.96\">¿Qué le pareció?</text><text start=\"102.11\" dur=\"0.56\">¡Fantástico!</text><text start=\"103.19\" dur=\"2.34\">Ahora, recuerde que tenemos un tema para esta noche.</text><text start=\"105.75\" dur=\"2.13\">Jeremy ha estado mirando los elegantes deportivos, coches pequeños.</text><text start=\"108.47\" dur=\"1.56\">Se siente como una emoción barata.</text><text start=\"110.48\" dur=\"3.48\">Ese dragster ha quemado unos 125 euros de combustible para hacer eso.</text><text start=\"114.4\" dur=\"3.82\">¡Y esa es la mejor forma de gastar 125 euros!</text></transcript>";
        BindingHandler handler = new BindingHandler(TranscriptionBinding.class);
        @SuppressWarnings("unchecked")
        final List<SrtItem> list = (List<SrtItem>) handler.parse(new InputSource(new ByteArrayInputStream(inputXML.getBytes("UTF-8"))));
        for (SrtItem item : list) {
            System.out.print(item);
        }
    }

}
