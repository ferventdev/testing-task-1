package base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

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
            // todo: possibly can be handled better
            return "{ \"error\": \"" + e.getMessage() + "\" }";
        }
    }
}
