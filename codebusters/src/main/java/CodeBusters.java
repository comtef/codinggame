import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in
                .nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right

        GameState gameState = new GameState(myTeamId == 0 ? 0 : 16000, myTeamId == 0 ? 0 : 9000, bustersPerPlayer,
                ghostCount);

        // game loop
        while (true) {
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            gameState.resetState();
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in
                        .nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to
                // trap this ghost.

                if (entityType == myTeamId) {
                    gameState.updateBuster(new Buster(entityId, x, y, state, state == 1 ? value : -1));
                } else if (entityType == -1) {
                    gameState.updateGhost(new Ghost(entityId, x, y, value));
                } else {
                    gameState.updateEnemyBuster(new Buster(entityId, x, y, state, state == 1 ? value : -1));
                }
            }

            // Debug state
            //System.err.println(gameState);

            List<Ghost> huntedGhosts = new ArrayList<>();

            gameState.myBusters.keySet().stream().sorted().forEach(
                    busterId -> {
                        Buster currentBuster = gameState.getBuster(busterId);
                        Buster enemyInStunRange = currentBuster
                                .canStunAnEnemy(gameState.getVisibleAndActiveEnnemyBusters());
                        if (enemyInStunRange != null) {
                            currentBuster.initializeStunReload();
                            System.out.println("STUN " + enemyInStunRange.id);
                        } else if (currentBuster.isCarryingAGhost()) {
                            // Release ghost if close enough
                            if (currentBuster.isCloseEnoughToBase(gameState.baseX, gameState.baseY)) {
                                System.out.println("RELEASE");
                            } else {
                                // Go back to base
                                System.out.println("MOVE " + gameState.baseX + " " + gameState.baseY);
                            }
                        } else {
                            Ghost ghost = currentBuster.canCatchAGhost(gameState.getVisibleGhosts(), huntedGhosts);
                            if (ghost != null) {
                                huntedGhosts.add(ghost);
                                System.out.println("BUST " + ghost.id);
                            } else {
                                ghost = currentBuster
                                        .findClosestAvailableGhost(gameState.getVisibleGhosts(), huntedGhosts);
                                if (ghost != null) {
                                    System.out.println("MOVE " + ghost.x + " " + ghost.y);
                                } else {
                                    if (currentBuster.destination == null || currentBuster.destinationReached()) {
                                        currentBuster.goToNextDestination(gameState.busterCount,
                                                currentBuster.id % gameState.busterCount,
                                                gameState.baseX == 0);
                                    }
                                    System.out.println(
                                            "MOVE " + currentBuster.destination[0] + " "
                                                    + currentBuster.destination[1]);

                                }
                            }
                        }
                    }
);
        }
    }
}

class GameParameters {
    public static int WIDTH = 16000;
    public static int HEIGHT = 9000;

    public static int RELEASE_MAX_DISTANCE = 1600;

    public static int CATCH_MIN_DISTANCE = 900;
    public static int CATCH_MAX_DISTANCE = 1760;

    public static int FLEE_DISTANCE = 400;

    public static int STUN_MAX_DISTANCE = 1760;

    public static Map<Integer, List<List<int[]>>> POSITIONS_FROM_TOP_LEFT = new HashMap<>();

