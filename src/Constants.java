import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensemble des noms d'agents et de la liste des vendeurs utilisée par Jack.
 */

public interface Constants {
    String JACK = "Jack";
    String LILI = "Lili";
    String LOLA = "Lola";
    String JIM  = "Jim";
    String LULU = "Lulu";
    String AGENT_INFO = " Agent :: ";
    List<AID> FSMSELLER = new ArrayList<>(List.of(
            new AID(LOLA, AID.ISLOCALNAME),
            new AID(LILI, AID.ISLOCALNAME),
            new AID(JIM, AID.ISLOCALNAME),
            new AID(LULU, AID.ISLOCALNAME)
    ));
}
