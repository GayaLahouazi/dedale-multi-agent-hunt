package eu.su.mas.dedaleEtu.mas.behaviours;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dataStructures.serializableGraph.SerializableSimpleGraph;
import dataStructures.tuple.Couple;

import eu.su.mas.dedale.env.Location;
import eu.su.mas.dedale.env.Observation;
import eu.su.mas.dedale.env.gs.GsLocation;
import eu.su.mas.dedaleEtu.mas.agents.HuntAgent;
import eu.su.mas.dedaleEtu.mas.agents.MultiExploreAgent;
import eu.su.mas.dedaleEtu.mas.knowledge.HuntSightingMessage;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation.MapAttribute;
import eu.su.mas.dedaleEtu.mas.knowledge.WumpusTracker;

import jade.core.AID;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Boucle principale du {@link HuntAgent} — explore puis chasse le Golem.
 *
 * <h3>Trois phases</h3>
 * <ol>
 *   <li><b>EXPLORE</b> — Aucune odeur jamais perçue. L'agent fait une exploration
 *       DFS coordonnée à la {@link MultiExploreBehaviour} (priorité lexicographique
 *       sur les intentions, broadcast périodique de la carte).</li>
 *   <li><b>TRACK</b> — Au moins une odeur observée mais position incertaine
 *       (ensemble de candidats &gt; 1). L'agent se dirige vers le candidat le
 *       plus proche pour réduire l'incertitude (de nouvelles observations
 *       élimineront des candidats).</li>
 *   <li><b>BLOCK</b> — Position du Golem connue avec certitude (singleton).
 *       Calcul d'une assignation déterministe : chaque chasseur reçoit un voisin
 *       du Golem à occuper. L'agent va vers son voisin assigné.</li>
 * </ol>
 *
 * <h3>Détection de capture</h3>
 * Si le Golem est localisé et que tous ses voisins sont occupés par des
 * chasseurs (positions connues via les sightings reçus), le Golem est bloqué.
 * L'agent broadcaste {@code HUNT-CAPTURED} et termine.
 *
 * <h3>Stratégie d'assignation (greedy déterministe)</h3>
 * <pre>
 * Soit V = voisins(golemPos) triés lexicographiquement
 * Soit H = chasseurs (incluant moi) triés lexicographiquement
 * Pour chaque h ∈ H (dans l'ordre):
 *     hPos = position connue de h
 *     v* = argmin_{v ∈ V \ Vassigned} distance(hPos, v)
 *     Vassigned += v*
 *     assignment[h] = v*
 * </pre>
 *
 * <h3>Garanties (1 Golem, communications suffisantes)</h3>
 * <ul>
 *   <li><b>Terminaison</b> : si {@code |chasseurs| ≥ degré(golemPos)} et la
 *       topologie est connexe, le Golem est capturé en au plus
 *       O(diamètre × |V|) ticks.</li>
 *   <li><b>Cohérence</b> : la stratégie greedy + le tri lexicographique sont
 *       déterministes : tous les chasseurs calculent la même assignation
 *       (à condition d'avoir la même information sur les positions).</li>
 *   <li><b>Robustesse</b> : si {@code |chasseurs| &lt; degré(golemPos)}, le
 *       Golem peut s'échapper. Stratégie d'amélioration documentée
 *       (push-to-leaf sur arbre) — non implémentée par défaut.</li>
 * </ul>
 *
 * <h3>Complexité par tick</h3>
 * <ul>
 *   <li>Observation + maj carte : O(deg(myNode))</li>
 *   <li>Mise à jour tracker : O(|S±| · |B(R)|) avec |B(R)| ≤ d^R</li>
 *   <li>Assignation : O(|H| · |V|) (Dijkstra par paire) — peut être amélioré</li>
 *   <li>Messages émis : O(|teammates|) — broadcast partagé</li>
 * </ul>
 */
public class HuntBehaviour extends SimpleBehaviour {

    private static final long serialVersionUID = 1L;

    /** Pause entre deux ticks (ms). Compromis vitesse / lisibilité. */
    private static final long TICK_PAUSE_MS = 800;

    private final HuntAgent agent;

    /** Vrai dès que la chasse est terminée (capture confirmée). */
    private boolean finished = false;

    /** Compteur de ticks (utile pour le log et le piggyback sur les sightings). */
    private long tickCounter = 0;

    /** Dernière intention diffusée — sert à éviter les rediffusions inutiles. */
    private String previousIntention = null;

    public HuntBehaviour(HuntAgent a) {
        super(a);
        this.agent = a;
    }

    @Override
    public void action() {
        // ── 0. Cadence ───────────────────────────────────────────────────────
        try {
            agent.doWait(TICK_PAUSE_MS);
        } catch (Exception ignored) {
        }
        tickCounter++;

        // Si on a déjà reçu HUNT-CAPTURED via le récepteur cyclique → on s'arrête.
        if (agent.isHuntComplete()) {
            finished = true;
            return;
        }

        Location myPos = agent.getCurrentPosition();
        if (myPos == null) return;

        agent.initMap();
        MapRepresentation map = agent.getMyMap();
        String myNodeId = myPos.getLocationId();

        // ── 1. Observer + classer les odeurs ─────────────────────────────────
        List<Couple<Location, List<Couple<Observation, String>>>> lobs = agent.observe();
        Set<String> stenchPositive = new HashSet<>();
        Set<String> stenchNegative = new HashSet<>();

        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nodeId = obs.getLeft().getLocationId();
            boolean hasStench = obs.getRight().stream()
                    .anyMatch(o -> o.getLeft() == Observation.STENCH);
            if (hasStench) {
                stenchPositive.add(nodeId);
            } else {
                stenchNegative.add(nodeId);
            }
        }

        // ── 2. Mise à jour de la carte ───────────────────────────────────────
        map.addNode(myNodeId, MapAttribute.closed);
        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nb = obs.getLeft().getLocationId();
            if (!nb.equals(myNodeId)) {
                map.addNewNode(nb);
                map.addEdge(myNodeId, nb);
            }
        }

        // ── 3. Mise à jour du tracker avec MES observations ──────────────────
        WumpusTracker tracker = agent.getTracker();
        tracker.update(stenchPositive, stenchNegative, map);

        // ── 4. Position courante : on l'enregistre pour les coéquipiers ──────
        agent.getTeamPositions().put(agent.getLocalName(), myNodeId);

        // ── 5. Broadcasts (sighting + topology) ──────────────────────────────
        broadcastSighting(myNodeId, stenchPositive, stenchNegative);
        broadcastTopology(map);

        // ── 6. Détection de capture ──────────────────────────────────────────
        if (tracker.isLocalised()) {
            String golemPos = tracker.getRepresentativePosition();
            if (isGolemBlocked(golemPos, map)) {
                onCapture(golemPos);
                finished = true;
                return;
            }
        }

        // ── 7. Choisir la prochaine cible selon la phase ─────────────────────
        String nextStep = chooseNextStep(myNodeId, lobs, map, tracker);
        if (nextStep == null || nextStep.equals(myNodeId)) {
            // Rien à faire ce tick (déjà en place ou aucun candidat).
            return;
        }

        // ── 8. Annoncer mon intention (anti-collision) ───────────────────────
        agent.getTeamIntentions().put(agent.getLocalName(), nextStep);
        if (!nextStep.equals(previousIntention)) {
            broadcastIntent(nextStep);
            previousIntention = nextStep;
        }

        // ── 9. Bouger ────────────────────────────────────────────────────────
        boolean moved = agent.moveTo(new GsLocation(nextStep));
        if (!moved) {
            // Collision (un coéquipier ou le Golem occupe la cible) → re-décider au tick suivant.
            previousIntention = null;
        }
    }

    @Override
    public boolean done() {
        return finished;
    }

    /* ════════════════════════════════════════════════════════════════════════
     * Choix de la prochaine cible
     * ════════════════════════════════════════════════════════════════════════ */

    /**
     * Aiguillage entre les trois phases EXPLORE / TRACK / BLOCK.
     */
    private String chooseNextStep(String myNodeId,
                                  List<Couple<Location, List<Couple<Observation, String>>>> lobs,
                                  MapRepresentation map,
                                  WumpusTracker tracker) {
        // Phase EXPLORE
        if (!tracker.hasAnyObservation()) {
            return chooseExplorationStep(myNodeId, lobs, map);
        }

        Set<String> candidates = tracker.getPossiblePositions();

        // Tracker incohérent → reset et retour à EXPLORE
        if (candidates.isEmpty()) {
            System.out.println("[" + agent.getLocalName()
                    + "] Tracker vide : observations contradictoires, reset.");
            tracker.reset();
            return chooseExplorationStep(myNodeId, lobs, map);
        }

        // Phase BLOCK
        if (candidates.size() == 1) {
            String golemPos = candidates.iterator().next();
            String myAssignedNeighbor = computeBlockingAssignment(golemPos, map);
            if (myAssignedNeighbor == null) {
                // Pas de voisin disponible → on continue à explorer pour gagner du contexte.
                return chooseExplorationStep(myNodeId, lobs, map);
            }
            if (myAssignedNeighbor.equals(myNodeId)) {
                // Déjà sur le voisin assigné : on reste en place (rien à faire).
                return null;
            }
            return stepTowards(myNodeId, myAssignedNeighbor, map);
        }

        // Phase TRACK : se diriger vers le candidat le plus proche
        String closestCandidate = null;
        int closestDist = Integer.MAX_VALUE;
        for (String c : candidates) {
            int d = map.getDistance(myNodeId, c);
            if (d < closestDist) {
                closestDist = d;
                closestCandidate = c;
            }
        }
        if (closestCandidate == null) return null;
        return stepTowards(myNodeId, closestCandidate, map);
    }

    /**
     * Étape d'exploration coordonnée (mêmes règles que {@link MultiExploreBehaviour})
     * — voisins ouverts non revendiqués &gt; voisins ouverts &gt; Dijkstra plus
     * proche nœud ouvert.
     */
    private String chooseExplorationStep(String myNodeId,
                                         List<Couple<Location, List<Couple<Observation, String>>>> lobs,
                                         MapRepresentation map) {
        Set<String> avoid = computeAvoidSet();
        List<String> openNodes = map.getOpenNodes();
        if (openNodes.isEmpty()) return null;

        // Pass 1 : voisin direct ouvert non revendiqué
        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nb = obs.getLeft().getLocationId();
            if (!nb.equals(myNodeId) && openNodes.contains(nb) && !avoid.contains(nb)) {
                return nb;
            }
        }
        // Pass 2 : voisin direct ouvert (avoidance ignorée)
        for (Couple<Location, List<Couple<Observation, String>>> obs : lobs) {
            String nb = obs.getLeft().getLocationId();
            if (!nb.equals(myNodeId) && openNodes.contains(nb)) {
                return nb;
            }
        }
        // Pass 3 : Dijkstra vers le plus proche nœud ouvert
        List<String> candidates = openNodes.stream()
                .filter(n -> !avoid.contains(n))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) candidates = openNodes;

        int bestLen = Integer.MAX_VALUE;
        String bestStep = null;
        for (String cand : candidates) {
            List<String> path = map.getShortestPath(myNodeId, cand);
            if (path != null && !path.isEmpty() && path.size() < bestLen) {
                bestLen = path.size();
                bestStep = path.get(0);
            }
        }
        return bestStep;
    }

    /**
     * Calcule l'assignation déterministe pour bloquer un Golem localisé.
     *
     * @return l'id du voisin du Golem que CET agent doit occuper, ou {@code null}
     *         si aucun voisin n'a pu lui être assigné
     */
    private String computeBlockingAssignment(String golemPos, MapRepresentation map) {
        List<String> neighbors = new ArrayList<>(map.getNeighbors(golemPos));
        if (neighbors.isEmpty()) return null;
        Collections.sort(neighbors); // ordre déterministe

        // Liste totale des chasseurs (moi + coéquipiers), triée pour le déterminisme.
        List<String> hunters = new ArrayList<>(agent.getTeammates());
        hunters.add(agent.getLocalName());
        Collections.sort(hunters);

        Map<String, String> assignment = new HashMap<>();
        Set<String> taken = new HashSet<>();

        for (String hunter : hunters) {
            String hunterPos;
            if (hunter.equals(agent.getLocalName())) {
                hunterPos = agent.getCurrentPosition().getLocationId();
            } else {
                hunterPos = agent.getTeamPositions().get(hunter);
            }
            if (hunterPos == null) {
                continue; // position inconnue → on saute (sera assigné quand on saura)
            }

            String bestNb = null;
            int bestDist = Integer.MAX_VALUE;
            for (String nb : neighbors) {
                if (taken.contains(nb)) continue;
                int d = map.getDistance(hunterPos, nb);
                if (d < bestDist) {
                    bestDist = d;
                    bestNb = nb;
                }
            }
            if (bestNb != null) {
                assignment.put(hunter, bestNb);
                taken.add(bestNb);
            }
        }

        return assignment.get(agent.getLocalName());
    }

    /**
     * Premier saut sur le plus court chemin {@code myNodeId → target}.
     * @return id du nœud à rejoindre au prochain {@code moveTo}, ou {@code null}
     *         si la cible est non-atteignable
     */
    private String stepTowards(String myNodeId, String target, MapRepresentation map) {
        if (myNodeId.equals(target)) return null;
        List<String> path = map.getShortestPath(myNodeId, target);
        if (path == null || path.isEmpty()) return null;
        return path.get(0);
    }

    /* ════════════════════════════════════════════════════════════════════════
     * Détection de capture
     * ════════════════════════════════════════════════════════════════════════ */

    /**
     * Le Golem est bloqué si tous ses voisins sont occupés par des chasseurs.
     * Utilise les positions connues (les nôtres + celles des coéquipiers reçues
     * via les sightings).
     */
    private boolean isGolemBlocked(String golemPos, MapRepresentation map) {
        List<String> neighbors = map.getNeighbors(golemPos);
        if (neighbors.isEmpty()) return false; // graphe encore incomplet

        // Réunir toutes les positions connues des chasseurs (moi + coéquipiers).
        Set<String> hunterPositions = new HashSet<>();
        Location my = agent.getCurrentPosition();
        if (my != null) hunterPositions.add(my.getLocationId());
        hunterPositions.addAll(agent.getTeamPositions().values());

        for (String nb : neighbors) {
            if (!hunterPositions.contains(nb)) {
                return false; // au moins un voisin non occupé → fuite possible
            }
        }
        return true;
    }

    /**
     * Annonce la capture aux coéquipiers et logge.
     */
    private void onCapture(String golemPos) {
        agent.setHuntComplete(true);
        agent.setCaptureLocation(golemPos);
        broadcastCaptured(golemPos);
        System.out.println("[" + agent.getLocalName()
                + "] === GOLEM CAPTURÉ au nœud " + golemPos + " (tick " + tickCounter + ") ===");
    }

    /* ════════════════════════════════════════════════════════════════════════
     * Helpers de coordination (avoidance + broadcast)
     * ════════════════════════════════════════════════════════════════════════ */

    /** Nœuds ciblés par les coéquipiers de nom lex-plus-petit (priorité). */
    private Set<String> computeAvoidSet() {
        String me = agent.getLocalName();
        return agent.getTeamIntentions().entrySet().stream()
                .filter(e -> e.getKey().compareTo(me) < 0)
                .map(Map.Entry::getValue)
                .filter(v -> v != null && !v.isEmpty())
                .collect(Collectors.toSet());
    }

    private void broadcastSighting(String myPos, Set<String> sPos, Set<String> sNeg) {
        HuntSightingMessage payload = new HuntSightingMessage(myPos, sPos, sNeg, tickCounter);
        sendToTeammates(HuntAgent.PROTO_SIGHTING, null, payload);
    }

    private void broadcastTopology(MapRepresentation map) {
        SerializableSimpleGraph<String, MapAttribute> sg = map.getSerializableGraph();
        sendToTeammates(MultiExploreAgent.PROTO_TOPO, null, sg);
    }

    private void broadcastIntent(String target) {
        sendToTeammates(MultiExploreAgent.PROTO_INTENT, target, null);
    }

    private void broadcastCaptured(String golemPos) {
        sendToTeammates(HuntAgent.PROTO_CAPTURED, golemPos, null);
    }

    /**
     * Envoie un INFORM à tous les coéquipiers connus du DF.
     */
    private void sendToTeammates(String protocol, String content, Serializable contentObject) {
        List<String> teammates = agent.getTeammates();
        if (teammates.isEmpty()) return;

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
        for (String t : teammates) {
            msg.addReceiver(new AID(t, AID.ISLOCALNAME));
        }
        agent.sendMessage(msg);
    }
}
