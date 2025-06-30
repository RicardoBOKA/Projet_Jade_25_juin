
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

/**
 * Point d'entrée de l'application.
 * Démarre la plateforme JADE et crée les agents du scénario :
 * un acheteur et quatre vendeurs.
 */

public class Main {
    /**
     * Lance la plateforme JADE et instancie tous les agents du scénario.
     */
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        ProfileImpl profile = new ProfileImpl();
        AgentContainer container = rt.createMainContainer(profile);
        try {
            container.createNewAgent(Constants.JACK, BuyerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.LILI, SellerAgent.class.getName(), new Object[]{2700}).start();
            container.createNewAgent(Constants.LOLA, SellerAgent.class.getName(), new Object[]{2800}).start();
            container.createNewAgent(Constants.JIM, SellerAgent.class.getName(), new Object[]{-1}).start();
            container.createNewAgent(Constants.LULU, SellerAgent.class.getName(), new Object[]{2400}).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
