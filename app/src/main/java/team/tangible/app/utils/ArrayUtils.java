package team.tangible.app.utils;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArrayUtils {
    public static <T> T[] joinDistinct(T[] arrayOne, T[] arrayTwo) {
        return Stream.concat(Arrays.stream(arrayOne), Arrays.stream(arrayTwo)).distinct().collect(Collectors.toList()).toArray(arrayOne);
    }
}
