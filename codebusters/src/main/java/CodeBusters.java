import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
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
                    gameState.updateBuster(new Buster(entityId, x, y, entityType, state, value));
                } else if (entityType == -1) {
                    gameState.updateGhost(new Ghost(entityId, x, y, entityType, state, value));
                } else {
                    gameState.updateEnemyBuster(new Buster(entityId, x, y, entityType, state, value));
                }
            }

            gameState.updateMoves();

            gameState.myBusters.keySet().stream().sorted().forEach(busterId -> {
                Buster buster = gameState.myBusters.get(busterId);
                System.out.println(buster.move);
            });
        }
    }
}

class GameParameters {
    public static final int DODGE_DISTANCE = 2200;
    public static int WIDTH = 16001;
    public static int HEIGHT = 9001;

    public static int VISIBILITY_DISTANCE = 2200;

    public static int RELEASE_MAX_DISTANCE = 1600;

    public static int CATCH_MIN_DISTANCE = 900;
    public static int CATCH_MAX_DISTANCE = 1760;

    public static int FLEE_DISTANCE = 400;

    public static int STUN_MAX_DISTANCE = 1760;

    public static List<int[]> POSITIONS_TO_EXPLORE = Arrays.asList(
            new int[]{1333, 1500},
            new int[]{4000, 4500},
            new int[]{6666, 1500},
            new int[]{9333, 4500},
            new int[]{12000, 1500},
            new int[]{14666, 4500},
            new int[]{12000, 7500},
            new int[]{9333, 4500},
            new int[]{6666, 7500},
            new int[]{4000, 4500},
            new int[]{1333, 7500},
            new int[]{1333, 4500},
            new int[]{4000, 1500},
            new int[]{6666, 4500},
            new int[]{9333, 1500},
            new int[]{12000, 4500},
            new int[]{14666, 1500},
            new int[]{14666, 7500},
            new int[]{12000, 4500},
            new int[]{9333, 7500},
            new int[]{6666, 4500},
            new int[]{4000, 7500},
            new int[]{1333, 7500},
            new int[]{1333, 4500});
}

class Entity {
    int id;
    int type;
    int x;
    int y;
    int state;
    int value;
    boolean visible = true;

    public Entity(int id, int x, int y, int type, int state, int value) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.state = state;
        this.value = value;
    }

    public int distanceToPoint(int[] position) {
        return distanceToPoint(position[0], position[1]);
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
                type == entity.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id=" + id +
                ", type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", state=" + state +
                ", value=" + value +
                ", visible=" + visible +
                '}';
    }

    public int[] getPosition() {
        return new int[]{x, y};
    }
}

class Buster extends Entity {
    private int stunReload = 0;
    List<Ghost> visibleGhosts = new ArrayList<>();
    List<Buster> visibleEnemies = new ArrayList<>();
    int pathIndex = 0;
    int[] currentExplorationDest = null;
    String move;

    public Buster(int id, int x, int y, int type, int state, int value) {
        super(id, x, y, type, state, value);
    }

    public boolean isCarryingAGhost() {
        return state == 1;
    }

    public boolean isCloseEnoughToBase(int baseX, int baseY) {
        return distanceToPoint(baseX, baseY) <= GameParameters.RELEASE_MAX_DISTANCE;
    }

    public Ghost canCatchAGhost() {
        // Catch easiest ghost first
        return visibleGhosts.stream().filter(ghost -> isInCatchRange(ghost))
                .sorted((g1, g2) -> Integer.compare(g1.state, g2.state))
                .findFirst()
                .orElse(null);
    }

    public boolean isInCatchRange(Ghost ghost) {
        int distance = distanceToPoint(ghost.x, ghost.y);
        return distance >= GameParameters.CATCH_MIN_DISTANCE && distance <= GameParameters.CATCH_MAX_DISTANCE;
    }

    public Ghost findClosestAvailableGhost() {
        return visibleGhosts.stream()
                .min((g1, g2) -> Integer.compare(distanceToPoint(g1.x, g1.y), distanceToPoint(g2.x, g2.y)))
                .orElse(null);
    }

    public Ghost findEasietAvailableGhost() {
        return visibleGhosts.stream()
                .sorted((g1, g2) -> Integer.compare(g1.state, g2.state))
                .findFirst()
                .orElse(null);
    }

