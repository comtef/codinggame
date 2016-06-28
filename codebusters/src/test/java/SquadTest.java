import org.assertj.core.api.WithAssertions;
import org.junit.Test;

import java.util.Arrays;


public class SquadTest implements WithAssertions {

    @Test
    public void noOtherSquadCloserTrueIfNoOneCloser() {
        Squad squad0 = new Squad(0, AIParameters.SQUAD_TYPE_SEEKER);
        squad0.addMember(new Buster(0, 0, 0, 0, 0, 0));
        Squad squad1 = new Squad(1, AIParameters.SQUAD_TYPE_SEEKER);
        squad1.addMember(new Buster(1, 500, 3000, 0, 0, 0));
        GameState gameState = new GameState(0, 0, 2, 1);
        gameState.setSquads(Arrays.asList(squad0, squad1));
        Ghost ghost = new Ghost(0, 1000, 0, 0, 0, 0);
        int distance = 1000;
        boolean closer = squad0.noOtherSquadCloser(gameState, ghost, distance);
        assertThat(closer).isTrue();
    }

    @Test
    public void noOtherSquadCloserFalseIfOtherSquadCloser() {
        Squad squad0 = new Squad(0, AIParameters.SQUAD_TYPE_SEEKER);
        squad0.addMember(new Buster(0, 0, 0, 0, 0, 0));
        Squad squad1 = new Squad(1, AIParameters.SQUAD_TYPE_SEEKER);
        squad1.addMember(new Buster(1, 500, 0, 0, 0, 0));
        GameState gameState = new GameState(0, 0, 2, 1);
        gameState.setSquads(Arrays.asList(squad0, squad1));
        Ghost ghost = new Ghost(0, 1000, 0, 0, 0, 0);
        int distance = 1000;
        boolean closer = squad0.noOtherSquadCloser(gameState, ghost, distance);
        assertThat(closer).isFalse();
    }
}