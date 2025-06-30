import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.ParallelBehaviour;
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
            System.out.println(getLocalName() + " :: starting BuyerBehaviour FSM");
            registerFirstState(new CFPBehaviour(), CALLING);
            registerState(new ParallelHandleBehaviour(), WAITING);
            registerState(new DecideBehaviour(), DECIDING);
            registerLastState(new EndBehaviour(), END);

            registerTransition(CALLING, WAITING, 0);
            registerTransition(WAITING, DECIDING, 0);
            registerDefaultTransition(DECIDING, END);
            //registerTransition(DECIDING, END, 0);
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(getLocalName() + " :: BuyerBehaviour finished with code " + code);
            return code;
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
        public void action() {
            System.out.println(getLocalName() + " :: entering state CALLING");
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID aid : FSMSELLER) {
                cfp.addReceiver(aid);
            }
            cfp.setContent("Looking for a bike");
            send(cfp);
            System.out.println(getLocalName() + " :: sent " + ACLMessage.getPerformative(cfp.getPerformative()) +
                    " to sellers with content " + cfp.getContent());
        }

        @Override
        public int onEnd() {
            int code = 0;
            System.out.println(getLocalName() + " :: exiting state CALLING with code " + code);
            System.out.println(getLocalName() + " :: FSM transitioning from CALLING to WAITING");
            return code;
        }
    }

    /**
     * Collecte les réponses des vendeurs en parallèle. Un sous-comportement
     * {@link WaitReplyBehaviour} est ajouté pour chaque vendeur afin de
     * recevoir sa réponse sans bloquer les autres. Le ParallelBehaviour se
     * termine lorsque toutes les réponses ont été traitées.
     */
    private class ParallelHandleBehaviour extends ParallelBehaviour {
        private MessageTemplate template;

        ParallelHandleBehaviour() {
            super(WHEN_ALL);
        }

        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state WAITING");
            template = MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.REFUSE));
            for (AID seller : FSMSELLER) {
                addSubBehaviour(new WaitReplyBehaviour(seller, template));
            }
        }

        @Override
        public int onEnd() {
            int code = 0;
            System.out.println(getLocalName() + " :: exiting state WAITING with code " + code);
            System.out.println(getLocalName() + " :: FSM transitioning from WAITING to DECIDING");
            return code;
        }
    }

    /** Behaviour waiting a reply from a single seller. */
    private class WaitReplyBehaviour extends SimpleBehaviour {
        private final AID seller;
        private final MessageTemplate template;
        private boolean received;

        WaitReplyBehaviour(AID seller, MessageTemplate template) {
            this.seller = seller;
            this.template = template;
        }

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                    template,
                    MessageTemplate.MatchSender(seller));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    proposals.put(seller, msg.getContent());
                    System.out.println(getLocalName() + " :: received PROPOSE from " +
                            seller.getLocalName() + " with content " + msg.getContent());
                } else {
                    refusals.add(seller);
                    System.out.println(getLocalName() + " :: received REFUSE from " +
                            seller.getLocalName() + " with content " + msg.getContent());
                    addBehaviour(new RefuseBehaviour(seller, msg.getContent()));
                }
                received = true;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return received;
        }

        @Override
        public int onEnd() {
            int code = 0;
            System.out.println(getLocalName() + " :: exiting WaitReplyBehaviour for " +
                    seller.getLocalName() + " with code " + code);
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
            System.out.println(getLocalName() + " :: entering RefuseBehaviour");
            System.out.println(getLocalName() + " :: seller " + seller.getLocalName() +
                    " refused because " + reason);
        }

        @Override
        public int onEnd() {
            int code = 0;
            System.out.println(getLocalName() + " :: exiting RefuseBehaviour with code " + code);
            return code;
        }
    }

    /**
     * Dernier état : sélection du vendeur proposant le prix le plus bas
     * et envoi des réponses ACCEPT/REJECT correspondantes.
     */
    private class DecideBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println(getLocalName() + " :: entering state DECIDING");
            AID winner = null;
            int best = Integer.MAX_VALUE;
            for (Map.Entry<AID, String> e : proposals.entrySet()) {
                int price = Integer.parseInt(e.getValue());
                if (price < best) {
                    best = price;
                    winner = e.getKey();
                }
            }
            for (Map.Entry<AID, String> e : proposals.entrySet()) {
                ACLMessage reply = new ACLMessage(e.getKey().equals(winner)
                        ? ACLMessage.ACCEPT_PROPOSAL
                        : ACLMessage.REJECT_PROPOSAL);
                reply.addReceiver(e.getKey());
                reply.setContent(e.getValue());
                send(reply);
                System.out.println(getLocalName() + " :: sent " + ACLMessage.getPerformative(reply.getPerformative()) +
                        " to " + e.getKey().getLocalName() + " with content " + reply.getContent());
            }
        }
        @Override
        public int onEnd() {
            // Trigger the transition to the terminal state of the FSM
            int code = ACLMessage.INFORM;
            System.out.println(getLocalName() + " :: exiting state DECIDING with code " + code);
            System.out.println(getLocalName() + " :: FSM transitioning from DECIDING to END");
            return code;
        }
    }

    /** Terminal state printing the end of the protocol. */
    private static class EndBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            System.out.println("Jack :: entering state END");
            System.out.println("Jack :: auction finished.");
        }

        @Override
        public int onEnd() {
            int code = 0;
            System.out.println("Jack :: exiting state END with code " + code);
            return code;
        }
    }
}
