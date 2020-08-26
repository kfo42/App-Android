package team.tangible.app.utils;

public class FloatUtils {
    public static boolean inRange(float coord, float init, float end){
        return (coord >= init) && (coord < end);
    }
}
