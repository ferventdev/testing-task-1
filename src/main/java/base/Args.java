package base;

import lombok.Getter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
//        try {
//            System.out.println("For check, parsed CLI parameters:");
//            String jsonString = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
//            System.out.println(jsonString + "\n\n");
//        } catch (JsonProcessingException e) {
//            System.out.println(e.getMessage());
//        }

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(filenames.size(), availableProcessors * 4));

        List<CompletableFuture<FileStats>> futures = filenames.stream()
                .map(filename -> CompletableFuture
                        .supplyAsync(() -> new ScrapTask(this, filename, words).call())
                        .exceptionally(ex -> {
                            // todo log smth
                            return null;
                        }))
                .collect(Collectors.toList());

        List<FileStats> fileStatsList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        fileStatsList.forEach(System.out::println);

        FileStats allFilesStats = fileStatsList.stream().reduce((fs1, fs2) -> {
            return FileStats.builder()
                    .filename("all files")
                    .charactersCount(fs1.getCharactersCount() + fs2.getCharactersCount())
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
                    .timeSpent(Math.max(fs1.getTimeSpent(), fs2.getTimeSpent()))
                    .build();
        }).get();

        System.out.println(allFilesStats);

        closePool(pool);
    }

    private void closePool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage()); // todo: maybe handled better
            pool.shutdownNow();
        }
    }
}
