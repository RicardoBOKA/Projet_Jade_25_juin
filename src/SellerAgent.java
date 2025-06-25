import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends Agent implements Constants {

    private int price = -1;
    private static final String WAITING = "WAITING";
    private static final String PROPOSING = "PROPOSING";
    private static final String REFUSING = "REFUSING";
    private static final String WINNING = "WINNING";
    private static final String LOSING = "LOSING";

    @Override
    protected void setup() {
        FSMBehaviour fsm = new FSMBehaviour(this);
        fsm.registerFirstState(new HandleMessageBehaviour(WAITING), WAITING);
        fsm.registerState(new HandleMessageBehaviour(PROPOSING), PROPOSING);
        fsm.registerState(new RefuseBehaviour(this), REFUSING);
        fsm.registerState(new WinnerBehaviour(this), WINNING);
        fsm.registerState(new LoserBehaviour(this), LOSING);

        // Transitions normales
        fsm.registerTransition(WAITING, PROPOSING, 1);
        fsm.registerTransition(WAITING, REFUSING, 2);
        fsm.registerTransition(PROPOSING, WINNING, 1);
        fsm.registerTransition(PROPOSING, LOSING, 2);

        // État final
        fsm.registerLastState(new OneShotBehaviour() {
            public void action() {
                System.out.println(getLocalName() + " :: FSM terminé.");
            }
        }, "END");

        // Transitions finales
        fsm.registerTransition(REFUSING, "END", 0);
        fsm.registerTransition(WINNING, "END", 0);
        fsm.registerTransition(LOSING, "END", 0);

        addBehaviour(fsm);
    }

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
                        exit = 2;
                        finished = true;
                    } else {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(String.valueOf(price));
                        myAgent.send(reply);
                        exit = 1;
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
                        exit = 1;
                    } else {
                        exit = 2;
                    }
                    finished = true;
                } else {
                    block();
                }
            }
        }

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

    private static class RefuseBehaviour extends OneShotBehaviour {
        RefuseBehaviour(Agent a) { super(a); }
        @Override
        public void action() {}
    }

    private class WinnerBehaviour extends OneShotBehaviour {
        WinnerBehaviour(Agent a) { super(a); }
        @Override
        public void action() {
            System.out.println("action :: agent " + getLocalName() + " is a great winner with a price equals to " + price);
        }
    }

    private class LoserBehaviour extends OneShotBehaviour {
        LoserBehaviour(Agent a) { super(a); }
        @Override
        public void action() {
            System.out.println("action :: agent " + getLocalName() + " is a pity loser with a price equals to " + price);
        }
    }
}