    public Buster shouldStunAnEnemy(List<Buster> visibleEnemies, GameState gameState) {
        // Stun if carrying a ghost
        // Or busting a ghost
        // Or close to our base
        // And in range
        return stunReload == 0 ? visibleEnemies.stream()
                .filter(buster -> buster.isActive() && buster.distanceToPoint(x, y) <= GameParameters.STUN_MAX_DISTANCE
                        && (
                        buster.isCarryingAGhost()
                                || buster.isBusting()
                                || buster.distanceToPoint(gameState.getBasePosition()) < 3000
                ))
                .findFirst().orElse(null) : null;
    }

    public void update(Buster buster) {
        x = buster.x;
        y = buster.y;
        state = buster.state;
        value = buster.value;
        updateStunReload();
    }

    public void initializeStunReload() {
        this.stunReload = 20;
    }

    public void updateStunReload() {
        if (stunReload > 0) {
            stunReload--;
        }
    }

    public boolean isActive() {
        return state != 2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Buster buster = (Buster) o;
        return stunReload == buster.stunReload;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), stunReload);
    }

    public int[] getDestOptimalDistance(Entity entity, int distanceThreshold) {
        List<Point2D> points = new ArrayList<>();
        Line2D line = new Line2D.Double(entity.x, entity.y, x, y);
        Point2D current;

        for (Iterator<Point2D> it = new LineIterator(line); it.hasNext(); ) {
            current = it.next();
            points.add(current);
        }

        PointWithDistance optimum = points.stream().map(p -> new PointWithDistance((int) p.getX(), (int) p.getY(), entity.distanceToPoint((int) p.getX(), (int) p.getY())))
                .filter(p -> entity.distanceToPoint(p.x, p.y) < distanceThreshold)
                .max((p1, p2) -> Integer.compare(p1.distance, p2.distance)).get();

        return new int[]{optimum.x, optimum.y};
    }

    public Buster isCloseToAnEnnemyBusting(List<Buster> enemyBusters) {
        return enemyBusters.stream().filter(enemy -> distanceToPoint(enemy.x, enemy.y) < GameParameters.VISIBILITY_DISTANCE && enemy.isBusting()).findFirst().orElse(null);
    }

    private boolean isBusting() {
        return state == 3;
    }

    public List<Buster> getEnemiesInSight(GameState gameState) {
        return gameState.enemyBusters.values().stream().filter(enemy -> distanceToPoint(enemy.x, enemy.y) <= GameParameters.VISIBILITY_DISTANCE).collect(Collectors.toList());
    }

    public void resetVisibleGhosts() {
        visibleGhosts.clear();
    }

    public void addVisibleGhost(Ghost ghost) {
        visibleGhosts.add(ghost);
    }

    private boolean samePosition(int[] position) {
        return x == position[0] && y == position[1];
    }

    public Ghost getClosestGhostNotVisibleAndAvailable(List<Ghost> invisibleGhosts) {
        Ghost ghost = invisibleGhosts.stream().filter(g -> g.available)
                .sorted((g1, g2) -> Integer.compare(distanceToPoint(g1.getPosition()), distanceToPoint(g2.getPosition())))
                .findFirst().orElse(null);
        if (ghost != null) {
            ghost.available = false;
        }
        return ghost;
    }

    private void updateNextExplorationDest() {
        if (currentExplorationDest == null) {
            Random random = new Random();
            // Start exploring from random point
            pathIndex = Math.abs(random.nextInt()) % GameParameters.POSITIONS_TO_EXPLORE.size();
            currentExplorationDest = GameParameters.POSITIONS_TO_EXPLORE.get(pathIndex);
        }

        if (x == currentExplorationDest[0] && y == currentExplorationDest[1]) {
            // Destination reached, update it
            pathIndex++;
            currentExplorationDest = GameParameters.POSITIONS_TO_EXPLORE
                    .get(pathIndex % GameParameters.POSITIONS_TO_EXPLORE.size());
        }
    }

    private Buster enemyThreat() {
        return visibleEnemies.stream().filter(enemy -> enemy.isActive() && enemy.distanceToPoint(getPosition()) > GameParameters.STUN_MAX_DISTANCE).findFirst().orElse(null);
    }

    public int[] getPointAtMinimalDistance(Entity entity, int minDistance) {

        boolean found = false;
        int[] result = null;
        int startAngle = 0;

        while (!found && startAngle < 361) {
            int newX = (int) (minDistance * Math.cos(Math.toRadians(startAngle)) + entity.x);
            int newY = (int) (minDistance * Math.sin(Math.toRadians(startAngle)) + entity.y);

            if (newX >= 0 && newX < GameParameters.WIDTH && newY >= 0 && newY < GameParameters.HEIGHT && newX != x && newY != y) {
                found = true;
                result = new int[]{newX, newY};
            } else {
                startAngle++;
            }
        }

        return result;
    }

    public void addVisibleEnemy(Buster enemy) {
        visibleEnemies.add(enemy);
    }

    public void resetVisibleEnemies() {
        visibleEnemies.clear();
    }

    public void updateMoveIfRelease(GameState gameState) {
        if (isCarryingAGhost()) {
            // Ghost will not be visible until he s dropped
            gameState.removeGhost(value);

            // Release ghost if close enough
            if (isCloseEnoughToBase(gameState.baseX, gameState.baseY)) {
                move = "RELEASE (" + value + ")";
            } else {
                // Go back to base
                int[] dest = gameState.getBasePosition();
                move = "MOVE " + dest[0] + " " + dest[1] + " (Back)";
            }
        }
    }

    public void updateMoveIfStun(GameState gameState) {
        Buster enemyToStun = shouldStunAnEnemy(getEnemiesInSight(gameState), gameState);
        if (enemyToStun != null) {
            initializeStunReload();
            move = "STUN " + enemyToStun.id;
        }
    }

    public void updateMoveIfCatch() {
        Ghost ghostToCatch = canCatchAGhost();
        if (ghostToCatch != null) {
            ghostToCatch.busted();
            move = "BUST " + ghostToCatch.id + " (" + id + " B : " + ghostToCatch.id + ")";
        }
    }

    public void updateBackupMove(Ghost ghost) {
        move = getMoveToGhost(ghost) + " (Backup " + ghost.id + ")";
    }

    private String getMoveToGhost(Ghost ghost) {
        if (distanceToPoint(ghost.x, ghost.y) < GameParameters.CATCH_MIN_DISTANCE) {
            // Too close
            int[] dest = getPointAtMinimalDistance(ghost, GameParameters.CATCH_MIN_DISTANCE);
            return "MOVE " + dest[0] + " " + dest[1];
        } else {
            return "MOVE " + ghost.x + " " + ghost.y;
        }
    }

    public void updateMoveIfCloseIn() {
        Ghost ghostToSeek = findEasietAvailableGhost();
        if (visibleGhosts.size() > 0) {
            move = getMoveToGhost(ghostToSeek) + " (CI " + ghostToSeek.id + ")";
        }
    }

    public void updateMoveIfKnownGhost(GameState gameState) {
        Ghost closestGhostNotVisible = getClosestGhostNotVisibleAndAvailable(gameState.getInvisibleGhosts());
        if (closestGhostNotVisible != null) {
            if (samePosition(closestGhostNotVisible.getPosition())) {
                // Ghost is not where we expected him to be, remove him for now
                gameState.removeGhost(closestGhostNotVisible.id);
            } else {
                // Move to last ghost posistion
                move = "MOVE " + closestGhostNotVisible.x + " " + closestGhostNotVisible.y + " (F " + closestGhostNotVisible.id + ")";
            }
        }
    }

    public void updateMoveIfExploration() {
        // Nothing to do, explore
        updateNextExplorationDest();
        move = "MOVE " + currentExplorationDest[0] + " " + currentExplorationDest[1] + " (E + "
                + currentExplorationDest[0] + " "
                + currentExplorationDest[1] + ")";
    }

    public void updateMoveIfStuned() {
        if (!isActive()) {
            move = "MOVE " + x + " " + y + " (" + value + ")";
        }
    }

    public void resetMove() {
        move = null;
    }
}

