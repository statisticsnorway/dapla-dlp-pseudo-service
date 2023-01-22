package no.ssb.dlp.pseudo.service.sid;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SidReaderTest {

    @Test
    void testReadSidsFromFile() {
        String filePath = "src/test/resources/freg/freg_snrkat.txt";
        SidReader sidReader = new SidReader();
        final AtomicInteger count = new AtomicInteger();
        sidReader.readSidsFromFile(filePath).subscribe(
                // onNext
                sidItem -> {
                    assertThat(sidItem.getFnr()).isNotNull();
                    count.getAndIncrement();
                },

                // onError
                e -> {
                    throw new RuntimeException(e);
                },

                // onComplete
                () -> {
                    System.out.println("Done");
                    assertThat(count.get()).isEqualTo(13);
                }
        );
    }


}