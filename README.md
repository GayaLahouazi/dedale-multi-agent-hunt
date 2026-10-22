# Multi-agent exploration and Golem hunt (JADE / Dedale)

![java](https://img.shields.io/badge/Java-21-orange) ![jade](https://img.shields.io/badge/JADE-multi--agent-blue) ![graphstream](https://img.shields.io/badge/GraphStream-2.0-lightgrey)

Course project: FoSyMa (*Foundations of Multi-Agent Systems*), M1 AI2D, Sorbonne Université, 2025–2026.
Supervisor: Cédric Herpson.

**Key result:** decentralised agents explore an unknown graph and catch a Golem they never see directly, with no
leader and no negotiation round. They pool stench observations into an exact set of possible Golem positions, then all
compute the same blocking assignment.

**Problem.** A team of autonomous agents starts in an unknown environment (an undirected graph). The agents must
(1) explore it cooperatively, then (2) find and block a Golem (a "Wumpus") that moves and is never directly visible.
Its presence is betrayed only by the stench it spreads within R hops. There is no central coordinator, the
communication range is limited and the agents act asynchronously.

## My contribution

About 1,500 lines of Java on top of the Dedale framework (package `eu.su.mas.dedaleEtu.mas`):

| Component | Files | What it does |
|---|---|---|
| Coordinated exploration | `agents/MultiExploreAgent`, `behaviours/MultiExploreBehaviour` | DFS with shared intentions, lexicographic-priority avoidance, 2-phase DONE / UNDONE termination vote |
| Communication | `behaviours/ReceiveShareBehaviour`, `behaviours/ReceiveHuntBehaviour` | 6 FIPA-ACL protocols (map, intention, done, undone, sighting, captured), one dispatcher per agent, teammates discovered through the JADE Directory Facilitator |
| Golem tracking | `knowledge/WumpusTracker`, `knowledge/HuntSightingMessage` | exact set of Golem positions consistent with all stench observations of the team |
| Capture | `agents/HuntAgent`, `behaviours/HuntBehaviour` | explore → track → block phases, deterministic hunter → neighbour assignment |
| Map | `knowledge/MapRepresentation` (extended) | radius-R balls, shortest paths, map merging |

## How it works

**1. Exploration without a leader.** Each agent closes its current node, opens its unknown neighbours, broadcasts
its map and announces its next target. Agent *i* avoids every target announced by a teammate *j* with
`name(j) < name(i)`: the rule is deterministic, asymmetric and needs no consensus round. If every option is avoided,
the agent falls back to any open neighbour (no deadlock), then to Dijkstra towards the nearest open node.
Termination: an agent votes DONE when its map has no open node, and retracts (UNDONE) if a received map reopens
nodes. Exploration ends when every teammate has voted.

**2. Tracking a Golem nobody can see.** `P` is the set of candidate positions. At each tick:

```
P ← (P ∪ N(P))  ∩  ⋂_{x ∈ S+} B(x, R)  \  ⋃_{y ∈ S−} B(y, R)
```

The Golem may have moved one hop. It is within R hops of every node where stench was smelled (S+), and farther
than R from every node without stench (S−). Observations from all hunters are pooled through `HUNT-SIGHTING`
messages. Invariant: the true position always stays in `P`. The Golem is localised when `|P| = 1`.

**3. Blocking.** Once the Golem is localised at node *g*, every hunter computes the *same* hunter → neighbour-of-*g*
assignment (double lexicographic sort), so no extra message is needed. Each hunter reaches its slot with Dijkstra.
Capture is detected when every neighbour of *g* is occupied, and a `HUNT-CAPTURED` message stops the team.

## Run it

Java 21 and Maven:

```bash
cd dedale-etu
mvn compile exec:java
```

Choose the scenario in `src/main/java/eu/su/mas/dedaleEtu/princ/ConfigurationFile.java`:
`INSTANCE_TOPOLOGY` (the map) and `INSTANCE_CONFIGURATION_ENTITIES` (the agents). For example,
`resources/agent-3hunt.json` gives 3 hunters and 1 Golem, and `resources/agent-7hunters-3golems.json` gives
7 hunters and 3 Golems.

## Limits and next steps

* Validated by running the course scenarios and watching the agents. There are no automated tests or quantitative
  benchmarks yet. Next: JUnit tests for `WumpusTracker` and `MapRepresentation`, and capture-time
  statistics over many random maps.
* The full map is broadcast at every tick, so message size grows with the graph. Delta-sharing would scale better.
* The tracker assumes a single Golem. With several Golems, the next step is one tracker per stench cluster.
* Blocking needs deg(*g*) hunters, and the greedy assignment is not cost-optimal. A Hungarian matching would fix
  the cost; push-to-leaf on trees needs a single hunter.

The full report (in French, with UML diagrams and complexity analysis) is in
[`docs/report_FoSyMa_2026_FR.pdf`](docs/report_FoSyMa_2026_FR.pdf).

## Credits

The Dedale environment and the course framework are by Cédric Herpson (LIP6, Sorbonne Université):
<https://dedale.gitlab.io>. Commits before April 2026 come from the course template. My work is in the files
listed above.

---
Author: **Gaya Lahouazi**, M2 AI2D (Sorbonne Université).
