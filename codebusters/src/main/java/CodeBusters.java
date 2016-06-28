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

        // Create squads of busters based on buster count per players
        List<Squad> squads = getSquads(gameState, myTeamId);

        /*for (int i = 0; i < squads.size(); i++) {
            System.err.println("S" + i + " is " + squads.get(i));
        }*/

        gameState.setSquads(squads);

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

            for (int i = 0; i < gameState.squads.size(); i++) {
                Squad squad = gameState.squads.get(i);
                if (squad.needToUpdateObjective(gameState)) {
                    squad.updateObjective(gameState);
                }

                squad.members.keySet().stream().sorted().forEach(
                        busterId -> {
                            Buster buster = squad.members.get(busterId);
                            if (buster.isActive()) {
                                if (buster.canParticipateInObjective(squad)) {
                                    System.out.println(squad.getSquadObjective());
                                } else {
                                    System.out.println(buster.getPersonnalObjective(gameState, squad));
                                }
                            } else {
                                System.out.println("MOVE " + buster.x + " " + buster.y + " (Stunned)");
                            }
                        }
                );
            }
        }
    }

    private static List<Squad> getSquads(GameState gameState, int myTeamId) {
        List<Squad> squads = new ArrayList<>();
        int busterId = myTeamId * gameState.busterCount;
        Squad squad;
        int[] stealObjective = gameState.isBaseTopLeft() ? new int[]{14000, 8000} : new int[]{1000, 1000};
        switch (gameState.busterCount) {
            case 2:
                squad = new Squad(0, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new DestroyerSquad(1, stealObjective);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                break;
            case 3:
                squad = new Squad(0, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new Squad(1, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new DestroyerSquad(2, stealObjective);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                break;
            case 4:
                squad = new Squad(0, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new Squad(1, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new DestroyerSquad(2, stealObjective);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                break;
            case 5:
                squad = new Squad(0, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new Squad(1, AIParameters.SQUAD_TYPE_SEEKER);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                squad = new DestroyerSquad(2, stealObjective);
                squad.addMember(new Buster(busterId++, gameState.baseX, gameState.baseY, 0, 0, 0));
                squads.add(squad);
                break;
        }

        return squads;
    }
}

class AIParameters {
    public static int SQUAD_COUNT = 2;

    public static Map<Integer, int[]> SQUAD_REPARTITION_BY_BUSTER_COUNT = new HashMap<>();

    static {
        SQUAD_REPARTITION_BY_BUSTER_COUNT.put(2, new int[]{0, 1});
        SQUAD_REPARTITION_BY_BUSTER_COUNT.put(3, new int[]{0, 1, 1});
        SQUAD_REPARTITION_BY_BUSTER_COUNT.put(4, new int[]{0, 0, 1, 1});
        SQUAD_REPARTITION_BY_BUSTER_COUNT.put(5, new int[]{0, 0, 1, 1, 1});
    }

    public static int SQUAD_TYPE_SEEKER = 1;
    public static int SQUAD_TYPE_DESTROYER = 2;
}

class GameParameters {
    public static int WIDTH = 16000;
    public static int HEIGHT = 9000;

    public static int VISIBILITY_DISTANCE = 2200;

    public static int RELEASE_MAX_DISTANCE = 1600;

    public static int CATCH_MIN_DISTANCE = 900;
    public static int CATCH_MAX_DISTANCE = 1760;

    public static int FLEE_DISTANCE = 400;

    public static int STUN_MAX_DISTANCE = 1760;

    public static Map<Integer, List<List<int[]>>> POSITIONS_FROM_TOP_LEFT = new HashMap<>();

    static {
        POSITIONS_FROM_TOP_LEFT.put(2, Arrays.asList(
                Arrays.asList(
                        new int[]{3111, 1555},
                        new int[]{6222, 1800},
                        new int[]{6222, 1555},
                        new int[]{9333, 1555},
                        new int[]{9333, 3500},
                        new int[]{12444, 4666},
                        new int[]{12444, 1555},
                        new int[]{14445, 1555},
                        new int[]{14445, 4666},
                        new int[]{14445, 7445}),
                Arrays.asList(
                        new int[]{1555, 3111},
                        new int[]{4300, 3111},
                        new int[]{1555, 6222},
                        new int[]{1555, 7745},
                        new int[]{4666, 6222},
                        new int[]{7777, 6222},
                        new int[]{7777, 7745},
                        new int[]{10888, 7745},
                        new int[]{10102, 7745})));
    }
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
}

class Buster extends Entity {
    private int stunReload = 0;

    public Buster(int id, int x, int y, int type, int state, int value) {
        super(id, x, y, type, state, value);
    }

    public boolean isCarryingAGhost() {
        return state == 1;
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
        return distance >= GameParameters.CATCH_MIN_DISTANCE && distance <= GameParameters.CATCH_MAX_DISTANCE;
    }

    public Ghost findClosestAvailableGhost(List<Ghost> ghosts, List<Ghost> huntedGhosts) {
        List<Ghost> remainingGhosts = new ArrayList<>(ghosts);
        remainingGhosts.removeAll(huntedGhosts);
        return remainingGhosts.stream().min((g1, g2) -> Integer.compare(distanceToPoint(g1.x, g1.y), distanceToPoint(g2.x, g2.y))).orElse(null);
    }

    public Buster shouldStunAnEnemy(List<Buster> visibleEnemies) {
        return stunReload == 0 ? visibleEnemies.stream()
                .filter(buster -> buster.distanceToPoint(x, y) <= GameParameters.STUN_MAX_DISTANCE && buster.isCarryingAGhost())
                .findFirst().orElse(null) : null;
    }

    public void update(Buster buster) {
        x = buster.x;
        y = buster.y;
        state = buster.state;
        value = buster.value;
    }

    public void initializeStunReload() {
        this.stunReload = 20;
    }

    public void updateStunReload() {
        if (stunReload > 0) {
            stunReload--;
        }
    }

    public boolean canParticipateInObjective(Squad squad) {
        if (squad.isBustingAGhost() && !isInCatchRange(squad.ghostToCatch)) {
            return false;
        }
        return !isCarryingAGhost() && isActive();
    }

    public boolean isActive() {
        return state != 2;
    }

    public String getPersonnalObjective(GameState gameState, Squad squad) {
        if (isCarryingAGhost()) {
            // Release ghost if close enough
            if (isCloseEnoughToBase(gameState.baseX, gameState.baseY)) {
                return "RELEASE (" + value + ")";
            } else {
                // Go back to base
                int[] dest = squad.getReleaseDestination(gameState);
                return "MOVE " + dest[0] + " " + dest[1] + " (Back)";
            }
        } else if (squad.isBustingAGhost()) {
            // Need to close in
            int[] objective = getDestOptimalDistance(squad.ghostToCatch, GameParameters.CATCH_MAX_DISTANCE);
            return "MOVE " + objective[0] + " " + objective[1] + " (S" + squad.id + " CI : " + squad.ghostToCatch.id + ")";
        }
        return "ERROR " + squad.id + " / " + toString();
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

    public Ghost(int id, int x, int y, int type, int state, int value) {
        super(id, x, y, type, state, value);
    }

    public void update(Ghost ghost) {
        x = ghost.x;
        y = ghost.y;
        state = ghost.state;
        value = ghost.value;
        visible = ghost.visible;
    }
}

class Squad {
    public static final int OBJECTIVE_TYPE_BUST = 1;
    public static final int OBJECTIVE_TYPE_CLOSE_IN = 2;
    public static final int OBJECTIVE_TYPE_EXPLORE = 3;
    public static final int OBJECTIVE_TYPE_STEAL = 4;


    int id;
    int squadType;
    Map<Integer, Buster> members = new HashMap<>();
    int objectiveType = OBJECTIVE_TYPE_EXPLORE;
    String objectiveMove;
    Ghost ghostToCatch;
    int[] objective = null;
    int pathIndex = 0;

    public Squad(int id, int squadType) {
        this.id = id;
        this.squadType = squadType;
    }

    public void addMember(Buster buster) {
        members.put(buster.id, buster);
    }

    public boolean needToUpdateObjective(GameState gameState) {
        // No objective yet
        if (objectiveMove == null) {
            System.err.println("S" + id + " UPD 1");
            return true;
        }

        // Ghost is catched
        if (ghostToCatch != null && !gameState.ghosts.get(ghostToCatch.id).visible) {
            System.err.println("S" + id + " UPD 2");
            ghostToCatch = null;
            return true;
        }

        // Found a ghost
        if (objectiveType == OBJECTIVE_TYPE_CLOSE_IN && members.values().stream().anyMatch(buster -> buster.canCatchAGhost(gameState.getVisibleGhosts(), gameState.getHuntedGhosts()) != null)) {
            System.err.println("S" + id + " UPD 3");
            return true;
        }

        // Found a ghost while exploring
        if (objectiveType == OBJECTIVE_TYPE_EXPLORE && members.values().stream().anyMatch(buster -> buster.canCatchAGhost(gameState.getVisibleGhosts(), gameState.getHuntedGhosts()) != null
                || buster.findClosestAvailableGhost(gameState.getVisibleGhosts(), gameState.getHuntedGhosts()) != null)) {
            System.err.println("S" + id + " UPD 4");
            return true;
        }

        // Found an ennemy catching a ghost
        /*if (objectiveType == OBJECTIVE_TYPE_EXPLORE && members.values().stream().anyMatch(buster -> buster.isCloseToAnEnnemyBusting(gameState.getVisibleAndActiveEnemyBusters()) != null)) {
            System.err.println("S" + id + " UPD 6");
            return true;
        }*/

        // Destination reached
        if ((objectiveType == OBJECTIVE_TYPE_EXPLORE || objectiveType == OBJECTIVE_TYPE_CLOSE_IN)
                && members.values().stream().anyMatch(buster -> buster.x == objective[0] && buster.y == objective[1])) {
            System.err.println("S" + id + " UPD 5");
            return true;
        }

        return false;
    }

    public void updateObjective(GameState gameState) {
        int distance = Integer.MAX_VALUE;

        Ghost huntedGhost = null;

        // 1. Try to catch a ghost
        for (Buster buster : members.values()) {
            Ghost ghost = buster.canCatchAGhost(gameState.getVisibleGhosts(), gameState.getHuntedGhosts());
            if (ghost != null && buster.distanceToPoint(ghost.x, ghost.y) < distance) {
                objective = new int[]{ghost.x, ghost.y};
                distance = buster.distanceToPoint(ghost.x, ghost.y);
                huntedGhost = ghost;
            }
        }

        if (huntedGhost != null) {
            objectiveType = OBJECTIVE_TYPE_BUST;
            objectiveMove = "BUST " + huntedGhost.id + " (S" + id + " B : " + huntedGhost.id + ")";
            ghostToCatch = huntedGhost;
            return;
        }

        if (objective == null) {
            for (Buster buster : members.values()) {
                Ghost ghost = buster.findClosestAvailableGhost(gameState.getVisibleGhosts(), gameState.getHuntedGhosts());
                if (ghost != null && buster.distanceToPoint(ghost.x, ghost.y) < distance && noOtherSquadCloser(gameState, ghost, distance)) {
                    objective = buster.getDestOptimalDistance(ghost, GameParameters.CATCH_MAX_DISTANCE);
                    distance = buster.distanceToPoint(ghost.x, ghost.y);
                    huntedGhost = ghost;
                }
            }
        }

        // 2. Close in to visible ghost
        if (huntedGhost != null) {
            ghostToCatch = huntedGhost;
            objectiveType = OBJECTIVE_TYPE_CLOSE_IN;
            objectiveMove = "MOVE " + objective[0] + " " + objective[1] + " (S" + id + " CI : " + huntedGhost.id + ")";
        } else {
            // 3. Explore
            List<int[]> path = GameParameters.POSITIONS_FROM_TOP_LEFT.get(2).get(id);
            objective = path.get(pathIndex++ % path.size());
            objectiveType = OBJECTIVE_TYPE_EXPLORE;
            objectiveMove = "MOVE " + objective[0] + " " + objective[1] + " (S" + id + " EX : " + objective[0] + " " + objective[1] + ")";
        }
    }

    public boolean noOtherSquadCloser(GameState gameState, Ghost ghost, int distance) {
        for (Squad squad : gameState.squads) {
            if (squad.id != id && !squad.isBusting()) {
                // Check other squad members distance to Ghost
                if (squad.members.values().stream().anyMatch(buster ->
                        buster.isActive()
                                && !buster.isCarryingAGhost()
                                && buster.distanceToPoint(ghost.x, ghost.y) < distance)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBusting() {
        return objectiveType == OBJECTIVE_TYPE_BUST;
    }

    public String getSquadObjective() {
        return objectiveMove;
    }

    @Override
    public String toString() {
        return "Squad{" +
                "id=" + id +
                ", members=" + members +
                '}';
    }

    public boolean isBustingAGhost() {
        return ghostToCatch != null;
    }

    public int[] getReleaseDestination(GameState gameState) {
        return new int[]{gameState.baseX, gameState.baseY};
    }
}

class DestroyerSquad extends Squad {

    private final int[] objective;
    int ghostToCatch = -1;
    int[] DODGING_PATH = new int[]{0, 9000};

    public DestroyerSquad(int id, int[] objective) {
        super(id, AIParameters.SQUAD_TYPE_DESTROYER);
        this.objective = objective;
    }

    @Override
    public void updateObjective(GameState gameState) {
        Buster buster = members.values().stream().findFirst().get();

        List<Buster> enemiesInSight = buster.getEnemiesInSight(gameState);
        System.err.println("Enemies in sight : " + enemiesInSight);

        // 1. Stun first, ask questions later
        Buster enemy = buster.shouldStunAnEnemy(enemiesInSight);
        if (enemy != null) {
            objectiveMove = "STUN " + enemy.id + " (On the ground " + enemy.id + "!)";
            buster.initializeStunReload();
            ghostToCatch = enemy.value;
        } else if (ghostToCatch != -1) {
            // 2. Steal ghost
            Ghost ghost = gameState.getGhost(ghostToCatch);
            if (buster.isInCatchRange(ghost)) {
                objectiveMove = "BUST " + ghost.id + " (S" + id + " B : " + ghost.id + ")";
            }
            ghostToCatch = -1;
        } else if (buster.x != objective[0] && buster.y != objective[1]) {
            // 3. Get in position
            objectiveMove = "MOVE " + objective[0] + " " + objective[1] + " (S" + id + " S : " + objective[0] + " " + objective[1] + ")";
        }
    }

    @Override
    public boolean needToUpdateObjective(GameState gameState) {
        Buster buster = members.values().stream().findFirst().get();
        if (buster.isActive() && !buster.isCarryingAGhost()) {
            return true;
        }
        return false;
    }

    public int[] getReleaseDestination(GameState gameState) {
        Buster buster = members.values().stream().findFirst().get();
        if (buster.x != DODGING_PATH[0] && buster.y != DODGING_PATH[1]) {
            return DODGING_PATH;
        } else {
            return new int[]{gameState.baseX, gameState.baseY};
        }
    }
}

class GameState {
    int baseX;
    int baseY;
    int busterCount;
    int ghostCount;
    List<Squad> squads;

    public GameState(int baseX, int baseY, int busterCount, int ghostCount) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.busterCount = busterCount;
        this.ghostCount = ghostCount;
    }

    Map<Integer, Buster> enemyBusters = new HashMap<>();
    Map<Integer, Ghost> ghosts = new HashMap<>();

    public void resetState() {
        enemyBusters.forEach((id, buster) -> buster.visible = false);
        ghosts.forEach((id, ghost) -> ghost.visible = false);
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
        squads.stream().forEach(squad -> {
            if (squad.members.containsKey(buster.id)) {
                squad.members.get(buster.id).update(buster);
                squad.members.get(buster.id).updateStunReload();
                // System.err.println("Updating buster " + buster + " in squad " + squad.id);
            }
        });
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

    public List<Buster> getVisibleAndActiveEnemyBusters() {
        return enemyBusters.values().stream().filter(buster -> buster.visible && buster.state != 2).collect(
                Collectors.toList());
    }

    public List<Ghost> getVisibleGhosts() {
        return ghosts.values().stream().filter(ghost -> ghost.visible).collect(Collectors.toList());
    }

    public void setSquads(List<Squad> squads) {
        this.squads = squads;
    }

    public boolean isBaseTopLeft() {
        return baseX == 0;
    }

    public List<Ghost> getHuntedGhosts() {
        return squads.stream().map(squad -> squad.ghostToCatch).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public Ghost getGhost(int ghostToCatch) {
        return ghosts.get(ghostToCatch);
    }
}