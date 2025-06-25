import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.*;

public class BuyerAgent extends Agent implements Constants {

    private final Map<AID, Integer> proposals = new HashMap<>();
    private final Set<AID> refusals = new HashSet<>();

    private static final String CALLING = "CALLING";
    private static final String WAITING = "WAITING";
    private static final String DECIDING = "DECIDING";

    @Override
    protected void setup() {
        FSMBehaviour fsm = new FSMBehaviour(this);
        fsm.registerFirstState(new CFPBehaviour(), CALLING);
        fsm.registerState(new ParallelHandleBehaviour(), WAITING);
        fsm.registerState(new ChooseBehaviour(), DECIDING);

        fsm.registerLastState(new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println("Jack :: auction finished.");
            }
        }, "END");

        fsm.registerDefaultTransition(CALLING, WAITING);
        fsm.registerDefaultTransition(WAITING, DECIDING);

        fsm.registerTransition(DECIDING, "END", 0);

        addBehaviour(fsm);
    }

    private class CFPBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (AID aid : FSMSELLER) {
                cfp.addReceiver(aid);
            }
            cfp.setContent("Looking for a bike");
            myAgent.send(cfp);
        }
    }

    private class ParallelHandleBehaviour extends Behaviour {
        private int replies = 0;

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                replies++;
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    proposals.put(msg.getSender(), Integer.parseInt(msg.getContent()));
                } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                    refusals.add(msg.getSender());
                    addBehaviour(new RefuseBehaviour(msg.getSender(), msg.getContent()));
                }
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return replies == FSMSELLER.size();
        }
    }

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

    private class ChooseBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            AID winner = null;
            int best = Integer.MAX_VALUE;
            for (Map.Entry<AID, Integer> e : proposals.entrySet()) {
                if (e.getValue() < best) {
                    best = e.getValue();
                    winner = e.getKey();
                }
            }
            for (Map.Entry<AID, Integer> e : proposals.entrySet()) {
                ACLMessage reply = new ACLMessage(e.getKey().equals(winner) ?
                        ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
                reply.addReceiver(e.getKey());
                reply.setContent(String.valueOf(e.getValue()));
                myAgent.send(reply);
            }
        }
    }
}