class PointWithDistance {
    int x;
    int y;
    int distance;

    public PointWithDistance(int x, int y, int distance) {
        this.x = x;
        this.y = y;
        this.distance = distance;
    }
}

class LineIterator implements Iterator<Point2D> {
    final static double DEFAULT_PRECISION = 1.0;
    final Line2D line;
    final double precision;

    final double sx, sy;
    final double dx, dy;

    double x, y, error;

    public LineIterator(Line2D line, double precision) {
        this.line = line;
        this.precision = precision;

        sx = line.getX1() < line.getX2() ? precision : -1 * precision;
        sy = line.getY1() < line.getY2() ? precision : -1 * precision;

        dx = Math.abs(line.getX2() - line.getX1());
        dy = Math.abs(line.getY2() - line.getY1());

        error = dx - dy;

        y = line.getY1();
        x = line.getX1();
    }

    public LineIterator(Line2D line) {
        this(line, DEFAULT_PRECISION);
    }

    @Override
    public boolean hasNext() {
        return Math.abs(x - line.getX2()) > 0.9 || (Math.abs(y - line.getY2()) > 0.9);
    }

    @Override
    public Point2D next() {
        Point2D ret = new Point2D.Double(x, y);

        double e2 = 2 * error;
        if (e2 > -dy) {
            error -= dy;
            x += sx;
        }
        if (e2 < dx) {
            error += dx;
            y += sy;
        }

        return ret;
    }

