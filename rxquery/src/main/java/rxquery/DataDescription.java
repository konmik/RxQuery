package rxquery;

public interface DataDescription {
    /**
     * Creates a string containing entire data set. This string could be used for matching with
     * pattern, returned by {@link DataPattern#getPattern}
     *
     * @return a string containing entire data set.
     */
    String getDescription();
}
