import org.assertj.core.api.WithAssertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class BusterTest implements WithAssertions {

    @Test
    public void isCloseEnoughToBaseTrueWhenOnBase() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(0, 0)).isTrue();
    }

    @Test
    public void isCloseEnoughToBaseFalseWhenOnMapOpposite() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(GameParameters.WIDTH, GameParameters.HEIGHT)).isFalse();
    }

    @Test
    public void isCloseEnoughToBaseFalseWhenOneDistanceTooFar() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(1 + GameParameters.RELEASE_MAX_DISTANCE, 0)).isFalse();
    }

    @Test
    public void isCloseEnoughToBaseTrueWhenAtMaximumPossibleDistance() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(GameParameters.RELEASE_MAX_DISTANCE, 0)).isTrue();
    }

    @Test
    public void isCloseEnoughToBaseTrueWhenCloseToMaximumPossibleDistance() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isCloseEnoughToBase(1249, 999)).isTrue();
    }

    @Test
    public void isInCatchRangeReturnsFalseIfTooFar() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 1800, 0, 0))).isFalse();
    }

    @Test
    public void isInCatchRangeReturnsFalseIfTooClose() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 10, 0, 0))).isFalse();
    }

    @Test
    public void isInCatchRangeReturnsTrueIfCloseEnough() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 1000, 600, 0))).isTrue();
    }

    @Test
    public void isInCatchRangeReturnsTrueIfJustInRangeCloseToMin() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 750, 500, 0))).isTrue();
    }

    @Test
    public void isInCatchRangeReturnsTrueIfJustInRangeCloseToMax() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        assertThat(buster.isInCatchRange(new Ghost(1, 1400, 1050, 0))).isTrue();
    }

    @Test
    public void canCatchAGhostReturnsNullIfNoGhosts() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList();
        Ghost ghost = buster.canCatchAGhost(ghosts);
        assertThat(ghost).isNull();
    }

    @Test
    public void canCatchAGhostReturnsGhostIfInRange() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 10000, 5000, 0),
                new Ghost(2, 1400, 1050, 0));
        Ghost ghost = buster.canCatchAGhost(ghosts);
        assertThat(ghost).isEqualTo(ghosts.get(1));
    }

    @Test
    public void canCatchAGhostReturnsNullIfNoGhostInRange() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 10000, 0, 0),
                new Ghost(2, 0, 0, 0));
        Ghost ghost = buster.canCatchAGhost(ghosts);
        assertThat(ghost).isNull();
    }

    @Test
    public void findClosestAvailableGhostReturnsNullIfNoGhosts() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList();
        List<Ghost> huntedGhosts = Arrays.asList();
        Ghost ghost = buster.findClosestAvailableGhost(ghosts, huntedGhosts);
        assertThat(ghost).isNull();
    }

    @Test
    public void findClosestAvailableGhostReturnsNullIfNoGhostAvailable() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 10000, 0, 0),
                new Ghost(2, 0, 0, 0));
        List<Ghost> huntedGhosts = Arrays.asList(
                new Ghost(1, 10000, 0, 0),
                new Ghost(2, 0, 0, 0));
        Ghost ghost = buster.findClosestAvailableGhost(ghosts, huntedGhosts);
        assertThat(ghost).isNull();
    }

    @Test
    public void findClosestAvailableGhostReturnsGhostIfAvailable() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 1000, 1000, 0),
                new Ghost(2, 500, 500, 0));
        List<Ghost> huntedGhosts = Arrays.asList(
                new Ghost(2, 500, 500, 0));
        Ghost ghost = buster.findClosestAvailableGhost(ghosts, huntedGhosts);
        assertThat(ghost).isEqualTo(ghosts.get(0));
    }

    @Test
    public void findClosestAvailableGhostReturnsClosestGhost() throws Exception {
        Buster buster = new Buster(1, 0, 0, -1);
        List<Ghost> ghosts = Arrays.asList(
                new Ghost(1, 1000, 1000, 0),
                new Ghost(2, 500, 500, 0),
                new Ghost(3, 750, 750, 0));
        List<Ghost> huntedGhosts = Arrays.asList(
                new Ghost(3, 750, 750, 0));
        Ghost ghost = buster.findClosestAvailableGhost(ghosts, huntedGhosts);
        assertThat(ghost).isEqualTo(ghosts.get(1));
    }
}