    @Override
    public void remove() {
        throw new AssertionError();
    }
}

class Ghost extends Entity {

    int ownBusterCount = 0;
    boolean available = true;

    public Ghost(int id, int x, int y, int type, int state, int value) {
        super(id, x, y, type, state, value);
    }

    public Ghost(int id, int x, int y, int type, int state, int value, boolean visible) {
        super(id, x, y, type, state, value);
        this.visible = visible;
    }

    public void update(Ghost ghost) {
        x = ghost.x;
        y = ghost.y;
        state = ghost.state;
        value = ghost.value;
        visible = ghost.visible;
        ownBusterCount = 0;
        available = true;
    }

    public void busted() {
        ownBusterCount++;
    }

    public boolean needBackup() {
        return visible && ownBusterCount > 0 && ownBusterCount == value - ownBusterCount;
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
        myBusters.forEach((id, buster) -> {
            buster.resetVisibleGhosts();
            buster.resetVisibleEnemies();
            buster.resetMove();
        });
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

    public void updateBuster(Buster buster) {
        if (myBusters.containsKey(buster.id)) {
            myBusters.get(buster.id).update(buster);
        } else {
            myBusters.put(buster.id, buster);
        }
    }

    public void updateGhost(Ghost ghost) {
        ghost.visible = true;
        if (ghosts.containsKey(ghost.id)) {
            ghosts.get(ghost.id).update(ghost);
        } else {
            ghosts.put(ghost.id, ghost);
        }

        myBusters.values().stream().forEach(buster -> {
            if (buster.distanceToPoint(ghost.x, ghost.y) <= GameParameters.VISIBILITY_DISTANCE) {
                buster.addVisibleGhost(ghosts.get(ghost.id));
            }
        });
    }

    public void updateEnemyBuster(Buster buster) {
        buster.visible = true;
        if (enemyBusters.containsKey(buster.id)) {
            enemyBusters.get(buster.id).update(buster);
        } else {
            enemyBusters.put(buster.id, buster);
        }

        myBusters.values().stream().forEach(myBuster -> {
            if (myBuster.distanceToPoint(buster.x, buster.y) <= GameParameters.VISIBILITY_DISTANCE) {
                myBuster.addVisibleEnemy(enemyBusters.get(buster.id));
            }
        });
    }

    public boolean isBaseTopLeft() {
        return baseX == 0;
    }

    public Ghost getGhost(int ghostToCatch) {
        return ghosts.get(ghostToCatch);
    }

    public int[] getBasePosition() {
        return new int[]{baseX, baseY};
    }

    public List<Ghost> getInvisibleGhosts() {
        return ghosts.values().stream().filter(ghost -> !ghost.visible).collect(Collectors.toList());
    }

    public void removeGhost(int ghostId) {
        ghosts.remove(ghostId);
    }

    public void updateMoves() {
        // Moves for stuned busters
        myBusters.values().stream().forEach(buster -> buster.updateMoveIfStuned());

        // First set all release moves
        myBusters.values().stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfRelease(this));

        // Then set stun moves
        myBusters.values().stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfStun(this));

        // Then set catch ghost moves
        myBusters.values().stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfCatch());

        // Then check for backup moves
        Ghost ghost = ghosts.values().stream().filter(g -> g.needBackup()).findFirst().orElse(null);
        if (ghost != null) {
            myBusters.values().stream().filter(buster -> buster.move == null)
                    .sorted((b1, b2) -> Integer.compare(b1.distanceToPoint(ghost.x, ghost.y), b2.distanceToPoint(ghost.x, ghost.y)))
                    .findFirst().ifPresent(buster -> buster.updateBackupMove(ghost));
        }

        // Then set close in moves
        myBusters.values().stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfCloseIn());

        // Then explore to last know ghost
        myBusters.values().stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfKnownGhost(this));

        // Then explore to last know ghost
        myBusters.values().stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfExploration());
    }
}