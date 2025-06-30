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

    private String price;
    private int performative = ACLMessage.PROPOSE;

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
        super.setup();
        addBehaviour(new SellerBehaviour(this));
        doArguments();

        /* Onstart SellerBehaviour
        fsm.registerFirstState(new HandleMessageBehaviour(WAITING), WAITING);
        fsm.registerState(new HandleMessageBehaviour(PROPOSING), PROPOSING);
        fsm.registerState(new RefuseBehaviour(this), REFUSING);
        fsm.registerState(new WinnerBehaviour(this), WINNING);
        fsm.registerState(new LoserBehaviour(this), LOSING);

        // Transitions normales
        fsm.registerTransition(WAITING, PROPOSING, ACLMessage.CFP*getPerformative());
        fsm.registerTransition(WAITING, REFUSING, ACLMessage.CFP*getPerformative());
        fsm.registerTransition(WAITING, WINNING, ACLMessage.ACCEPT_PROPOSAL*getPerformative());
        fsm.registerTransition(WAITING, LOSING, ACLMessage.REJECT_PROPOSAL*getPerformative());

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


        */

    }

    private class SellerBehaviour extends FSMBehaviour {

        private ACLMessage receivedMessage;

        public SellerBehaviour(SellerAgent sellerAgent) {
        }

        private void doArguments() {
            Object[] args = getArguments(); //Tableau d'objets

            //et ici ou l'on traite
            //si arg de 0 est passé ca veut dire qu'on a recupere le prix et donc on setPrice()
            // et ca veut dire que la performative inherente de mon vendeur c'est de faire un PROPOSE (s'il a un prix il propose, sinon il refuse)

        }

        //changer en simple behavior

        /**
         * Gère la réception des messages suivant l'état courant du vendeur.
         */
        private class HandleMessageBehaviour extends Behaviour {
            private final String state;
            private MessageTemplate template;
            private boolean finished = false;
            private int exit = 0;

            HandleMessageBehaviour(String state) {
                this.state = state;
            }

            //onStart() preparation template (CFP ou ACCEPT ou REJECT ) et Buyer
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(template);
                if (msg != null) {
                    doMessage(msg);
                } else {
                    block();
                }
            }

            private void doMessage(ACLMessage msg) {

            }
        /*
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

         */

            //Mettre dans le main

            /**
             * Détermine le prix proposé par ce vendeur.
             * Jim renvoie -1 pour simuler une indisponibilité.
             */
            private void decidePrice() {
                switch (getLocalName()) {
                    case LILI:
                        price = 2700;
                        break;
                    case LOLA:
                        price = 2800;
                        break;
                    case JIM:
                        price = -1;
                        break;
                    case LULU:
                        price = 2400;
                        break;
                    default:
                        price = 9999;
                        break;
                }
            }

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public int onEnd() {
                return exit;
            }
        }

        /**
         * État où le vendeur indique son impossibilité de participer.
         */
        private static class RefuseBehaviour extends OneShotBehaviour {
            RefuseBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void action() {
            }
        }

        /**
         * Affiche que ce vendeur a remporté l'enchère.
         */
        private class WinnerBehaviour extends OneShotBehaviour {
            WinnerBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void action() {
                System.out.println("action :: agent " + getLocalName() + " is a great winner with a price equals to " + price);
            }
        }

        /**
         * Affiche que ce vendeur a perdu l'enchère.
         */
        private class LoserBehaviour extends OneShotBehaviour {
            LoserBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void action() {
                System.out.println("action :: agent " + getLocalName() + " is a pity loser with a price equals to " + price);
            }
        }
    }
}
