package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.su.mas.dedaleEtu.mas.behaviours.HuntBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveHuntBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.WumpusTracker;

import jade.core.behaviours.Behaviour;

/**
 * Agent chasseur — extension de {@link MultiExploreAgent} pour la <b>section 3</b>
 * du sujet de projet (patrouille et capture du Golem).
 *
 * <h3>Capacités héritées de {@link MultiExploreAgent}</h3>
 * <ul>
 *   <li>Découverte des coéquipiers via le DF (service {@code "explo-team"})</li>
 *   <li>Partage de la topologie ({@code MEXPLO-TOPO})</li>
 *   <li>Partage des intentions ({@code MEXPLO-INTENT})</li>
 *   <li>Vote de terminaison d'exploration ({@code MEXPLO-DONE} / {@code MEXPLO-UNDONE})</li>
 * </ul>
 *
 * <h3>Capacités ajoutées</h3>
 * <ul>
 *   <li><b>Tracker</b> ({@link WumpusTracker}) : infère les positions possibles
 *       du Golem à partir des odeurs perçues</li>
 *   <li><b>Protocole {@value #PROTO_SIGHTING}</b> : partage des odeurs observées
 *       chaque tick (positives et négatives)</li>
 *   <li><b>Protocole {@value #PROTO_CAPTURED}</b> : annonce que le Golem a été
 *       capturé (tous ses voisins occupés par des chasseurs)</li>
 *   <li><b>Cache des positions de coéquipiers</b> : reçues via les sightings,
 *       servent au {@code BlockingPlanner} pour l'assignation des rôles</li>
 * </ul>
 *
 * <h3>Paramètres utilisateur</h3>
 * Le rayon de diffusion d'odeur du Golem peut être passé via {@code userParameters}
 * dans le JSON :
 * <pre>"userParameters": ["stenchRadius=2"]</pre>
 * Valeur par défaut : 1 (Stench observé uniquement dans les voisins immédiats).
 */
public class HuntAgent extends MultiExploreAgent {

    private static final long serialVersionUID = 1L;

    // ── Protocoles ACL spécifiques à la chasse ────────────────────────────────
    /** Diffusion des observations d'odeur (payload : {@code HuntSightingMessage}). */
    public static final String PROTO_SIGHTING = "HUNT-SIGHTING";
    /** Annonce de capture du Golem (contenu : id du nœud où il est bloqué). */
    public static final String PROTO_CAPTURED = "HUNT-CAPTURED";

    /** Rayon de diffusion d'odeur par défaut (cf. {@code DEFAULT_DETECTION_RADIUS} dans {@code ConfigurationFile}). */
    public static final int DEFAULT_STENCH_RADIUS = 1;

    /** Tracker du Golem ; instancié dans {@link #setup()}. */
    private WumpusTracker tracker;

    /**
     * Position connue de chaque coéquipier (mise à jour par les sightings reçus).
     * Clé = nom local, Valeur = id du nœud courant.
     */
    private final Map<String, String> teamPositions = new ConcurrentHashMap<>();

    /** Vrai dès qu'un message {@code HUNT-CAPTURED} a été émis ou reçu. */
    private volatile boolean huntComplete = false;

    /** Position où le Golem a été capturé (pour le log/rapport). */
    private volatile String captureLocation = null;

    @Override
    protected void setup() {
        // Lecture du paramètre stenchRadius (si présent) avant l'initialisation du tracker.
        int radius = parseStenchRadius(getArguments());
        this.tracker = new WumpusTracker(radius);
        super.setup(); // déclenche createBehaviours() de cette sous-classe
        System.out.println("[" + getLocalName() + "] HuntAgent — rayon d'odeur supposé = " + radius);
    }

    /**
     * Surcharge le hook de {@link MultiExploreAgent} : on installe les comportements
     * de chasse à la place de l'exploration pure.
     */
    @Override
    protected List<Behaviour> createBehaviours() {
        List<Behaviour> lb = new ArrayList<>();
        lb.add(new ReceiveHuntBehaviour(this));
        lb.add(new HuntBehaviour(this));
        return lb;
    }

    /**
     * Lit {@code stenchRadius=N} dans les userParameters JSON, sinon retourne le défaut.
     * Format attendu dans le JSON : {@code "userParameters": ["stenchRadius=2", "..."]}.
     */
    private int parseStenchRadius(Object[] args) {
        if (args == null) return DEFAULT_STENCH_RADIUS;
        for (Object a : args) {
            if (a instanceof String) {
                String s = (String) a;
                if (s.startsWith("stenchRadius=")) {
                    try {
                        return Integer.parseInt(s.substring("stenchRadius=".length()).trim());
                    } catch (NumberFormatException ignored) {
                        // valeur invalide → on garde le défaut
                    }
                }
            }
        }
        return DEFAULT_STENCH_RADIUS;
    }

    // ── Accesseurs ────────────────────────────────────────────────────────────

    public WumpusTracker getTracker() {
        return tracker;
    }

    public Map<String, String> getTeamPositions() {
        return teamPositions;
    }

    public boolean isHuntComplete() {
        return huntComplete;
    }

    public void setHuntComplete(boolean complete) {
        this.huntComplete = complete;
    }

    public String getCaptureLocation() {
        return captureLocation;
    }

    public void setCaptureLocation(String node) {
        this.captureLocation = node;
    }
}
