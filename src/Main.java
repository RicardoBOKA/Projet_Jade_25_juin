
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        ProfileImpl profile = new ProfileImpl();
        AgentContainer container = rt.createMainContainer(profile);
        try {
            container.createNewAgent(Constants.JACK, BuyerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.LILI, SellerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.LOLA, SellerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.JIM, SellerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.LULU, SellerAgent.class.getName(), null).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}