    static {
        POSITIONS_FROM_TOP_LEFT.put(2, Arrays.asList(
                Arrays.asList(
                        new int[] { 15000, 1000 },
                        new int[] { 12000, 6750 },
                        new int[] { 10000, 3000 },
                        new int[] { 4000, 3000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 1000, 8000 },
                        new int[] { 15000, 6750 },
                        new int[] { 10000, 5500 },
                        new int[] { 4000, 5500 },
                        new int[] { 1000, 1000 })));

        POSITIONS_FROM_TOP_LEFT.put(3, Arrays.asList(
                Arrays.asList(
                        new int[] { 15000, 1000 },
                        new int[] { 15000, 3000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 15000, 5000 },
                        new int[] { 12000, 8000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 9000, 8000 },
                        new int[] { 1000, 8000 },
                        new int[] { 1000, 1000 })));

        POSITIONS_FROM_TOP_LEFT.put(4, Arrays.asList(
                Arrays.asList(
                        new int[] { 15000, 1000 },
                        new int[] { 15000, 2000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 15000, 4000 },
                        new int[] { 15000, 8000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 14000, 8000 },
                        new int[] { 8000, 8000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 4000, 8000 },
                        new int[] { 1000, 8000 },
                        new int[] { 1000, 1000 })));

        POSITIONS_FROM_TOP_LEFT.put(4, Arrays.asList(
                Arrays.asList(
                        new int[] { 15000, 1000 },
                        new int[] { 15000, 2000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 15000, 4000 },
                        new int[] { 15000, 8000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 14000, 8000 },
                        new int[] { 8000, 8000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 4000, 8000 },
                        new int[] { 1000, 8000 },
                        new int[] { 1000, 1000 }),
                Arrays.asList(
                        new int[] { 15000, 8000 },
                        new int[] { 1000, 1000 })));
    }

    public static List<int[]> EXPLORATION_DESTINATIONS_FROM_TOP_LEFT = Arrays.asList(
            new int[]{14000, 3000},
            new int[]{9000, 8000},
            new int[]{GameParameters.WIDTH, GameParameters.HEIGHT},
            new int[]{15000, 1000},
            new int[]{1000, 8000}
    );

    public static List<int[]> EXPLORATION_DESTINATIONS_FROM_BOTTOM_RIGHT = Arrays.asList(
            new int[]{8000, 1000},
            new int[]{1000, 3000},
            new int[]{0, 0},
            new int[]{15000, 1000},
            new int[]{1000, 8000}
    );
}

class Entity {
    int id;
    int x;
    int y;
    public boolean visible = true;

    public Entity(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int distanceToPoint(int x, int y) {
        return (int) Math.hypot(this.x - x, this.y - y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity entity = (Entity) o;
        return id == entity.id &&
                x == entity.x &&
                y == entity.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y);
    }
}

class Buster extends Entity {
    int ghostCarried;
    int state;
    int[] destination;
    private int stunReload = 0;
    private int destinationIndex = 0;

    public Buster(int id, int x, int y, int state, int ghostCarried) {
        super(id, x, y);
        this.state = state;
        this.ghostCarried = ghostCarried;
    }

    public boolean isCarryingAGhost() {
        return ghostCarried >= 0;
    }

    public boolean isCloseEnoughToBase(int baseX, int baseY) {
        return distanceToPoint(baseX, baseY) <= GameParameters.RELEASE_MAX_DISTANCE;
    }

    public Ghost canCatchAGhost(List<Ghost> ghosts, List<Ghost> huntedGhosts) {
        return ghosts.stream().filter(ghost -> isInCatchRange(ghost) && !huntedGhosts.contains(ghost)).findFirst()
                .orElse(null);
    }

    public boolean isInCatchRange(Ghost ghost) {
        int distance = distanceToPoint(ghost.x, ghost.y);
        return distance > GameParameters.CATCH_MIN_DISTANCE && distance < GameParameters.CATCH_MAX_DISTANCE;
    }

    public Ghost findClosestAvailableGhost(List<Ghost> ghosts, List<Ghost> huntedGhosts) {
        List<Ghost> remainingGhosts = new ArrayList<>(ghosts);
        remainingGhosts.removeAll(huntedGhosts);
        return remainingGhosts.stream().sorted((g1, g2) -> Integer.compare(distanceToPoint(g1.x, g1.y), distanceToPoint(g2.x, g2.y))).findFirst().orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Buster buster = (Buster) o;
        return ghostCarried == buster.ghostCarried;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ghostCarried);
    }

