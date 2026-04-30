package eu.su.mas.dedaleEtu.mas.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import eu.su.mas.dedale.mas.AbstractDedaleAgent;
import eu.su.mas.dedale.mas.agent.behaviours.platformManagment.StartMyBehaviours;
import eu.su.mas.dedaleEtu.mas.behaviours.MultiExploreBehaviour;
import eu.su.mas.dedaleEtu.mas.behaviours.ReceiveShareBehaviour;
import eu.su.mas.dedaleEtu.mas.knowledge.MapRepresentation;

import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

/**
 * Agent explorateur coopératif — FoSyMa TP Section 3.2.
 *
 * <p>Corrige les défauts documentés d'{@code ExploreCoopAgent} :
 * <ul>
 *   <li><b>Découverte via le DF</b> : les coéquipiers sont trouvés dynamiquement
 *       (service type {@value #SERVICE_TYPE}) — aucun nom codé en dur, fonctionne
 *       pour N agents quelconque.</li>
 *   <li><b>Broadcast d'intention</b> : chaque agent annonce son nœud cible via
 *       {@value #PROTO_INTENT}. Les agents de nom lexicographiquement plus grand
 *       cèdent la priorité, éliminant la convergence sur le même nœud ouvert.</li>
 *   <li><b>Terminaison par vote</b> : protocole DONE / UNDONE à 2 phases avec
 *       re-broadcast périodique pour les retardataires hors de portée.</li>
 * </ul>
 *
 * <h3>Comportements installés</h3>
 * <ol>
 *   <li>{@link ReceiveShareBehaviour} — dispatcher cyclique pour les 4 protocoles MEXPLO-*.</li>
 *   <li>{@link MultiExploreBehaviour} — boucle de déplacement cadencée (800 ms/tick).</li>
 * </ol>
 */
public class MultiExploreAgent extends AbstractDedaleAgent {

    private static final long serialVersionUID = 1L;

    // ── Type de service DF pour la découverte des coéquipiers ─────────────────
    /** Service type enregistré dans le DF Jade pour ce groupe d'agents. */
    public static final String SERVICE_TYPE  = "explo-team";

    // ── Identificateurs de protocoles ACL ─────────────────────────────────────
    /** Topologie sérialisée complète (payload : {@code SerializableSimpleGraph}). */
    public static final String PROTO_TOPO    = "MEXPLO-TOPO";
    /** Nœud cible courant (contenu String). Chaîne vide = agent inactif. */
    public static final String PROTO_INTENT  = "MEXPLO-INTENT";
    /** L'émetteur n'a plus de nœud ouvert — exploration locale terminée. */
    public static final String PROTO_DONE    = "MEXPLO-DONE";
    /** L'émetteur retire son vote DONE (nouvelle topologie reçue). */
    public static final String PROTO_UNDONE  = "MEXPLO-UNDONE";

    // ── État partagé entre les deux comportements ─────────────────────────────

    /** Connaissance topologique locale. Initialisée paresseusement au 1er observe(). */
    private MapRepresentation myMap;

    /**
     * Dernière intention déclarée par chaque coéquipier.
     * Clé = nom local, valeur = id du nœud cible.
     * {@code ConcurrentHashMap} car {@link ReceiveShareBehaviour} et
     * {@link MultiExploreBehaviour} s'entrelacent dans le modèle coopératif de Jade.
     */
    private final Map<String, String> teamIntentions = new ConcurrentHashMap<>();

    /**
     * Noms des agents ayant diffusé un vote DONE.
     * La terminaison globale est atteinte quand cet ensemble contient tous les coéquipiers.
     */
    private final Set<String> terminationVotes = ConcurrentHashMap.newKeySet();

    /** {@code true} si cet agent a lui-même diffusé DONE. */
    private boolean iAmDone = false;

    // ── Cache de la liste des coéquipiers ─────────────────────────────────────

