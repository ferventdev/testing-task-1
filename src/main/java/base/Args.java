package base;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Getter
public class Args implements Runnable {

    @Parameters(index = "0", arity = "1", split = ",", paramLabel = "WORD", description = "words that are searched")
    private List<String> words;

    @Parameters(index = "1..*", arity = "1..*", paramLabel = "FILE", description = "text files for search")
    private List<String> filenames;

    @Option(names = "-w", description = "if on, words occurrences in files are counted")
    private boolean wordsCounterEnabled = false;

    @Option(names = "-c", description = "if on, number of characters in files are counted")
    private boolean charsCounterEnabled = false;

    @Option(names = "-e", description = "if on, sentences with found words are extracted")
    private boolean extractionEnabled = false;

    @Option(names = "-v", description = "if on, than time spent of text scraping is recorded")
    private boolean verbosityEnabled = false;


    @Override
    public void run() {

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(filenames.size(), availableProcessors * 4));

        List<CompletableFuture<FileStats>> futures = filenames.stream()
                .map(filename -> CompletableFuture
                        .supplyAsync(() -> new ScrapTask(this, filename, words).call())
                        .exceptionally(ex -> {
                            log.error("An exception occurred during the ScrapTask for the file '{}': {}", filename, ex);
                            return null;
                        }))
                .collect(Collectors.toList());

        Instant startTime = Instant.now();

        List<FileStats> fileStatsList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        fileStatsList.forEach(oneFileStats -> log.info("File '{}' scrap summary:\n{}", oneFileStats.getFilename(), oneFileStats));

        // looks cool, but doesn't perform very well
        Optional<FileStats> allFilesStats = fileStatsList.stream().reduce((fs1, fs2) -> FileStats.builder()
                .filename("all files")
                .charactersCount(!charsCounterEnabled ? null : fs1.getCharactersCount() + fs2.getCharactersCount())
                .wordsCount(!wordsCounterEnabled ? null :
                        Stream.of(fs1.getWordsCount(), fs2.getWordsCount())
                                .flatMap(map -> map.entrySet().stream())
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (v1, v2) -> v1 + v2))
                )
                .sentencesExtracted(!isExtractionEnabled() ? null :
                        Stream.of(fs1.getSentencesExtracted(), fs2.getSentencesExtracted())
                                .flatMap(map -> map.entrySet().stream())
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()))
                                )
                )
                .timeSpent(!isVerbosityEnabled() ? null : Duration.between(startTime, Instant.now()).toMillis())
                .build()
        );

        allFilesStats.ifPresent(sumStats -> log.info("All files scrap summary:\n{}", sumStats));

        closePool(pool);
    }

    private void closePool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            log.error("Executor pool termination was interrupted", e);
            pool.shutdownNow();
        }
    }
}
