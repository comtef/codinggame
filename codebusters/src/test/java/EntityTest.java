import org.assertj.core.api.WithAssertions;
import org.junit.Test;

public class EntityTest implements WithAssertions {

    @Test
    public void distanceToSamePointEqualsZero() throws Exception {
        Entity entity = new Entity(1, 0, 0);
        assertThat(entity.distanceToPoint(0, 0)).isEqualTo(0);
    }

    @Test
    public void distanceToPointIsTruncated() throws Exception {
        Entity entity = new Entity(1, 0, 0);
        assertThat(entity.distanceToPoint(1000, 600)).isEqualTo(1166);
    }

    @Test
    public void distanceToPointIsTruncatedInvertedPoints() throws Exception {
        Entity entity = new Entity(1, 1000, 600);
        assertThat(entity.distanceToPoint(0, 0)).isEqualTo(1166);
    }

    @Test
    public void distanceToPointExactResult() throws Exception {
        Entity entity = new Entity(1, 0, 0);
        assertThat(entity.distanceToPoint(800, 600)).isEqualTo(1000);
    }

    @Test
    public void distanceToPointIsTruncatedNotRounded() throws Exception {
        Entity entity = new Entity(1, 0, 0);
        assertThat(entity.distanceToPoint(800, 698)).isEqualTo(1061);
    }
}