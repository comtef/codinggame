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

        GameState gameState = new GameState(myTeamId == 0 ? new int[]{0, 0} : new int[]{16000, 9000},
                myTeamId == 0 ? new int[]{16000, 9000} : new int[]{0, 0}, bustersPerPlayer,
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

            gameState.updateTurn();
            gameState.updateMoves();

            gameState.myBusters.keySet().stream().sorted().forEach(busterId -> {
                Buster buster = gameState.myBusters.get(busterId);
                System.out.println(buster.move);
            });
        }
    }
}

class GameParameters {
    public static int WIDTH = 16001;
    public static int HEIGHT = 9001;

    public static int VISIBILITY_DISTANCE = 2200;

    public static int RELEASE_MAX_DISTANCE = 1600;

    public static int CATCH_MIN_DISTANCE = 900;
    public static int CATCH_MAX_DISTANCE = 1760;

    public static int STUN_MAX_DISTANCE = 1760;

    public static int START_CATCHING_GHOST_15_AT_TURN = 15;
    public static int START_CATCHING_GHOST_40_AT_TURN = 75;

    public static List<int[]> POSITION_TO_EXPLORE_FIRST_FROM_TOP = Arrays.asList(
            new int[]{9333, 5000},
            new int[]{12000, 3000},
            new int[]{6666, 6000},
            new int[]{14666, 1500},
            new int[]{1333, 7500}
    );

    public static List<int[]> POSITION_TO_EXPLORE_FIRST_FROM_BOTTOM = Arrays.asList(
            new int[]{6666, 4000},
            new int[]{4000, 6000},
            new int[]{9333, 1500},
            new int[]{14666, 1500},
            new int[]{1333, 7500}
    );

    public static List<int[]> POSITIONS_TO_EXPLORE_LATE_GAME = Arrays.asList(
            new int[]{8000, 1000},
            new int[]{15000, 1000},
            new int[]{15000, 4500},
            new int[]{8000, 4500},
            new int[]{1000, 7000},
            new int[]{1000, 4500},
            new int[]{8000, 4500}
    );

    public static List<int[]> POSITIONS_TO_EXPLORE = Arrays.asList(
            new int[]{4000, 1500},
            new int[]{6666, 1500},
            new int[]{9333, 1500},
            new int[]{12000, 1500},
            new int[]{1333, 4500},
            new int[]{4000, 4500},
            new int[]{6666, 4500},
            new int[]{9333, 4500},
            new int[]{12000, 4500},
            new int[]{14666, 4500},
            new int[]{1333, 7500},
            new int[]{4000, 7500},
            new int[]{6666, 7500},
            new int[]{9333, 7500},
            new int[]{12000, 7500});
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

    public int[] getPosition() {
        return new int[]{x, y};
    }
}

class Buster extends Entity {
    private int stunReload = 0;
    List<Ghost> visibleGhosts = new ArrayList<>();
    List<Buster> visibleEnemies = new ArrayList<>();
    int[] ghostToSteal = null;
    int[] currentExplorationDest = null;
    String move;

    public Buster(int id, int x, int y, int type, int state, int value) {
        super(id, x, y, type, state, value);
    }

    public boolean isCarryingAGhost() {
        return state == 1;
    }

