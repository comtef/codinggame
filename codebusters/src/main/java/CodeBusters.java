import java.util.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right

        GameState gameState = new GameState(myTeamId == 0 ? 0 : 16000, myTeamId == 0 ? 0 : 9000, bustersPerPlayer, ghostCount);

        // game loop
        while (true) {
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            gameState.clearState();
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.

                if (entityType == myTeamId) {
                    gameState.addBuster(new Buster(entityId, x, y, state == 1 ? value : -1));
                } else if (entityType == -1) {
                    gameState.addGhost(new Ghost(entityId, x, y, value));
                } else {
                    gameState.addEnemyBuster(new Buster(entityId, x, y, state == 1 ? value : -1));
                }
            }

            // Debug state
            //System.err.println(gameState);

            List<Ghost> huntedGhosts = new ArrayList<>();

            for (int i = 0; i < bustersPerPlayer; i++) {

                Buster currentBuster = gameState.getBuster(i);

                if (currentBuster.isCarryingAGhost()) {
                    // Release ghost if close enough
                    if (currentBuster.isCloseEnoughToBase(gameState.baseX, gameState.baseY)) {
                        System.out.println("RELEASE");
                    } else {
                        // Go back to base
                        System.out.println("MOVE " + gameState.baseX + " " + gameState.baseY);
                    }
                } else {
                    Ghost ghost = currentBuster.canCatchAGhost(gameState.ghosts);
                    if (ghost != null) {
                        huntedGhosts.add(ghost);
                        System.out.println("BUST " + ghost.id);
                    } else {
                        ghost = currentBuster.findClosestAvailableGhost(gameState.ghosts, huntedGhosts);
                        if (ghost != null) {
                            System.out.println("MOVE " + ghost.x + " " + ghost.y);
                        } else {
                            int[] positionToReach = currentBuster.getDestinationToExplore(i, gameState.isBaseTopLeft());
                            System.out.println("MOVE " + positionToReach[0] + " " + positionToReach[1]);
                        }
                    }
                }
            }
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

    public Buster(int id, int x, int y, int ghostCarried) {
        super(id, x, y);
        this.ghostCarried = ghostCarried;
    }

    public boolean isCarryingAGhost() {
        return ghostCarried > 0;
    }

    public boolean isCloseEnoughToBase(int baseX, int baseY) {
        return distanceToPoint(baseX, baseY) <= GameParameters.RELEASE_MAX_DISTANCE;
    }

    public Ghost canCatchAGhost(List<Ghost> ghosts) {
        return ghosts.stream().filter(ghost -> isInCatchRange(ghost)).findFirst().orElse(null);
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

    public int[] getDestinationToExplore(int buster, boolean topLeft) {
        if (topLeft) {
            return GameParameters.EXPLORATION_DESTINATIONS_FROM_TOP_LEFT.get(buster);
        } else {
            return GameParameters.EXPLORATION_DESTINATIONS_FROM_BOTTOM_RIGHT.get(buster);
        }
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
}

class Ghost extends Entity {
    int attackIntensity;

    public Ghost(int id, int x, int y, int attackIntensity) {
        super(id, x, y);
        this.attackIntensity = attackIntensity;
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

    List<Buster> myBusters = new ArrayList<>();
    List<Buster> ennemyBusters = new ArrayList<>();
    List<Ghost> ghosts = new ArrayList<>();

    public void clearState() {
        myBusters.clear();
        ennemyBusters.clear();
        ghosts.clear();
    }

    public void addBuster(Buster buster) {
        myBusters.add(buster);
    }

    public void addEnemyBuster(Buster buster) {
        ennemyBusters.add(buster);
    }

    public void addGhost(Ghost ghost) {
        ghosts.add(ghost);
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
}