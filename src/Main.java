
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
            container.createNewAgent(Constants.LILI, SellerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.LOLA, SellerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.JIM, SellerAgent.class.getName(), null).start();
            container.createNewAgent(Constants.LULU, SellerAgent.class.getName(), null).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
