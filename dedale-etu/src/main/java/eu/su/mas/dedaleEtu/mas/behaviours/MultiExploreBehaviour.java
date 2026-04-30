package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedaleEtu.mas.agents.MultiExploreAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Coordinated exploration loop.
 *
 * Each tick:
 *   1) close current node, register neighbors as open (DFS-like backbone)
 *   2) detect local termination (no open node) and emit DONE / global termination check
 *   3) pick next step, avoiding nodes targeted by lex-smaller teammates (collision avoidance)
 *   4) broadcast intention (only when changed) and topology (every tick)
 *   5) moveTo(target)  -- last action of the behaviour
 */
public class MultiExploreBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 1L;
    private static final long TICK_PAUSE_MS = 800;
    private static final int IDLE_REBROADCAST_THRESHOLD = 10;

    private final MultiExploreAgent agent;
    private boolean finished = false;
    private String previousIntention = null;
    private int idleTicks = 0;

    public MultiExploreBehaviour(MultiExploreAgent a) {
        super(a);
        this.agent = a;
    }

    @Override
    public void action() {
        try {
            agent.doWait(TICK_PAUSE_MS);
        } catch (Exception ignored) {
        }

        Location myPosition = agent.getCurrentPosition();
        if (myPosition == null) return;

        agent.initMap();
        MapRepresentation myMap = agent.getMyMap();
        String myNodeId = myPosition.getLocationId();

        List<Couple<Location, List<Couple<Observation, String>>>> lobs = agent.observe();

        myMap.addNode(myNodeId, MapAttribute.closed);
        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nb = obs.getLeft().getLocationId();
            if (!nb.equals(myNodeId)) {
                myMap.addNewNode(nb);
                myMap.addEdge(myNodeId, nb);
            }
        }

        if (!myMap.hasOpenNode()) {
            handleLocalTermination();
            return;
        }
        idleTicks = 0;

        if (agent.isIAmDone()) {
            agent.setIAmDone(false);
            agent.getTerminationVotes().remove(agent.getLocalName());
            broadcast(MultiExploreAgent.PROTO_UNDONE, null, null);
        }

        String nextStep = chooseNextStep(myNodeId, lobs);
        if (nextStep == null) return;

        agent.getTeamIntentions().put(agent.getLocalName(), nextStep);
        if (!nextStep.equals(previousIntention)) {
            broadcast(MultiExploreAgent.PROTO_INTENT, nextStep, null);
            previousIntention = nextStep;
        }

        SerializableSimpleGraph<String, MapAttribute> sg = myMap.getSerializableGraph();
        broadcast(MultiExploreAgent.PROTO_TOPO, null, sg);

        boolean ok = agent.moveTo(new GsLocation(nextStep));
        if (!ok) {
            previousIntention = null;
        }
    }

    private void handleLocalTermination() {
        if (!agent.isIAmDone()) {
            agent.setIAmDone(true);
            agent.getTerminationVotes().add(agent.getLocalName());
            broadcast(MultiExploreAgent.PROTO_DONE, null, null);
            System.out.println(agent.getLocalName() + " - locally done, broadcasting DONE.");
        }

        List<String> teammates = agent.getTeammates();
        Set<String> votes = agent.getTerminationVotes();
        if (teammates.stream().allMatch(votes::contains)) {
            finished = true;
            System.out.println(agent.getLocalName() + " - GLOBAL TERMINATION (all explored).");
            return;
        }

        idleTicks++;
        if (idleTicks > IDLE_REBROADCAST_THRESHOLD) {
            broadcast(MultiExploreAgent.PROTO_DONE, null, null);
            idleTicks = 0;
        }
    }

    private String chooseNextStep(String myNodeId,
                                  List<Couple<Location, List<Couple<Observation, String>>>> lobs) {
        MapRepresentation myMap = agent.getMyMap();
        Set<String> avoid = computeAvoidSet();
        List<String> openNodes = myMap.getOpenNodes();
        if (openNodes.isEmpty()) return null;

        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nb = obs.getLeft().getLocationId();
            if (nb.equals(myNodeId)) continue;
            if (openNodes.contains(nb) && !avoid.contains(nb)) return nb;
        }
        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nb = obs.getLeft().getLocationId();
            if (nb.equals(myNodeId)) continue;
            if (openNodes.contains(nb)) return nb;
        }

        List<String> candidates = openNodes.stream()
                .filter(n -> !avoid.contains(n))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) candidates = openNodes;

        int bestLen = Integer.MAX_VALUE;
        String bestStep = null;
        for (String cand : candidates) {
            List<String> path = myMap.getShortestPath(myNodeId, cand);
            if (path != null && !path.isEmpty() && path.size() < bestLen) {
                bestLen = path.size();
                bestStep = path.get(0);
            }
        }
        return bestStep;
    }

    private Set<String> computeAvoidSet() {
        String myName = agent.getLocalName();
        return agent.getTeamIntentions().entrySet().stream()
                .filter(e -> e.getKey().compareTo(myName) < 0)
                .map(Map.Entry::getValue)
                .filter(v -> v != null && !v.isEmpty())
                .collect(Collectors.toSet());
    }

    private void broadcast(String protocol, String content, Serializable contentObject) {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol(protocol);
        msg.setSender(agent.getAID());
        if (content != null) msg.setContent(content);
        if (contentObject != null) {
            try {
                msg.setContentObject(contentObject);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        for (String t : agent.getTeammates()) {
            msg.addReceiver(new AID(t, AID.ISLOCALNAME));
        }
        agent.sendMessage(msg);
    }

    @Override
    public boolean done() {
        return finished;
    }
}
