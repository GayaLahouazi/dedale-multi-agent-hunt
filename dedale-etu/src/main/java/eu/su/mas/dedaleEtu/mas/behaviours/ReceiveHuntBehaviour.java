package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedaleEtu.mas.agents.HuntAgent;
import eu.su.mas.dedaleEtu.mas.agents.MultiExploreAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.HuntSightingMessage;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.WumpusTracker;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * Dispatcher cyclique pour <b>tous</b> les protocoles utilisés par {@link HuntAgent} :
 * les 4 protocoles d'exploration hérités ({@code MEXPLO-*}) et les 2 protocoles
 * de chasse ({@code HUNT-*}).
 *
 * <p>Cette classe remplace {@link ReceiveShareBehaviour} pour les agents chasseurs ;
 * elle ne l'étend pas afin de garder le filtrage des messages dans une seule
 * {@link MessageTemplate}, ce qui évite les passages multiples sur la file d'attente.
 *
 * <h3>Protocoles traités</h3>
 * <table border="1">
 *   <tr><th>Protocole</th><th>Action</th></tr>
 *   <tr><td>{@code MEXPLO-TOPO}</td><td>fusion topologique dans la carte locale</td></tr>
 *   <tr><td>{@code MEXPLO-INTENT}</td><td>maj des intentions de l'émetteur</td></tr>
 *   <tr><td>{@code MEXPLO-DONE}</td><td>ajout de l'émetteur aux votes de terminaison</td></tr>
 *   <tr><td>{@code MEXPLO-UNDONE}</td><td>retrait du vote correspondant</td></tr>
 *   <tr><td>{@code HUNT-SIGHTING}</td><td>maj de la position de l'émetteur + maj du tracker</td></tr>
 *   <tr><td>{@code HUNT-CAPTURED}</td><td>marque la chasse comme terminée</td></tr>
 * </table>
 */
public class ReceiveHuntBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private final HuntAgent agent;

    /** Filtre pré-construit pour les 6 protocoles. */
    private final MessageTemplate template;

    public ReceiveHuntBehaviour(HuntAgent a) {
        super(a);
        this.agent = a;
        this.template = MessageTemplate.or(
                MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_TOPO),
                MessageTemplate.or(
                        MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_INTENT),
                        MessageTemplate.or(
                                MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_DONE),
                                MessageTemplate.or(
                                        MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_UNDONE),
                                        MessageTemplate.or(
                                                MessageTemplate.MatchProtocol(HuntAgent.PROTO_SIGHTING),
                                                MessageTemplate.MatchProtocol(HuntAgent.PROTO_CAPTURED))))));
    }

    @Override
    public void action() {
        ACLMessage msg = agent.receive(template);
        if (msg == null) {
            block();
            return;
        }

        String sender   = msg.getSender().getLocalName();
        String protocol = msg.getProtocol();

        switch (protocol) {
            case MultiExploreAgent.PROTO_TOPO:
                handleTopo(msg);
                break;
            case MultiExploreAgent.PROTO_INTENT:
                handleIntent(msg, sender);
                break;
            case MultiExploreAgent.PROTO_DONE:
                agent.getTerminationVotes().add(sender);
                break;
            case MultiExploreAgent.PROTO_UNDONE:
                agent.getTerminationVotes().remove(sender);
                break;
            case HuntAgent.PROTO_SIGHTING:
                handleSighting(msg, sender);
                break;
            case HuntAgent.PROTO_CAPTURED:
                handleCaptured(msg, sender);
                break;
            default:
                // Protocole inconnu : on ignore (le filtre devrait empêcher d'arriver ici).
                break;
        }
    }

    /* ════════════════════════════════════════════════════════════════════════
     * Handlers par protocole
     * ════════════════════════════════════════════════════════════════════════ */

    /** Fusionne la topologie reçue dans la carte locale. */
    private void handleTopo(ACLMessage msg) {
        try {
            @SuppressWarnings("unchecked")
            SerializableSimpleGraph<String, MapAttribute> received =
                    (SerializableSimpleGraph<String, MapAttribute>) msg.getContentObject();
            agent.initMap();
            agent.getMyMap().mergeMap(received);
        } catch (UnreadableException e) {
            System.err.println("[" + agent.getLocalName()
                    + "] TOPO non désérialisable : " + e.getMessage());
        }
    }

    /** Met à jour ou retire l'intention déclarée de l'émetteur. */
    private void handleIntent(ACLMessage msg, String sender) {
        String target = msg.getContent();
        if (target == null || target.isEmpty()) {
            agent.getTeamIntentions().remove(sender);
        } else {
            agent.getTeamIntentions().put(sender, target);
        }
    }

    /**
     * Met à jour le tracker avec les odeurs partagées par {@code sender}, et
     * met à jour la position connue de {@code sender}.
     */
    private void handleSighting(ACLMessage msg, String sender) {
        try {
            HuntSightingMessage payload = (HuntSightingMessage) msg.getContentObject();
            if (payload.senderPosition != null) {
                agent.getTeamPositions().put(sender, payload.senderPosition);
            }
            // Mise à jour du tracker — la fusion se fait par les ensembles.
            // Note : on ne traite pas les anciens ticks différemment des nouveaux ;
            // le tracker est monotone-décroissant, donc des observations « anciennes »
            // ne contredisent pas les nouvelles tant que l'odeur est cohérente.
            WumpusTracker tracker = agent.getTracker();
            MapRepresentation map = agent.getMyMap();
            if (map != null) {
                tracker.update(payload.stenchPositive, payload.stenchNegative, map);
            }
        } catch (UnreadableException e) {
            System.err.println("[" + agent.getLocalName()
                    + "] SIGHTING non désérialisable : " + e.getMessage());
        }
    }

    /** Marque la chasse comme terminée et logge. */
    private void handleCaptured(ACLMessage msg, String sender) {
        if (!agent.isHuntComplete()) {
            agent.setHuntComplete(true);
            String location = msg.getContent();
            agent.setCaptureLocation(location);
            System.out.println("[" + agent.getLocalName()
                    + "] Notifié par " + sender + " : Golem capturé"
                    + (location != null ? " au nœud " + location : "") + ".");
        }
    }
}
