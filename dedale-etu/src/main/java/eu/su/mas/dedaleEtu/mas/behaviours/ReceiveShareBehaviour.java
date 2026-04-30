package eu.su.mas.dedaleEtu.mas.behaviours;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import eu.su.mas.dedaleEtu.mas.agents.MultiExploreAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * Single dispatcher for all four MEXPLO-* protocols. Keeping it cyclic and
 * filtered avoids polluting the main exploration loop with receive-side branching.
 */
public class ReceiveShareBehaviour extends CyclicBehaviour {

    private static final long serialVersionUID = 1L;

    private final MultiExploreAgent agent;
    private final MessageTemplate template;

    public ReceiveShareBehaviour(MultiExploreAgent a) {
        super(a);
        this.agent = a;
        this.template = MessageTemplate.or(
                MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_TOPO),
                MessageTemplate.or(
                        MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_INTENT),
                        MessageTemplate.or(
                                MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_DONE),
                                MessageTemplate.MatchProtocol(MultiExploreAgent.PROTO_UNDONE))));
    }

    @Override
    public void action() {
        ACLMessage msg = agent.receive(template);
        if (msg == null) {
            block();
            return;
        }
        String sender = msg.getSender().getLocalName();
        String protocol = msg.getProtocol();

        if (MultiExploreAgent.PROTO_TOPO.equals(protocol)) {
            try {
                @SuppressWarnings("unchecked")
                SerializableSimpleGraph<String, MapAttribute> g =
                        (SerializableSimpleGraph<String, MapAttribute>) msg.getContentObject();
                agent.initMap();
                MapRepresentation myMap = agent.getMyMap();
                myMap.mergeMap(g);
            } catch (UnreadableException e) {
                e.printStackTrace();
            }
        } else if (MultiExploreAgent.PROTO_INTENT.equals(protocol)) {
            String target = msg.getContent();
            if (target == null || target.isEmpty()) {
                agent.getTeamIntentions().remove(sender);
            } else {
                agent.getTeamIntentions().put(sender, target);
            }
        } else if (MultiExploreAgent.PROTO_DONE.equals(protocol)) {
            agent.getTerminationVotes().add(sender);
        } else if (MultiExploreAgent.PROTO_UNDONE.equals(protocol)) {
            agent.getTerminationVotes().remove(sender);
        }
    }
}
