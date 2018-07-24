package com.TrakEngineering.FluidSecureHub;

import android.app.Activity;

import java.util.HashMap;

/**
 * Created by Administrator on 7/5/2017.
 */

public class ActivityHandler{
    public static HashMap<Integer, Activity> screenStack;

    // Add activity
    public static void addActivities(int actNo, Activity _activity) {
        if (screenStack == null) {
            screenStack = new HashMap<Integer, Activity>();
        }

        if (_activity != null && !screenStack.containsKey(actNo))
            screenStack.put(actNo, _activity);
    }

    // Remove Activity
    public static void removeActivity(int key) {
        if (screenStack != null && screenStack.size() > 0) {
            Activity _activity = screenStack.get(key);
            if (_activity != null  && screenStack.containsKey(key))
            {
                _activity.finish();
                screenStack.remove(key);
            }
        }
    }

    //Getback to WelcomeActivity
    public static void GetBacktoWelcomeActivity(){

        try {
            ActivityHandler.removeActivity( 7);
            ActivityHandler.removeActivity( 6);
            ActivityHandler.removeActivity( 5);
            ActivityHandler.removeActivity( 4);
            ActivityHandler.removeActivity( 3);
            ActivityHandler.removeActivity( 2);
            ActivityHandler.removeActivity( 1);

        }catch (Exception e){
            System.out.println(e);
        }

    }

}