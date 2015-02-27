package rxquery;

import java.util.regex.Pattern;

public interface DataPattern {
    /**
     * Creates a pattern that could be used to match data sets received with {@link DataDescription#getDescription}.
     *
     * @return a compiled pattern.
     */
    Pattern getPattern();
}
