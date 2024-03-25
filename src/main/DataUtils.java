package main;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Option;
import model.vote.SimpleRankingVote;
import model.vote.SingleVote;
import model.vote.Vote;
import model.vote.WeightedVote;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataUtils {
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final List<Class<? extends Vote>> VOTE_TYPES = Arrays.asList(WeightedVote.class, SimpleRankingVote.class, SingleVote.class);

    public static void writeFile(@NotNull Path filepath, @Nullable String contents) throws IOException {
        Files.createDirectories(filepath.getParent());
        Files.deleteIfExists(filepath);
        if (contents == null) {
            Files.createFile(filepath);
        } else {
            Files.write(filepath, contents.getBytes());
        }
    }

    /** Serializes the given items into strings, joined with newlines. */
    @NotNull
    public static <T> String serializeItems(@Nullable Collection<T> items) {
        if (items == null) return "";

        try {
            StringJoiner joiner = new StringJoiner("\n");
            for (T item : items) {
                String s = mapper.writeValueAsString(item);
                joiner.add(s);
            }
            return joiner.toString();
        } catch (JsonProcessingException jpe) {
            System.out.println("error during serialization");
            throw new RuntimeException("Error processing JSON", jpe);
        }
    }

    /** @return the first successfully serialized item when iterating through the given types, or else <code>null</code> */
    @Nullable
    public static <T> T deserializeItem(@NotNull String input, @NotNull Iterable<Class<? extends T>> types) {
        try {
            for (Class<? extends T> type : types) {
                T item = mapper.readValue(input, type);
                if (item == null) {
                    continue;
                }
                return item;
            }
        } catch (JsonProcessingException jpe) {
            String message = "error deserializing item: " + input;
            System.out.println(message);
            throw new RuntimeException("Error processing JSON\n" + message, jpe);
        }

        return null;
    }

    /** @see DataUtils#deserializeItem(String, Iterable) */
    @Nullable
    public static <T> T deserializeItem(@NotNull String input, @NotNull Class<T> type) {
        return deserializeItem(input, Collections.singleton(type));
    }

    /** @see DataUtils#deserializeItem(String, Iterable) */
    @Nullable
    public static Vote deserializeVote(@NotNull String input) throws RuntimeException {
        return deserializeItem(input, VOTE_TYPES);
    }

    /** {@link DataUtils#deserializeVote(String) Deserialize} a Vote and coerce it into a {@link WeightedVote}. */
    @Nullable
    public static WeightedVote deserializeWeightedVote(@NotNull String input) {
        Vote vote = deserializeVote(input);
        if (vote == null) return null;
        if (vote instanceof WeightedVote weighted) return weighted;
        return WeightedVote.fromVote(vote);
    }

    /** @see DataUtils#deserializeItem(String, Class) */
    @Nullable
    public static Option deserializeOption(@NotNull String input) {
        return deserializeItem(input, Option.class);
    }

    /**
     * Deserialized each line of the given file using {@link DataUtils#deserializeStrings(List, Function)}.
     * @param filepath path to the file to deserialize
     * @param deserializer for interpreting each line of the given file
     * @return a list of types deserialized, one per file line
     * @param <T> the type to try to deserialize each line to
     * @throws IOException for errors during file read
     */
    @NotNull
    public static <T> List<T> deserializeFile(@NotNull Path filepath, @NotNull Function<String, T> deserializer) throws IOException {
        if (!Files.exists(filepath)) return new ArrayList<>();
        return deserializeStrings(Files.readAllLines(filepath), deserializer);
    }

    /**
     * @param input a list of strings to deserialize
     * @param deserializer for interpreting each of the given strings
     * @return a list with <code>null</code> items removed
     * @param <T> the type to try to deserialize each string to
     */
    @NotNull
    public static <T> List<T> deserializeStrings(@Nullable List<String> input, @NotNull Function<String, T> deserializer) {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);

        return input.stream()
                .map(deserializer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
