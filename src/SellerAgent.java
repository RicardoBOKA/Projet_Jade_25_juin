import jade.core.Agent;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Generic seller agent participating in a one-shot auction protocol.
 */
public class SellerAgent extends Agent implements Constants {

    private String price;
    private int performative = ACLMessage.PROPOSE;
    private ACLMessage lastMessage;

    private static final String WAITING = "WAITING";
    private static final String PROPOSING = "PROPOSING";
    private static final String REFUSING = "REFUSING";
    private static final String WINNING = "WINNING";
    private static final String LOSING = "LOSING";

    @Override
    protected void setup() {
        doArguments();
        addBehaviour(new SellerBehaviour(this));
    }

    private void doArguments() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            price = args[0].toString();
        } else {
            performative = ACLMessage.REFUSE;
        }
    }

    private void doMessage(ACLMessage msg) {
        lastMessage = msg;
    }

    /**
     * FSM controlling the seller life cycle.
     */
    private class SellerBehaviour extends FSMBehaviour {
        SellerBehaviour(Agent a) {
            super(a);
        }

        @Override
        public void onStart() {
            registerFirstState(new HandleMessageBehaviour(WAITING), WAITING);
            registerState(new HandleMessageBehaviour(PROPOSING), PROPOSING);
            registerLastState(new RefuseBehaviour(), REFUSING);
            registerLastState(new WinnerBehaviour(), WINNING);
            registerLastState(new LoserBehaviour(), LOSING);

            registerTransition(WAITING, PROPOSING, ACLMessage.PROPOSE);
            registerTransition(WAITING, REFUSING, ACLMessage.REFUSE);
            registerTransition(PROPOSING, WINNING, ACLMessage.ACCEPT_PROPOSAL);
            registerTransition(PROPOSING, LOSING, ACLMessage.REJECT_PROPOSAL);
        }

        @Override
        public int onEnd() {
            myAgent.doDelete();
            return super.onEnd();
        }
    }

    /**
     * Behaviour handling messages according to the current state.
     */
    private class HandleMessageBehaviour extends SimpleBehaviour {
        private final String state;
        private MessageTemplate template;
        private boolean finished;
        private int exitCode;

        HandleMessageBehaviour(String state) {
            this.state = state;
        }

        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state: " + state);
            if (WAITING.equals(state)) {
                template = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.CFP),
                        MessageTemplate.MatchSender(JACK));
            } else if (PROPOSING.equals(state)) {
                template = MessageTemplate.and(
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)),
                        MessageTemplate.MatchSender(JACK));
            }
        }

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(template);
            if (msg != null) {
                System.out.println(getLocalName() + " :: received " +
                        ACLMessage.getPerformative(msg.getPerformative()) +
                        " from " + msg.getSender().getLocalName() +
                        " with content: " + msg.getContent());
                doMessage(msg);
                if (WAITING.equals(state)) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(performative);
                    if (performative == ACLMessage.PROPOSE) {
                        reply.setContent(price);
                        exitCode = ACLMessage.PROPOSE;
                        System.out.println(getLocalName() + " :: proposing price " + price);
                    } else {
                        reply.setContent("no stock");
                        exitCode = ACLMessage.REFUSE;
                        System.out.println(getLocalName() + " :: no price provided, sending REFUSE");
                    }
                    myAgent.send(reply);
                    System.out.println(getLocalName() + " :: sending " +
                            ACLMessage.getPerformative(reply.getPerformative()) +
                            " to " + msg.getSender().getLocalName() +
                            " with content: " + reply.getContent());
                } else if (PROPOSING.equals(state)) {
                    if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                        exitCode = ACLMessage.ACCEPT_PROPOSAL;
                    } else {
                        exitCode = ACLMessage.REJECT_PROPOSAL;
                    }
                }
                finished = true;
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return finished;
        }

        @Override
        public int onEnd() {
            System.out.println(getLocalName() + " :: exiting state: " + state + " with exit code: " + exitCode);
            return exitCode;
        }
    }

    /** Terminal behaviour for refusal state. */
    private static class RefuseBehaviour extends OneShotBehaviour {
        @Override
        public void onStart() {
            System.out.println(myAgent.getLocalName() + " :: entering state: " + REFUSING);
        }
        @Override
        public void action() {
            System.out.println(myAgent.getLocalName() + " :: out of stock.");
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(myAgent.getLocalName() + " :: exiting state: " + REFUSING + " with exit code: " + code);
            return code;
        }
    }

    /** Terminal behaviour when winning the auction. */
    private class WinnerBehaviour extends OneShotBehaviour {
        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state: " + WINNING);
        }
        @Override
        public void action() {
            System.out.println(getLocalName() + " :: wins with price " + price);
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(getLocalName() + " :: exiting state: " + WINNING + " with exit code: " + code);
            return code;
        }
    }

    /** Terminal behaviour when losing the auction. */
    private class LoserBehaviour extends OneShotBehaviour {
        @Override
        public void onStart() {
            System.out.println(getLocalName() + " :: entering state: " + LOSING);
        }
        @Override
        public void action() {
            System.out.println(getLocalName() + " :: loses with price " + price);
        }

        @Override
        public int onEnd() {
            int code = super.onEnd();
            System.out.println(getLocalName() + " :: exiting state: " + LOSING + " with exit code: " + code);
            return code;
        }
    }
}
