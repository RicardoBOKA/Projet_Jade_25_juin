import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

/**
 * Agent acheteur (Jack) participant au protocole d'enchère à un tour.
 * Il envoie un CFP puis collecte les réponses avant de choisir la meilleure
 * proposition.
 * La FSM comporte trois états : CALLING, WAITING et DECIDING.
 */
public class BuyerAgent extends Agent implements Constants {

    private final Map<AID, String> proposals = new HashMap<>();
    private final Set<AID> refusals = new HashSet<>();

    private static final String CALLING = "CALLING";
    private static final String WAITING = "WAITING";
    private static final String DECIDING = "DECIDING";
    private static final String END = "END";

    /**
     * Finite state machine controlling the buyer protocol.
     */
    private class BuyerBehaviour extends FSMBehaviour {
        BuyerBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void onStart() {
            registerFirstState(new CFPBehaviour(), CALLING);
            registerState(new HandleRepliesBehaviour(), WAITING);
            registerState(new DecideBehaviour(), DECIDING);
            registerLastState(new EndBehaviour(), END);

            registerTransition(CALLING, WAITING, 0);
            registerTransition(WAITING, DECIDING, 0);
            registerDefaultTransition(DECIDING, END);
            //registerTransition(DECIDING, END, 0);
        }

        @Override
        public int onEnd() {
            return 0;
            //myAgent.doDelete();
            //return super.onEnd();
        }
    }

    /**
     * Initialise l'agent en ajoutant la FSM qui gère le protocole
     * d'enchère.
     */
    @Override
    protected void setup() {
        addBehaviour(new BuyerBehaviour(this));
    }

    /**
     * Premier état de l'acheteur : diffusion du CFP à tous les vendeurs.
     */
    private class CFPBehaviour extends OneShotBehaviour {
        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state: " + CALLING);
        }

        @Override
        public void action() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID aid : FSMSELLER) {
                cfp.addReceiver(aid);
            }
            cfp.setContent("Looking for a bike");
            send(cfp);
            List<String> names = new ArrayList<>();
            for (AID aid : FSMSELLER) {
                names.add(aid.getLocalName());
            }
            System.out.println(getLocalName() + " :: sending " +
                    ACLMessage.getPerformative(cfp.getPerformative()) +
                    " to " + String.join(", ", names) +
                    " with content: " + cfp.getContent());
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(getLocalName() + " :: exiting state: " + CALLING + " with exit code: " + code);
            return code;
        }
    }

    /**
     * Collecte toutes les réponses des vendeurs.
     * Cet état reste actif jusqu'à avoir reçu autant de messages
     * qu'il y a de vendeurs annoncés dans {@link Constants#FSMSELLER}.
     */
    private class HandleRepliesBehaviour extends SimpleBehaviour {
        private int replies;
        private MessageTemplate template;

        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state: " + WAITING);
            template = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(template);
            if (msg != null) {
                if (FSMSELLER.contains(msg.getSender())) {
                    replies++;
                    System.out.println(getLocalName() + " :: received " +
                            ACLMessage.getPerformative(msg.getPerformative()) +
                            " from " + msg.getSender().getLocalName() +
                            " with content: " + msg.getContent());
                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        proposals.put(msg.getSender(), msg.getContent());
                    } else {
                        refusals.add(msg.getSender());
                        addBehaviour(new RefuseBehaviour(msg.getSender(), msg.getContent()));
                    }
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return replies >= FSMSELLER.size();
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(getLocalName() + " :: exiting state: " + WAITING + " with exit code: " + code);
            return code;
        }
    }

    /**
     * Affiche le motif d'un refus reçu d'un vendeur.
     */
    private class RefuseBehaviour extends OneShotBehaviour {
        private final AID seller;
        private final String reason;
        public RefuseBehaviour(AID seller, String reason) {
            this.seller = seller;
            this.reason = reason;
        }
        @Override
        public void action() {
            System.out.println(getLocalName() + " :: seller " + seller.getLocalName() +
                    " refused because " + reason);
        }
    }

    /**
     * Dernier état : sélection du vendeur proposant le prix le plus bas
     * et envoi des réponses ACCEPT/REJECT correspondantes.
     */
    private class DecideBehaviour extends OneShotBehaviour {
        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state: " + DECIDING);
        }
        @Override
        public void action() {
            AID winner = null;
            int best = Integer.MAX_VALUE;
            for (Map.Entry<AID, String> e : proposals.entrySet()) {
                int price = Integer.parseInt(e.getValue());
                if (price < best) {
                    best = price;
                    winner = e.getKey();
                }
            }
            if (winner != null) {
                System.out.println(getLocalName() + " :: selected winner: " +
                        winner.getLocalName() + " with price " + best);
            }
            for (Map.Entry<AID, String> e : proposals.entrySet()) {
                ACLMessage reply = new ACLMessage(e.getKey().equals(winner)
                        ? ACLMessage.ACCEPT_PROPOSAL
                        : ACLMessage.REJECT_PROPOSAL);
                reply.addReceiver(e.getKey());
                reply.setContent(e.getValue());
                send(reply);
                System.out.println(getLocalName() + " :: sending " +
                        ACLMessage.getPerformative(reply.getPerformative()) +
                        " to " + e.getKey().getLocalName() +
                        " with content: " + reply.getContent());
            }
        }
        @Override
        public int onEnd() {
            int code = ACLMessage.INFORM;
            System.out.println(getLocalName() + " :: exiting state: " + DECIDING + " with exit code: " + code);
            return code;
        }
    }

    /** Terminal state printing the end of the protocol. */
    private static class EndBehaviour extends OneShotBehaviour {
        @Override
        public void onStart() {
            System.out.println(myAgent.getLocalName() + " :: entering state: " + END);
        }
        @Override
        public void action() {
            System.out.println("Jack :: auction finished.");
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(myAgent.getLocalName() + " :: exiting state: " + END + " with exit code: " + code);
            return code;
        }
    }
}