    /** Résultat mis en cache du dernier accès au DF. */
    private List<String> cachedTeammates = null;
    /** Timestamp (ms) du dernier accès au DF. */
    private long teammateCacheTime = 0L;
    /**
     * Durée de validité du cache en millisecondes.
     * 5 s est un bon compromis : plus court = trop de requêtes DF,
     * plus long = liste obsolète si un agent rejoint ou quitte la plateforme.
     */
    private static final long TEAMMATE_CACHE_TTL_MS = 5_000L;

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    @Override
    protected void setup() {
        super.setup();
        registerInDF();

        addBehaviour(new StartMyBehaviours(this, createBehaviours()));

        System.out.println("[" + getLocalName() + "] " + getClass().getSimpleName() + " démarré.");
    }

    /**
     * Hook pour les sous-classes : retourne la liste des comportements à installer
     * une fois l'agent enregistré dans le DF.
     *
     * <p>Par défaut : {@link ReceiveShareBehaviour} (réception des protocoles
     * MEXPLO-*) + {@link MultiExploreBehaviour} (boucle d'exploration coordonnée).
     * Surcharger pour spécialiser l'agent (ex. {@code HuntAgent} y ajoute la chasse).
     */
    protected List<Behaviour> createBehaviours() {
        List<Behaviour> lb = new ArrayList<>();
        lb.add(new ReceiveShareBehaviour(this));
        lb.add(new MultiExploreBehaviour(this));
        return lb;
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ignored) {
            // Désenregistrement best-effort : la plateforme peut déjà être en arrêt.
        }
        super.takeDown();
    }

    // ── Enregistrement DF ─────────────────────────────────────────────────────

    /**
     * Enregistre cet agent dans le DF sous le type {@value #SERVICE_TYPE}.
     * Appelé une seule fois lors de {@link #setup()}.
     */
    private void registerInDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    // ── Accesseurs publics ────────────────────────────────────────────────────

    /**
     * Retourne la liste des coéquipiers actifs (autres agents enregistrés sous
     * {@value #SERVICE_TYPE} dans le DF).
     *
     * <p>Le résultat est mis en cache pendant {@value #TEAMMATE_CACHE_TTL_MS} ms afin
     * d'éviter un aller-retour DF à chaque tick d'exploration.
     *
     * @return snapshot mutable des noms de coéquipiers (jamais null)
     */
    public List<String> getTeammates() {
        long now = System.currentTimeMillis();
        if (cachedTeammates != null && (now - teammateCacheTime) < TEAMMATE_CACHE_TTL_MS) {
            return new ArrayList<>(cachedTeammates); // retourne une copie défensive
        }

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(SERVICE_TYPE);
        template.addServices(sd);

        List<String> result = new ArrayList<>();
        try {
            DFAgentDescription[] found = DFService.search(this, template);
            for (DFAgentDescription d : found) {
                String name = d.getName().getLocalName();
                if (!name.equals(getLocalName())) {
                    result.add(name);
                }
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Mise à jour du cache
        cachedTeammates = result;
        teammateCacheTime = now;
        return new ArrayList<>(result);
    }

    /**
     * Retourne la connaissance topologique locale, ou {@code null} avant le
     * premier appel à {@link #initMap()}.
     */
    public MapRepresentation getMyMap() {
        return myMap;
    }

    /**
     * Initialise la carte locale lors du premier appel.
     * Les appels suivants sont des no-ops (idempotent).
     */
    public void initMap() {
        if (myMap == null) {
            myMap = new MapRepresentation(getLocalName());
        }
    }

    /**
     * Retourne la map mutable des intentions des coéquipiers.
     * Clé = nom local du coéquipier, valeur = id du nœud cible.
     */
    public Map<String, String> getTeamIntentions() {
        return teamIntentions;
    }

    /**
     * Retourne l'ensemble des agents (dont soi-même) ayant diffusé un vote DONE.
     */
    public Set<String> getTerminationVotes() {
        return terminationVotes;
    }

    /** @return {@code true} si cet agent a diffusé son propre vote DONE. */
    public boolean isIAmDone() {
        return iAmDone;
    }

    /** Met à jour l'état DONE de cet agent. */
    public void setIAmDone(boolean done) {
        this.iAmDone = done;
    }
}
