package eu.su.mas.dedaleEtu.mas.knowledge;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Inférence de la position d'un Golem (Wumpus) à partir des observations d'odeur.
 *
 * <h3>Modèle d'observation</h3>
 * Chaque Golem diffuse une odeur dans tous les nœuds situés à au plus {@code R}
 * sauts de sa position courante (R = rayon de diffusion, paramètre du scénario).
 * Sur un nœud observé :
 * <ul>
 *   <li><b>Stench présent</b> → le Golem est à distance ≤ R de ce nœud</li>
 *   <li><b>Stench absent</b> → le Golem est à distance &gt; R de ce nœud</li>
 * </ul>
 *
 * <h3>Inférence</h3>
 * Soit {@code S+} l'ensemble des nœuds stench-positifs et {@code S-} l'ensemble
 * des nœuds stench-négatifs observés ce tick (mutualisés entre coéquipiers).
 * Les positions compatibles sont :
 * <pre>
 *     candidats = ( ∩_{x ∈ S+} B(x, R) ) \ ( ∪_{y ∈ S-} B(y, R) )
 * </pre>
 * où {@code B(n, R)} est la boule de rayon {@code R} autour du nœud {@code n}.
 *
 * <h3>Mouvement entre deux ticks</h3>
 * Le Golem peut s'être déplacé d'un saut depuis le tick précédent. Avant
 * d'appliquer les nouvelles observations, on étend l'ensemble courant des
 * positions possibles en y ajoutant tous leurs voisins.
 *
 * <h3>Garanties</h3>
 * <ul>
 *   <li><b>Cohérence</b> : si la vraie position est dans l'ensemble de départ,
 *       elle reste dans l'ensemble après chaque mise à jour
 *       (preuve : intersection + différence préservent l'appartenance pour la
 *       vraie position, qui est bien dans toutes les boules positives et dans
 *       aucune boule négative).</li>
 *   <li><b>Décroissance</b> : à information fixe, l'ensemble ne grandit jamais
 *       (l'expansion par 1 saut ne survient qu'entre ticks).</li>
 *   <li><b>Localisation</b> : avec suffisamment d'observations, l'ensemble
 *       converge vers le singleton {@code {position_réelle}}.</li>
 * </ul>
 *
 * <h3>Complexité</h3>
 * Pour un graphe à {@code |V|} nœuds, max degré {@code d}, et |S+| + |S-| = m :
 * <ul>
 *   <li>BFS rayon R : O(d · |B(R)|) où |B(R)| ≤ d^R</li>
 *   <li>Mise à jour : O(m · |B(R)| + |candidats| · |B(R)|)</li>
 * </ul>
 * Mémoire : O(|candidats|).
 *
 * <h3>Limitation multi-Golems</h3>
 * Cette classe trace <b>un seul</b> Golem. Pour un scénario multi-Golems, créer
 * plusieurs instances et attribuer chaque odeur via clustering spatial des
 * nœuds stench-positifs (composantes connexes dans le sous-graphe induit).
 */
public class WumpusTracker implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Rayon de diffusion d'odeur (paramètre du scénario). */
    private final int diffusionRadius;

    /** Ensemble courant des positions possibles du Golem. */
    private Set<String> possiblePositions = new HashSet<>();

    /** Vrai tant qu'on n'a pas reçu la première observation contenant une odeur. */
    private boolean firstSighting = true;

    public WumpusTracker(int diffusionRadius) {
        if (diffusionRadius < 0) throw new IllegalArgumentException("radius < 0");
        this.diffusionRadius = diffusionRadius;
    }

    /**
     * Met à jour la croyance avec un nouveau lot d'observations.
     *
     * @param stenchPositive nœuds où le Stench a été observé ce tick
     * @param stenchNegative nœuds observés ce tick <em>sans</em> stench
     * @param map            connaissance topologique partagée (utilisée pour les BFS)
     */
    public void update(Set<String> stenchPositive,
                       Set<String> stenchNegative,
                       MapRepresentation map) {
        if (stenchPositive.isEmpty() && stenchNegative.isEmpty()) {
            // Aucune information nouvelle : on garde la croyance précédente.
            return;
        }

        // ── 1. Modèle de mouvement : avant d'appliquer la nouvelle obs, on
        //    autorise le Golem à avoir bougé d'un saut depuis le tick précédent.
        if (!firstSighting && !possiblePositions.isEmpty()) {
            Set<String> expanded = new HashSet<>(possiblePositions);
            for (String p : possiblePositions) {
                expanded.addAll(map.getNeighbors(p));
            }
            possiblePositions = expanded;
        }

        // ── 2. Calcul des nœuds compatibles avec ce lot d'observations.
        //    candidats = ( ∩ B(x, R) pour x ∈ S+ ) \ ( ∪ B(y, R) pour y ∈ S- )
        Set<String> compatible;

        if (firstSighting) {
            // Initialisation : on doit considérer tous les nœuds connus.
            // Optimisation : si S+ non-vide, on initialise à B(premier_S+, R)
            //                puis on intersecte avec les autres B(x, R).
            if (!stenchPositive.isEmpty()) {
                String first = stenchPositive.iterator().next();
                compatible = new HashSet<>(map.getNodesWithinRadius(first, diffusionRadius));
                for (String x : stenchPositive) {
                    if (x.equals(first)) continue;
                    compatible.retainAll(map.getNodesWithinRadius(x, diffusionRadius));
                }
            } else {
                // S+ vide mais S- non-vide : on part de tous les nœuds connus.
                compatible = new HashSet<>(map.getAllNodes());
            }
            firstSighting = false;
        } else {
            // On restreint l'ensemble (déjà étendu) aux nœuds compatibles.
            compatible = new HashSet<>(possiblePositions);
            for (String x : stenchPositive) {
                compatible.retainAll(map.getNodesWithinRadius(x, diffusionRadius));
            }
        }

        // ── 3. Soustraction des boules négatives.
        for (String y : stenchNegative) {
            compatible.removeAll(map.getNodesWithinRadius(y, diffusionRadius));
        }

        possiblePositions = compatible;
    }

    /** Snapshot non-modifiable de l'ensemble courant des positions possibles. */
    public Set<String> getPossiblePositions() {
        return new HashSet<>(possiblePositions);
    }

    /** {@code true} si la position est connue avec certitude (singleton). */
    public boolean isLocalised() {
        return possiblePositions.size() == 1;
    }

    /** Nombre de positions candidates. 0 = aucune information ou perdu la trace. */
    public int candidateCount() {
        return possiblePositions.size();
    }

    /**
     * Heuristique : retourne une position « représentative » des candidats.
     * Si l'ensemble est singleton, c'est la vraie position.
     *
     * @return un identifiant de nœud, ou {@code null} si aucun candidat
     */
    public String getRepresentativePosition() {
        return possiblePositions.isEmpty() ? null : possiblePositions.iterator().next();
    }

    /** {@code true} si on a reçu au moins une observation depuis le dernier reset. */
    public boolean hasAnyObservation() {
        return !firstSighting;
    }

    /** Réinitialise le tracker (par ex. après une capture pour traquer un autre Golem). */
    public void reset() {
        possiblePositions.clear();
        firstSighting = true;
    }

    public int getDiffusionRadius() {
        return diffusionRadius;
    }

    @Override
    public String toString() {
        return "WumpusTracker{R=" + diffusionRadius
                + ", candidates=" + possiblePositions.size()
                + ", positions=" + possiblePositions + "}";
    }
}
