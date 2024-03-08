package no.ssb.dlp.pseudo.service.sid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RedactorTest {

    @Test
    public void redactVariableStrings() {
        Assertions.assertNull(Redactor.redact(null, 4));
        Assertions.assertEquals("123", Redactor.redact("123", 4));
        Assertions.assertEquals("1234", Redactor.redact("1234", 4));
        Assertions.assertEquals("1234****", Redactor.redact("12345678", 4));
    }
    @Test
    public void redactFnr() {
        Assertions.assertEquals("118548*****", Redactor.redactFnr("11854898347"));
    }
    @Test
    public void redactSnr() {
        Assertions.assertEquals("0001***", Redactor.redactSnr("0001ha3"));
    }
}
