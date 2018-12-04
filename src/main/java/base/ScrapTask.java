package base;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScrapTask implements Callable<FileStats> {

    // naive regexps
    private static final Pattern SENTENCE_DELIMITER = Pattern.compile("\\s*[.?!]+\\s+", Pattern.MULTILINE);
    private static final Pattern WORD_DELIMITER = Pattern.compile("\\s*[\\s,;:(){}<>'`\"]+\\s*", Pattern.MULTILINE);

    private final Args args;
    private final String filename;
    private final List<String> words;

    @Override
    public FileStats call() {

        try (Reader reader = Files.newBufferedReader(Paths.get(filename), StandardCharsets.UTF_8)) {

            Instant startTime = Instant.now();

            // saving to string
            String text = IOUtils.toString(reader);

            // processing..
            Map<String, Long> wordsCount = null;
            if (args.isWordsCounterEnabled()) {
                wordsCount = words.stream()
                        .collect(Collectors.toMap(Function.identity(), word -> 0L));
            }

            Map<String, List<String>> sentencesExtracted = null;
            if (args.isExtractionEnabled()) {
                sentencesExtracted = words.stream()
                        .collect(Collectors.toMap(Function.identity(), word -> new ArrayList<>()));
            }

            if (args.isWordsCounterEnabled() || args.isExtractionEnabled()) {
                String[] sentences = SENTENCE_DELIMITER.split(text);

                // maybe can be rewritten with streams / forEach
                for (String sentence : sentences) {
                    String[] tokens = WORD_DELIMITER.split(sentence);
                    for (String word : words) {

                        long countOccurences = Arrays.stream(tokens).filter(token -> token.equalsIgnoreCase(word)).count();

                        if (countOccurences > 0) {
                            if (args.isExtractionEnabled()) {
                                sentencesExtracted.compute(word, (w, list) -> {
                                    list.add(sentence);
                                    return list;
                                });
                            }
                            if (args.isWordsCounterEnabled()) {
                                wordsCount.compute(word, (w, count) -> count + countOccurences);
                            }
                        }
                    }
                }
            }

            Instant finishTime = Instant.now();

            return FileStats.builder()
                    .filename(filename)
                    .charactersCount(args.isCharsCounterEnabled() ? (long) text.codePointCount(0, text.length()) : null)
                    .wordsCount(wordsCount)
                    .sentencesExtracted(sentencesExtracted)
                    .timeSpent(args.isVerbosityEnabled() ? Duration.between(startTime, finishTime).toMillis() : null)
                    .build();

        } catch (IOException e) {
            // todo log
            System.out.println(e.getMessage());
            return null;
        }
    }
}
