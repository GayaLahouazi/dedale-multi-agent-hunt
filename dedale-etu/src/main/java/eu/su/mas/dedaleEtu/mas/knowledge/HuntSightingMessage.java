package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Message échangé entre chasseurs (protocole {@code HUNT-SIGHTING}) :
 * un agent diffuse à chaque tick l'ensemble des nœuds où il a perçu
 * (ou non perçu) une odeur de Golem, ainsi que sa position courante.
 *
 * <p>Le récepteur fusionne ces observations dans son propre {@link WumpusTracker}.
 * Sa position courante sert au {@code BlockingPlanner} pour assigner les rôles.
 *
 * <p>Champs publics : c'est un POJO de transport, pas de logique.
 */
public class HuntSightingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Position courante de l'émetteur (id de nœud), peut être {@code null} si non placé. */
    public String senderPosition;

    /** Nœuds où l'émetteur a observé du {@code Stench} ce tick. */
    public Set<String> stenchPositive = new HashSet<>();

    /** Nœuds visibles à l'émetteur ce tick <em>sans</em> {@code Stench} observé. */
    public Set<String> stenchNegative = new HashSet<>();

    /** Numéro de tick côté émetteur (optionnel, sert au log et au debug). */
    public long tick;

    public HuntSightingMessage() {
        // Constructeur sans argument requis pour la sérialisation Java standard.
    }

    public HuntSightingMessage(String senderPosition,
                               Set<String> stenchPositive,
                               Set<String> stenchNegative,
                               long tick) {
        this.senderPosition = senderPosition;
        this.stenchPositive = (stenchPositive != null) ? new HashSet<>(stenchPositive) : new HashSet<>();
        this.stenchNegative = (stenchNegative != null) ? new HashSet<>(stenchNegative) : new HashSet<>();
        this.tick = tick;
    }

    @Override
    public String toString() {
        return "HuntSighting{pos=" + senderPosition
                + ", S+=" + stenchPositive
                + ", S-=" + stenchNegative
                + ", t=" + tick + "}";
    }
}
