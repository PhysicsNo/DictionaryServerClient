import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Tester {

    InputStream sysInBackup = System.in; // backup System.in to restore it later
    ByteArrayInputStream in;

    @BeforeEach
    public void setUp() {
        String[] input = {"-d"};
        CSdict.main(input);
    }

    @AfterEach
    public void reset() {
        sysInBackup = System.in; // backup System.in to restore it later
        in = new ByteArrayInputStream("quit".getBytes());
        System.setIn(in);

        System.setIn(sysInBackup);
    }

    @Test
    public void testInput() {
        in = new ByteArrayInputStream("quit".getBytes());
        System.setIn(in);
    }

    @Test
    public void testCheckCommand() {

    }

    @Test
    public void testRunCommand() {

    }

    @Test
    public void testOpen() {

    }

    @Test
    public void testClose() {

    }

    @Test
    public void testQuit() {

    }

}

