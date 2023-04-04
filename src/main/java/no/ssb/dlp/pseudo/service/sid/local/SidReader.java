package no.ssb.dlp.pseudo.service.sid.local;

import com.univocity.parsers.fixed.FixedWidthFields;
import com.univocity.parsers.fixed.FixedWidthParserSettings;
import com.univocity.parsers.fixed.FixedWidthRoutines;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.InputStream;

@RequiredArgsConstructor
@Singleton
@Slf4j
public class SidReader {

    @SneakyThrows
    public void readSidsFromFile(String filePath, SidCache sidCache) {
        FileInputStream fis = new FileInputStream(filePath);
        readSidsFromFile(fis, sidCache);
    }

    public void readSidsFromFile(InputStream inputStream, SidCache sidCache) {
        FixedWidthRoutines routines = new FixedWidthRoutines(fixedWidthParserSettings());
        for (SidItem sidItem : routines.iterate(SidItem.class, inputStream, "UTF-8")) {
            sidCache.register(sidItem, true);
        }
        sidCache.markAsInitialized();
    }

    @SneakyThrows
    public Flowable<SidItem> readSidsFromFile(String filePath) {
        FileInputStream fis = new FileInputStream(filePath);
        return readSidsFromFile(fis);
    }

    private FixedWidthParserSettings fixedWidthParserSettings() {
        FixedWidthFields fields = new FixedWidthFields();
        for (SidMappingFileField f : SidMappingFileField.values()) { // Note: We assume that
            fields.addField(f.getOriginalName(), f.getLength());
        }

        FixedWidthParserSettings settings = new FixedWidthParserSettings(fields);
        settings.setSkipTrailingCharsUntilNewline(true);
        settings.setRecordEndsOnNewline(true);
        settings.setNumberOfRowsToSkip(1);

        return settings;
    }

    public Flowable<SidItem> readSidsFromFile(InputStream inputStream) {
        FixedWidthRoutines routines = new FixedWidthRoutines(fixedWidthParserSettings());

        return Flowable.create(emitter -> {
            try {
                for (SidItem sidItem: routines.iterate(SidItem.class, inputStream, "UTF-8")) {
                    emitter.onNext(sidItem);
                }
            }
            catch (Exception e) {
                emitter.onError(e);
            }

            emitter.onComplete();
        }, BackpressureStrategy.BUFFER);
    }

}
