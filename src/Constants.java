import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensemble des noms d'agents et de la liste des vendeurs utilisée par Jack.
 */

public interface Constants {
    AID JACK = new AID("JACK", AID.ISLOCALNAME);
    AID LILI = new AID("LILI", AID.ISLOCALNAME);
    AID LOLA = new AID("Lola", AID.ISLOCALNAME);
    AID JIM  = new AID("JIM", AID.ISLOCALNAME);
    AID LULU = new AID("LULU", AID.ISLOCALNAME);
    AID BUYER=JACK;
    String AGENT_INFO = " Agent :: ";
    List<AID> FSMSELLER = new ArrayList<>(List.of(LOLA, LILI, JIM, LULU));
}
