package base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;

@Log4j2
@Getter
@RequiredArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileStats {

    public static final ObjectWriter PRETTY_PRINTER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private final String filename;
    private final Long charactersCount;
    private final Map<String, Long> wordsCount;
    private final Map<String, List<String>> sentencesExtracted;
    private final Long timeSpent;


    @Override
    public String toString() {
        try {
            return PRETTY_PRINTER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("FileStats for the file '{}' can't be serialized into JSON. Reason: {}", filename, e);
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}
