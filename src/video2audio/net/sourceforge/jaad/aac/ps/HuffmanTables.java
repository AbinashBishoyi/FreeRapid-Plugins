/*
 *  Copyright (C) 2011 in-somnia
 * 
 *  This file is part of JAAD.
 * 
 *  JAAD is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 3 of the 
 *  License, or (at your option) any later version.
 *
 *  JAAD is distributed in the hope that it will be useful, but WITHOUT 
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General 
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.jaad.aac.ps;

interface HuffmanTables {

    int[][] HUFFMAN_IID_DEFAULT_DT = {
            {-31, 1},
            {-32, 2},
            {-30, 3},
            {-33, 4},
            {-29, 5},
            {-34, 6},
            {-28, 7},
            {-35, 8},
            {-27, 9},
            {-36, 10},
            {-26, 11},
            {-37, 12},
            {-25, 13},
            {-24, 14},
            {-38, 15},
            {16, 17},
            {-23, -39},
            {18, 19},
            {20, 21},
            {22, 23},
            {-22, -45},
            {-44, -43},
            {24, 25},
            {26, 27},
            {-42, -41},
            {-40, -21},
            {-20, -19},
            {-18, -17}
    };
    int[][] HUFFMAN_IID_DEFAULT_DF = {
            {-31, 1},
            {2, 3},
            {-30, -32},
            {4, 5},
            {-29, -33},
            {6, 7},
            {-28, -34},
            {8, 9},
            {-35, -27},
            {-26, 10},
            {-36, 11},
            {-25, 12},
            {-37, 13},
            {-38, 14},
            {-24, 15},
            {16, 17},
            {-23, -39},
            {18, 19},
            {-22, -21},
            {20, 21},
            {-40, -20},
            {22, 23},
            {-41, 24},
            {25, 26},
            {-42, -45},
            {-44, -43},
            {-19, 27},
            {-18, -17}
    };
    int[][] HUFFMAN_IID_FINE_DT = {
            {1, -31},
            {-30, 2},
            {3, -32},
            {4, 5},
            {6, 7},
            {-33, -29},
            {8, -34},
            {-28, 9},
            {-35, -27},
            {10, 11},
            {-26, 12},
            {13, 14},
            {-37, -25},
            {15, 16},
            {17, -36},
            {18, -38},
            {-24, 19},
            {20, 21},
            {-22, 22},
            {23, 24},
            {-39, -23},
            {25, 26},
            {-20, 27},
            {28, 29},
            {-41, -21},
            {30, 31},
            {32, -40},
            {33, -44},
            {-18, 34},
            {35, 36},
            {37, -43},
            {-19, 38},
            {39, -42},
            {40, 41},
            {42, 43},
            {44, 45},
            {46, -46},
            {-16, 47},
            {-45, -17},
            {48, 49},
            {-52, -51},
            {-13, -12},
            {-50, -49},
            {50, 51},
            {52, 53},
            {54, 55},
            {56, -48},
            {-14, 57},
            {58, -47},
            {-15, 59},
            {-57, -5},
            {-59, -58},
            {-2, -1},
            {-4, -3},
            {-61, -60},
            {-56, -6},
            {-55, -7},
            {-54, -8},
            {-53, -9},
            {-11, -10}
    };
    int[][] HUFFMAN_IID_FINE_DF = {
            {1, -31},
            {2, 3},
            {4, -32},
            {-30, 5},
            {-33, -29},
            {6, 7},
            {-34, -28},
            {8, 9},
            {-35, -27},
            {10, 11},
            {-36, -26},
            {12, 13},
            {-37, -25},
            {14, 15},
            {-24, 16},
            {17, 18},
            {19, -39},
            {-23, 20},
            {21, -38},
            {-21, 22},
            {23, -40},
            {-22, 24},
            {-42, -20},
            {25, 26},
            {27, -41},
            {28, -43},
            {-19, 29},
            {30, 31},
            {32, -45},
            {-17, 33},
            {34, -44},
            {-18, 35},
            {36, 37},
            {38, -46},
            {-16, 39},
            {40, 41},
            {42, 43},
            {-48, -14},
            {44, 45},
            {46, 47},
            {48, 49},
            {-47, -15},
            {-52, -10},
            {-50, -12},
            {-49, -13},
            {50, 51},
            {52, 53},
            {54, 55},
            {56, 57},
            {58, 59},
            {-57, -56},
            {-59, -58},
            {-53, -9},
            {-55, -54},
            {-6, -5},
            {-8, -7},
            {-2, -1},
            {-4, -3},
            {-61, -60},
            {-51, -11}
    };
    int[][] HUFFMAN_ICC_DT = {
            {-31, 1},
            {-30, 2},
            {-32, 3},
            {-29, 4},
            {-33, 5},
            {-28, 6},
            {-34, 7},
            {-27, 8},
            {-35, 9},
            {-26, 10},
            {-36, 11},
            {-25, 12},
            {-37, 13},
            {-38, -24}
    };
    int[][] HUFFMAN_ICC_DF = {
            {-31, 1},
            {-30, 2},
            {-32, 3},
            {-29, 4},
            {-33, 5},
            {-28, 6},
            {-34, 7},
            {-27, 8},
            {-26, 9},
            {-35, 10},
            {-25, 11},
            {-36, 12},
            {-24, 13},
            {-37, -38}
    };
    int[][] HUFFMAN_IPD_DT = {
            {1, -31},
            {2, 3},
            {4, 5},
            {-30, -24},
            {-26, 6},
            {-29, -25},
            {-27, -28}
    };
    int[][] HUFFMAN_IPD_DF = {
            {1, -31},
            {2, 3},
            {-30, 4},
            {5, 6},
            {-27, -26},
            {-28, -25},
            {-29, -24}
    };
    int[][] HUFFMAN_OPD_DT = {
            {1, -31},
            {2, 3},
            {4, 5},
            {-30, -24},
            {-26, -29},
            {-25, 6},
            {-27, -28}
    };
    int[][] HUFFMAN_OPD_DF = {
            {1, -31},
            {2, 3},
            {-24, -30},
            {4, 5},
            {-28, -25},
            {-29, 6},
            {-26, -27}
    };
}
