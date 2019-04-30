package com.example.v2x_safety.entity;


public class decisionClass {

    private String mode;

    private static final int PROX_CLOSE = 0;
    private static final int PROX_FAR = 8;
    private static final int LIGHT_TH = 0;
    private static final float ACC_WALK = 2;
    private static final float ACC_RUNN = 1;
    private static final float ACC_BIKI = 3;
    private static final float MIN_VEL_WALK = 4;
    private static final float MAX_VEL_WALK = 10;
    private static final float MIN_VEL_RUNN = 8;
    private static final float MAX_VEL_RUNN = 20;
    private static final float MIN_VEL_BIK = 7;
    private static final float MAX_VEL_BIK = 30;

    public decisionClass(String mode){
        this.mode = mode;
    }

    // Public Methods, ModeActivity will used them at some point
    public int levelWarning(float acc, float vel, float prox, float light){
        int warning = 0;
        boolean checkAcc, checkVel, checkProx, checkLum, checkUse;
        // Take booleans with the behaviour
        checkAcc = checkAcceleration(acc);
        checkVel = checkVel(vel);
        checkProx = checkProximity(prox);
        checkLum = checkLight(light);
        checkUse = checkNormalUse(vel);
        /* RULES OF WARNING
        - Two triggers: change of acceleration (when normal use) and proximity to road (to implement): add +1 in warning
        - When the triggers act, two situations can happen, any of them will increment +1 in warning:
            - The speed is higher than expected.
            - The user is looking at the phone, it is is a biker we add another level the warning (+1)
        - If it is night, we will add +1.
         */
        if (checkAcc && checkUse) {
            if(checkProx || checkVel){
                if(mode.equals("Biking")){
                    warning = warning + 2;
                } else warning ++;
            }
            if(checkLum) warning++;
        }
        // To implement second trigger
        if(warning > 3) warning = 3;
        // Pass back the warning
        return warning;
    }

    public boolean checkAcceleration(float acc){
        boolean check = false;
        switch(mode){
            case "Walking":
                if(acc >= ACC_WALK) check = true;
                break;
            case "Running":
                if(acc >= ACC_RUNN) check = true;
                break;
            case "Biking":
                if(acc >= ACC_BIKI) check = true;
                break;
        }
        return check;
    }

    public boolean checkProximity(float prox){
        boolean check = true;
        switch ((int)prox){
            case PROX_CLOSE:
                check = false;
                break;
            case PROX_FAR:
                check = true;
                break;
        }
        return check;
    }

    // Private Methods - Used just in that class
    private boolean checkLight(float light){
        return light <= LIGHT_TH;
    }

    private boolean checkVel(float vel){
        boolean check = false;

        switch(mode){
            case "Walking":
                if(vel >= MAX_VEL_WALK) check = true;
                break;
            case "Running":
                if(vel >= MAX_VEL_RUNN) check = true;
                break;
            case "Biking":
                if(vel >= MAX_VEL_BIK) check = true;
        }
        return check;
    }

    private boolean checkNormalUse(float vel){
        boolean check = false;
        switch(mode){
            case "Walking":
                if(vel >= MIN_VEL_WALK) check = true;
                break;
            case "Running":
                if(vel >= MIN_VEL_RUNN) check = true;
                break;
            case "Biking":
                if(vel >= MIN_VEL_BIK) check = true;
        }
        return check;
    }
}