    public boolean isCloseEnoughToBase(int[] basePosition) {
        return distanceToPoint(basePosition) <= GameParameters.RELEASE_MAX_DISTANCE;
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

    public Buster shouldStunAnEnemy() {
        // Good old stun
        return stunReload == 0 ? visibleEnemies.stream()
                .filter(buster -> buster.isActive() && buster.distanceToPoint(x, y) <= GameParameters.STUN_MAX_DISTANCE)
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

    public void resetVisibleGhosts() {
        visibleGhosts.clear();
    }

    public void addVisibleGhost(Ghost ghost) {
        visibleGhosts.add(ghost);
    }

    private boolean samePosition(int[] position) {
        return x == position[0] && y == position[1];
    }

    private void updateNextExplorationDest(GameState gameState) {
        if (gameState.unknowPositions.size() == 0) {
            gameState.resetUnknownPositions();
        }
        if (currentExplorationDest == null || (x == currentExplorationDest[0] && y == currentExplorationDest[1])) {
            currentExplorationDest = getClosestPosition(gameState.unknowPositions);
            gameState.unknowPositions.remove(currentExplorationDest);
        }
    }

    private int[] getClosestPosition(List<int[]> unknowPositions) {
        return unknowPositions.stream().sorted((p1, p2) -> Integer.compare(distanceToPoint(p1), distanceToPoint(p2)))
                .findFirst().get();
    }

    public int[] getPointAtMinimalDistance(Entity entity, int minDistance) {
        return getPointAtMinimalDistance(entity.x, entity.y, minDistance);
    }

    public int[] getPointAtMinimalDistance(int entityX, int entityY, int minDistance) {

        boolean found = false;
        int[] result = null;
        int startAngle = 0;

        while (!found && startAngle < 361) {
            int newX = (int) (minDistance * Math.cos(Math.toRadians(startAngle)) + entityX);
            int newY = (int) (minDistance * Math.sin(Math.toRadians(startAngle)) + entityY);

            if (newX >= 0 && newX < GameParameters.WIDTH && newY >= 0 && newY < GameParameters.HEIGHT && newX != x
                    && newY != y) {
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
            if (isCloseEnoughToBase(gameState.getBasePosition())) {
                gameState.releaseCount++;
                move = "RELEASE (" + value + ")";
            }
        }
    }

    public void updateMoveIfStun() {
        Buster enemyToStun = shouldStunAnEnemy();
        if (enemyToStun != null) {
            initializeStunReload();
            move = "STUN " + enemyToStun.id;

            if (enemyToStun.isCarryingAGhost()) {
                ghostToSteal = enemyToStun.getPosition();
            }

            // Mark him as stunned
            enemyToStun.state = 2;
        }
    }

    public void updateMoveIfCatch(GameState gameState) {
        Ghost ghostToCatch = canCatchAGhostWithMaxStamina(gameState.getCurrentGhostThreshold());
        if (ghostToCatch != null) {
            ghostToCatch.busted();
            move = "BUST " + ghostToCatch.id + " (" + id + " B : " + ghostToCatch.id + ")";
        }
    }

    private Ghost canCatchAGhostWithMaxStamina(int maxStamina) {
        return visibleGhosts.stream().filter(ghost -> ghost.state <= maxStamina && isInCatchRange(ghost))
                .sorted((g1, g2) -> Integer.compare(g1.state, g2.state))
                .findFirst()
                .orElse(null);
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

    public void updateMoveIfCloseIn(GameState gameState) {
        int currentObjective = gameState.getCurrentGhostThreshold();
        Ghost ghostToSeek = findGhostInSight(currentObjective);
        if (ghostToSeek != null) {
            move = getMoveToGhost(ghostToSeek) + " (CI " + ghostToSeek.id + ")";
        }
    }

    private Ghost findGhostInSight(int maxStamina) {
        return visibleGhosts.stream()
                .filter(g -> g.state <= maxStamina)
                .sorted((g1, g2) -> Integer.compare(g1.state, g2.state))
                .findFirst()
                .orElse(null);
    }

    public void updateMoveIfKnownGhost(GameState gameState) {
        Ghost ghost = tryToFindGhost(gameState);
        if (ghost != null) {
            if (samePosition(ghost.getPosition())) {
                // Ghost is not where we expected him to be, remove him for now
                gameState.removeGhost(ghost.id);
            } else {
                // Move to last ghost posistion
                move = "MOVE " + ghost.x + " " + ghost.y + " (F " + ghost.id + ")";
            }
        }
    }

    private Ghost tryToFindGhost(GameState gameState) {
        int currentObjective = gameState.getCurrentGhostThreshold();
        return gameState.ghosts.values().stream()
                .filter(g -> g.state <= currentObjective)
                .sorted((g1, g2) -> Integer.compare(g1.state, g2.state))
                .findFirst().orElse(null);
    }

    public void updateMoveIfExploration(GameState gameState) {
        // Nothing to do, explore
        updateNextExplorationDest(gameState);
        move = "MOVE " + currentExplorationDest[0] + " " + currentExplorationDest[1];
    }

    public void updateMoveIfStuned() {
        if (!isActive()) {
            move = "MOVE " + x + " " + y + " (" + value + ")";
            ghostToSteal = null;
        }
    }

    public void resetMove() {
        move = null;
    }

    public void updateMoveIfGoBack(GameState gameState) {
        if (isCarryingAGhost()) {
            // Go back to base
            int[] dest = gameState.getBasePosition();
            move = "MOVE " + dest[0] + " " + dest[1] + " (Back)";
        }
    }

    public void updateMoveIfHunt(GameState gameState) {
        if (ghostToSteal != null) {
            int[] dest = getPointAtMinimalDistance(ghostToSteal[0], ghostToSteal[1],
                    GameParameters.CATCH_MAX_DISTANCE - 1);
            move = "MOVE " + dest[0] + " " + dest[1];
            ghostToSteal = null;
        } else {
            Buster enemyToHunt = visibleEnemies.stream()
                    .filter(enemy -> enemy.isCarryingAGhost()
                            && distanceToPoint(enemy.getPosition()) <= GameParameters.STUN_MAX_DISTANCE
                            && enemy.distanceToPoint(gameState.getEnemyBasePosition()) / 800 > stunReload
                    )
                    .findFirst().orElse(null);
            if (enemyToHunt != null) {
                move = "MOVE " + enemyToHunt.x + " " + enemyToHunt.y + " (H)";
            }
        }
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

    public Ghost(int id, int x, int y, int type, int state, int value) {
        super(id, x, y, type, state, value);
    }

    public void update(Ghost ghost) {
        x = ghost.x;
        y = ghost.y;
        state = ghost.state;
        value = ghost.value;
        visible = ghost.visible;
        ownBusterCount = 0;
    }

    public void busted() {
        ownBusterCount++;
    }

    public boolean needBackup() {
        return visible && ownBusterCount > 0 && ownBusterCount == value - ownBusterCount;
    }

    @Override
    public String toString() {
        return "Ghost{" +
                "id=" + id +
                ", x=" + x +
                ", y=" + y +
                ", state=" + state +
                ", value=" + value +
                ", visible=" + visible +
                ", ownBusterCount=" + ownBusterCount +
                '}';
    }
}

class GameState {
    int[] basePosition;
    int[] enemyBasePosition;
    int busterCount;
    int ghostCount;
    List<int[]> unknowPositions;

    int currentTurn = 0;
    int releaseCount = 0;

    public GameState(int[] basePosition, int[] enemyBasePosition, int busterCount, int ghostCount) {
        this.basePosition = basePosition;
        this.enemyBasePosition = enemyBasePosition;
        this.busterCount = busterCount;
        this.ghostCount = ghostCount;
        resetUnknownPositions();
    }

    public void resetUnknownPositions() {
        if (currentTurn > 100) {
            unknowPositions = new ArrayList<>(GameParameters.POSITIONS_TO_EXPLORE_LATE_GAME);
        } else {
            unknowPositions = new ArrayList<>(GameParameters.POSITIONS_TO_EXPLORE);
        }
    }

    Map<Integer, Buster> myBusters = new HashMap<>();
    Map<Integer, Buster> enemyBusters = new HashMap<>();
    Map<Integer, Ghost> ghosts = new HashMap<>();


    public void resetState() {
        enemyBusters.forEach((id, buster) -> buster.visible = false);
        ghosts.forEach((id, ghost) -> {
            ghost.visible = false;
        });
        myBusters.forEach((id, buster) -> {
            buster.resetVisibleGhosts();
            buster.resetVisibleEnemies();
            buster.resetMove();
        });
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
        return basePosition[0] == 0;
    }

    public int[] getBasePosition() {
        return basePosition;
    }

    public int[] getEnemyBasePosition() {
        return enemyBasePosition;
    }

    public void removeGhost(int ghostId) {
        ghosts.remove(ghostId);
    }

    public void updateMoves() {
        List<Buster> busters = myBusters.values().stream().collect(Collectors.toList());

        if (currentTurn == 1) {
            // Set initial moves
            busters.stream().forEach(buster -> buster.currentExplorationDest = isBaseTopLeft() ?
                    GameParameters.POSITION_TO_EXPLORE_FIRST_FROM_TOP.get(buster.id % busterCount)
                    : GameParameters.POSITION_TO_EXPLORE_FIRST_FROM_BOTTOM.get(buster.id % busterCount));
        }

        // Moves for stuned busters
        busters.stream().forEach(buster -> buster.updateMoveIfStuned());

        // First set all release moves
        busters.stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfRelease(this));

        // Then set stun moves
        busters.stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfStun());

        if (keepAdvantage()) {
            busters.stream().filter(buster -> buster.move == null && buster.isCarryingAGhost()).forEach(buster -> {
                if (buster.distanceToPoint(16000, 0) > buster.distanceToPoint(0, 9000)) {
                    buster.move = "MOVE " + 0 + " " + 9000;
                } else {
                    buster.move = "MOVE " + 16000 + " " + 0;
                }
            });

            busters.stream().filter(buster -> buster.move == null).forEach(buster -> {
                Buster busterToProtect = busters.stream().filter(b -> b.isCarryingAGhost()).findFirst().get();
                if (busterToProtect.y < 4500) {
                    buster.move = "MOVE " + 15500 + " " + 500;
                } else {
                    buster.move = "MOVE " + 500 + " " + 8500;
                }
            });
        } else {
            // Then set go back moves
            busters.stream().filter(buster -> buster.move == null)
                    .forEach(buster -> buster.updateMoveIfGoBack(this));

            // Then set catch ghost moves
            busters.stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfCatch(this));

            // Then set hunt moves
            busters.stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfHunt(this));

            // Then set intercept moves
            enemyBusters.values().stream().filter(enemy -> enemy.isCarryingAGhost() && enemy.isActive()).forEach(enemyToIntercept -> {
                busters.stream().filter(buster -> buster.move == null && buster.distanceToPoint(getEnemyBasePosition()) < enemyToIntercept.distanceToPoint(getEnemyBasePosition()))
                        .sorted((b1, b2) -> Integer.compare(b1.distanceToPoint(getEnemyBasePosition()), b2.distanceToPoint(getEnemyBasePosition())))
                        .findFirst().ifPresent(buster -> {
                    if (isBaseTopLeft()) {
                        buster.move = "MOVE " + 14500 + " " + 7500;
                    } else {
                        buster.move = "MOVE " + 1500 + " " + 1500;
                    }
                });
            });


            // Then set close in moves
            busters.stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfCloseIn(this));

            // Then check for backup moves
            Ghost ghost = ghosts.values().stream().filter(g -> g.needBackup()).findFirst().orElse(null);
            if (ghost != null) {
                busters.stream().filter(buster -> buster.move == null)
                        .sorted((b1, b2) -> Integer.compare(b1.distanceToPoint(ghost.x, ghost.y), b2.distanceToPoint(ghost.x, ghost.y)))
                        .findFirst().ifPresent(buster -> buster.updateBackupMove(ghost));
            }

            // Then explore to last known ghost
            busters.stream().filter(buster -> buster.move == null).forEach(buster -> buster.updateMoveIfKnownGhost(this));

            // Then explore
            busters.stream().filter(buster -> buster.move == null)
                    .forEach(buster -> buster.updateMoveIfExploration(this));
        }
    }

    public void updateTurn() {
        currentTurn++;
    }

    public int getCurrentGhostThreshold() {
        if (currentTurn < GameParameters.START_CATCHING_GHOST_15_AT_TURN) {
            return 3;
        } else if (currentTurn < GameParameters.START_CATCHING_GHOST_40_AT_TURN) {
            return 15;
        } else {
            return 40;
        }
    }

    public boolean keepAdvantage() {
        return releaseCount + myBusters.values().stream().mapToInt(b -> b.isCarryingAGhost() ? 1 : 0).sum() > ghostCount / 2;
    }
}