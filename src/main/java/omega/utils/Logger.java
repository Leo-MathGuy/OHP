package omega.utils;

import arc.util.Log;

public class Logger {
    public static void discLog(Object o){
        Log.info("<disc> @", o);
    }

    public static void discLog(Object ...objects){
        Log.info("<disc> ", objects);
    }

    public static void discLogWarn(Object o){
        Log.warn("<disc> @", o);
    }

    public static void discLogWarn(Object ...objects){
        Log.warn("<disc> ", objects);
    }

    public static void discLogErr(Object o){
        Log.info("<disc> @", o);
    }

    public static void discLogErr(Object ...objects){
        Log.info("<disc> ", objects);
    }

}
