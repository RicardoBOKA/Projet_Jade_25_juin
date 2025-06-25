import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Vendeur générique utilisé pour Lili, Lola, Jim et Lulu.
 * Chaque agent suit une FSM où il attend un CFP, propose un prix ou refuse,
 * puis apprend s'il a gagné ou perdu l'enchère.
 */
public class SellerAgent extends Agent implements Constants {

    private int price = -1;
    private static final String WAITING = "WAITING";
    private static final String PROPOSING = "PROPOSING";
    private static final String REFUSING = "REFUSING";
    private static final String WINNING = "WINNING";
    private static final String LOSING = "LOSING";

    /**
     * Initialisation de la FSM du vendeur.
     * Chaque état correspond à une étape du protocole d'enchère.
     */
    @Override
    protected void setup() {
        FSMBehaviour fsm = new FSMBehaviour(this);
        fsm.registerFirstState(new HandleMessageBehaviour(WAITING), WAITING);
        fsm.registerState(new HandleMessageBehaviour(PROPOSING), PROPOSING);
        fsm.registerState(new RefuseBehaviour(this), REFUSING);
        fsm.registerState(new WinnerBehaviour(this), WINNING);
        fsm.registerState(new LoserBehaviour(this), LOSING);

        // Transitions normales
        fsm.registerTransition(WAITING, PROPOSING, ACLMessage.PROPOSE);
        fsm.registerTransition(WAITING, REFUSING, ACLMessage.REFUSE);
        fsm.registerTransition(PROPOSING, WINNING, ACLMessage.ACCEPT_PROPOSAL);
        fsm.registerTransition(PROPOSING, LOSING, ACLMessage.REJECT_PROPOSAL);

        // État final
        fsm.registerLastState(new OneShotBehaviour() {
            public void action() {
                System.out.println(getLocalName() + " :: FSM terminé.");
            }
        }, "END");

        // Transitions finales
        fsm.registerTransition(REFUSING, "END", ACLMessage.INFORM);
        fsm.registerTransition(WINNING, "END", ACLMessage.INFORM);
        fsm.registerTransition(LOSING, "END", ACLMessage.INFORM);

        addBehaviour(fsm);
    }

    /**
     * Gère la réception des messages suivant l'état courant du vendeur.
     */
    private class HandleMessageBehaviour extends Behaviour {
        private final String state;
        private boolean finished = false;
        private int exit = 0;
        HandleMessageBehaviour(String state) { this.state = state; }

        @Override
        public void action() {
            if (WAITING.equals(state)) {
                ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.CFP));
                if (msg != null) {
                    decidePrice();
                    if (price < 0) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("rupture de stock");
                        myAgent.send(reply);
                        exit = ACLMessage.REFUSE;
                        finished = true;
                    } else {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(String.valueOf(price));
                        myAgent.send(reply);
                        exit = ACLMessage.PROPOSE;
                        finished = true;
                    }
                } else {
                    block();
                }
            } else if (PROPOSING.equals(state)) {
                ACLMessage msg = myAgent.receive(MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                        MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)));
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        exit = ACLMessage.ACCEPT_PROPOSAL;
                    } else {
                        exit = ACLMessage.REJECT_PROPOSAL;
                    }
                    finished = true;
                } else {
                    block();
                }
            }
        }

        /**
         * Détermine le prix proposé par ce vendeur.
         * Jim renvoie -1 pour simuler une indisponibilité.
         */
        private void decidePrice() {
            switch (getLocalName()) {
                case LILI: price = 2700; break;
                case LOLA: price = 2800; break;
                case JIM: price = -1; break;
                case LULU: price = 2400; break;
                default: price = 9999; break;
            }
        }

        @Override
        public boolean done() { return finished; }

        @Override
        public int onEnd() { return exit; }
    }

    /**
     * État où le vendeur indique son impossibilité de participer.
     */
    private static class RefuseBehaviour extends OneShotBehaviour {
        RefuseBehaviour(Agent a) { super(a); }
        @Override
        public void action() {}
    }

    /**
     * Affiche que ce vendeur a remporté l'enchère.
     */
    private class WinnerBehaviour extends OneShotBehaviour {
        WinnerBehaviour(Agent a) { super(a); }
        @Override
        public void action() {
            System.out.println("action :: agent " + getLocalName() + " is a great winner with a price equals to " + price);
        }
    }

    /**
     * Affiche que ce vendeur a perdu l'enchère.
     */
    private class LoserBehaviour extends OneShotBehaviour {
        LoserBehaviour(Agent a) { super(a); }
        @Override
        public void action() {
            System.out.println("action :: agent " + getLocalName() + " is a pity loser with a price equals to " + price);
        }
    }
}
