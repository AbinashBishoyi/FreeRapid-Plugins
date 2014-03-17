package net.sourceforge.jaad.aac.sbr;

interface Constants {

    int[][] OFFSET = {
            {-8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7}, //16000
            {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 9, 11, 13}, //22050
            {-5, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 9, 11, 13, 16}, //24000
            {-6, -4, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 9, 11, 13, 16}, //32000
            {-4, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 9, 11, 13, 16, 20}, //44100-64000
            {-2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 9, 11, 13, 16, 20, 24} //>64000
    };
    int FIXFIX = 0;
    int FIXVAR = 1;
    int VARFIX = 2;
    int VARVAR = 3;
    int[] CEIL_LOG2 = {0, 1, 2, 2, 3, 3}; //CEIL_LOG2[i] = ceil(log(i+1)/log(2))
    int[] TIME_SLOTS = {16, 15}; //1024->16, 960->15
    int RATE = 2;
    int NOISE_FLOOR_OFFSET = 6;
    int[] PAN_OFFSET = {24, 12};
    int T_HF_GEN = 8;
    int T_HF_ADJ = 2;
}