    public Buster canStunAnEnemy(List<Buster> enemyBusters) {
        return stunReload == 0 ? enemyBusters.stream()
                .filter(buster -> buster.distanceToPoint(x, y) <= GameParameters.STUN_MAX_DISTANCE)
                .findFirst().orElse(null) : null;
    }

    public void update(Buster buster) {
        x = buster.x;
        y = buster.y;
        state = buster.state;
        ghostCarried = buster.ghostCarried;
    }

    public int[] getDestinationToExplore(int buster, boolean topLeft) {
        if (topLeft) {
            return GameParameters.EXPLORATION_DESTINATIONS_FROM_TOP_LEFT.get(buster);
        } else {
            return GameParameters.EXPLORATION_DESTINATIONS_FROM_BOTTOM_RIGHT.get(buster);
        }
    }

    public boolean destinationReached() {
        return x == destination[0] && y == destination[1];
    }

    public void setDestination(int[] positionToReach) {
        destination = positionToReach;
    }

    public void initializeStunReload() {
        this.stunReload = 20;
    }

    public void updateStunReload() {
        if (stunReload > 0) {
            stunReload--;
        }
    }

    public void goToNextDestination(int busterCount, int busterIndex, boolean fromTopLeft) {
        List<int[]> destinations = GameParameters.POSITIONS_FROM_TOP_LEFT.get(busterCount).get(busterIndex);
        destination = destinations.get(destinationIndex++ % destinations.size());
    }
}

class Ghost extends Entity {
    int attackIntensity;

    public Ghost(int id, int x, int y, int attackIntensity) {
        super(id, x, y);
        this.attackIntensity = attackIntensity;
    }

    public void update(Ghost ghost) {
        x = ghost.x;
        y = ghost.y;
        visible = ghost.visible;
        attackIntensity = ghost.attackIntensity;
    }
}

class GameState {
    int baseX;
    int baseY;
    int busterCount;
    int ghostCount;

    public GameState(int baseX, int baseY, int busterCount, int ghostCount) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.busterCount = busterCount;
        this.ghostCount = ghostCount;
    }

    Map<Integer, Buster> myBusters = new HashMap<>();
    Map<Integer, Buster> enemyBusters = new HashMap<>();
    Map<Integer, Ghost> ghosts = new HashMap<>();

    public void resetState() {
        enemyBusters.forEach((id, buster) -> buster.visible = false);
        ghosts.forEach((id, ghost) -> ghost.visible = false);

    }

    public Buster getBuster(int i) {
        return myBusters.get(i);
    }

    @Override
    public String toString() {
        return "GameState{" +
                "baseX=" + baseX +
                ", baseY=" + baseY +
                ", ghostCount=" + ghostCount +
                ", busterCount=" + busterCount +
                '}';
    }

    public boolean isBaseTopLeft() {
        return baseX == 0;
    }

    public void updateBuster(Buster buster) {
        if (myBusters.containsKey(buster.id)) {
            myBusters.get(buster.id).update(buster);
        } else {
            myBusters.put(buster.id, buster);
        }

        myBusters.get(buster.id).updateStunReload();
    }

    public void updateGhost(Ghost ghost) {
        ghost.visible = true;
        if (ghosts.containsKey(ghost.id)) {
            ghosts.get(ghost.id).update(ghost);
        } else {
            ghosts.put(ghost.id, ghost);
        }
    }

    public void updateEnemyBuster(Buster buster) {
        buster.visible = true;
        if (enemyBusters.containsKey(buster.id)) {
            enemyBusters.get(buster.id).update(buster);
        } else {
            enemyBusters.put(buster.id, buster);
        }
    }

    public List<Buster> getVisibleAndActiveEnnemyBusters() {
        return enemyBusters.values().stream().filter(buster -> buster.visible && buster.state != 2).collect(
                Collectors.toList());
    }

    public List<Ghost> getVisibleGhosts() {
        return ghosts.values().stream().filter(ghost -> ghost.visible).collect(Collectors.toList());
    }
}