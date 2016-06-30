import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class BusterTest implements WithAssertions {

    @Test
    public void isCloseEnoughToBaseTrueWhenOnBase() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(0, 0)).isTrue();
    }

    @Test
    public void isCloseEnoughToBaseFalseWhenOnMapOpposite() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(GameParameters.WIDTH, GameParameters.HEIGHT)).isFalse();
    }

    @Test
    public void isCloseEnoughToBaseFalseWhenOneDistanceTooFar() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(1 + GameParameters.RELEASE_MAX_DISTANCE, 0)).isFalse();
    }

    @Test
    public void isCloseEnoughToBaseTrueWhenAtMaximumPossibleDistance() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(GameParameters.RELEASE_MAX_DISTANCE, 0)).isTrue();
    }

    @Test
    public void isCloseEnoughToBaseTrueWhenCloseToMaximumPossibleDistance() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(1249, 999)).isTrue();
    }

    @Test
    public void isInCatchRangeReturnsFalseIfTooFar() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 1800, 0, 0, 0, 0))).isFalse();
    }

    @Test
    public void isInCatchRangeReturnsFalseIfTooClose() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 10, 0, 0, 0, 0))).isFalse();
    }

    @Test
    public void isInCatchRangeReturnsTrueIfCloseEnough() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 1000, 600, 0, 0, 0))).isTrue();
    }

    @Test
    public void isInCatchRangeReturnsTrueIfJustInRangeCloseToMin() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 750, 500, 0, 0, 0))).isTrue();
    }

    @Test
    public void isInCatchRangeReturnsTrueIfJustInRangeCloseToMax() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 1400, 1050, 0, 0, 0))).isTrue();
    }

    @Test
    public void canCatchAGhostReturnsNullIfNoGhosts() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        Ghost ghost = buster.canCatchAGhost();
        assertThat(ghost).isNull();
    }

    @Test
    public void canCatchAGhostReturnsGhostIfInRange() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 10000, 5000, 0, 0, 0),
                new Ghost(2, 1400, 1050, 0, 0, 0));
        buster.visibleGhosts = ghosts;
        Ghost ghost = buster.canCatchAGhost();
        assertThat(ghost).isEqualTo(ghosts.get(1));
    }

    @Test
    public void canCatchAGhostReturnsNullIfNoGhostInRange() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 10000, 0, 0, 0, 0),
                new Ghost(2, 0, 0, 0, 0, 0));
        buster.visibleGhosts = ghosts;
        Ghost ghost = buster.canCatchAGhost();
        assertThat(ghost).isNull();
    }

    @Test
    public void findClosestAvailableGhostReturnsNullIfNoGhosts() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList();
        buster.visibleGhosts = ghosts;
        Ghost ghost = buster.findClosestAvailableGhost();
        assertThat(ghost).isNull();
    }

    @Test
    public void findClosestAvailableGhostReturnsGhostIfAvailable() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 1000, 1000, 0, 0, 0),
                new Ghost(2, 500, 500, 0, 0, 0));
        buster.visibleGhosts = ghosts;
        Ghost ghost = buster.findClosestAvailableGhost();
        assertThat(ghost).isEqualTo(ghosts.get(1));
    }

    @Test
    public void findClosestAvailableGhostReturnsClosestGhost() throws Exception {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 1000, 1000, 0, 0, 0),
                new Ghost(2, 500, 500, 0, 0, 0),
                new Ghost(3, 750, 750, 0, 0, 0));
        buster.visibleGhosts = ghosts;
        Ghost ghost = buster.findClosestAvailableGhost();
        assertThat(ghost).isEqualTo(ghosts.get(1));
    }

    @Test
    public void canStunAnEnemyInRange() {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Buster> enemyList = Arrays.asList(
                new Buster(1, 10000, 0, 0, 0, -1),
                new Buster(1, 1500, 900, 0, 1, 1));
        buster.visibleEnemies = enemyList;
        Buster enemy = buster.shouldStunAnEnemy();
        assertThat(enemy).isEqualTo(enemyList.get(1));
    }

    @Test
    public void canStunAnEnemyNoOneInRange() {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        List<Buster> enemyList = Arrays.asList(
                new Buster(1, 10000, 0, 0, 0, -1),
                new Buster(1, 1600, 900, 0, 0, -1));
        buster.visibleEnemies = enemyList;
        Buster enemy = buster.shouldStunAnEnemy();
        assertThat(enemy).isNull();
    }

    @Test
    public void canStunAnEnemyNoOneVisible() {
        Buster buster = new Buster(1, 0, 0, 0, 0, -1);
        Buster enemy = buster.shouldStunAnEnemy();
        assertThat(enemy).isNull();
    }


    @Test
    public void getDestOptimalDistanceInHorizontalLine() {
        Buster buster = new Buster(1, 1000, 1000, 0, 0, 0);
        Ghost ghost = new Ghost(0, 2780, 1000, 0, 0, 0);
        int[] dest = buster.getDestOptimalDistance(ghost, GameParameters.CATCH_MAX_DISTANCE);
        assertThat(dest).isEqualTo(new int[]{1021, 1000});
    }

    @Test
    public void getDestOptimalDistanceInDiagonalLine() {
        Buster buster = new Buster(1, 1000, 1000, 0, 0, 0);
        Ghost ghost = new Ghost(0, 2000, 2450, 0, 0, 0);
        int[] dest = buster.getDestOptimalDistance(ghost, GameParameters.CATCH_MAX_DISTANCE);
        assertThat(dest).isEqualTo(new int[]{1001, 1002});
    }

    @Test
    public void getPointAtMinimalDistance() {
        Buster buster = new Buster(1, 1000, 1000, 0, 0, 0);
        int[] dest = buster.getPointAtMinimalDistance(new Ghost(0, 1000, 1500, 0, 0, 0), 600);

        assertThat(dest).isEqualTo(new int[]{1600, 1500});
    }

    @Test
    public void getPointAtMinimalDistanceFromBottomLeft() {
        Buster buster = new Buster(1, 16000, 9000, 0, 0, 0);
        int[] dest = buster.getPointAtMinimalDistance(new Ghost(0, 16000, 8500, 0, 0, 0), 600);

        assertThat(dest).isEqualTo(new int[]{15664, 8997});
    }
}