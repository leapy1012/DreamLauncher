package com.coui.appcompat.springchain;


public interface ICOUIGridSpringChainViewGroup {

    public static final class DefaultImpls {
        public static float getLastTranslationY(ICOUIGridSpringChainViewGroup iCOUIGridSpringChainViewGroup) {
            COUIGridSpringChain springChain = iCOUIGridSpringChainViewGroup.getSpringChain();
            if (springChain != null) {
                return springChain.getLastTranslationY();
            }
            return 0.0f;
        }

        public static COUIGridSpringChain getSpringChain(ICOUIGridSpringChainViewGroup iCOUIGridSpringChainViewGroup) {
            return null;
        }

        public static int isSpringSystemRunning(ICOUIGridSpringChainViewGroup iCOUIGridSpringChainViewGroup) {
            COUIGridSpringChain springChain = iCOUIGridSpringChainViewGroup.getSpringChain();
            if (springChain != null) {
                return springChain.isSpringSystemRunning();
            }
            return 0;
        }

        public static void releaseSpring(ICOUIGridSpringChainViewGroup iCOUIGridSpringChainViewGroup) {
            COUIGridSpringChain springChain = iCOUIGridSpringChainViewGroup.getSpringChain();
            if (springChain != null) {
                springChain.releaseSpring();
            }
        }

        public static void startRebound(ICOUIGridSpringChainViewGroup iCOUIGridSpringChainViewGroup, int i2) {
            COUIGridSpringChain springChain = iCOUIGridSpringChainViewGroup.getSpringChain();
            if (springChain != null) {
                springChain.startRebound(i2);
            }
        }

        public static void updateMoveTranslation(ICOUIGridSpringChainViewGroup iCOUIGridSpringChainViewGroup, float f2, int i2) {
            COUIGridSpringChain springChain = iCOUIGridSpringChainViewGroup.getSpringChain();
            if (springChain != null) {
                springChain.updateMoveTranslation(f2, i2);
            }
        }
    }

    float getLastTranslationY();

    COUIGridSpringChain getSpringChain();

    int isSpringSystemRunning();

    void releaseSpring();

    void startRebound(int i2);

    void updateMoveTranslation(float f2, int i2);
}
