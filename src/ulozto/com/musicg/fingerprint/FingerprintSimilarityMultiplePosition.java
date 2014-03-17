/*
 * Copyright (C) 2012 Jacquet Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.musicg.fingerprint;

import com.musicg.properties.FingerprintProperties;

/**
 * A class for fingerprint's similarities
 *
 * @author jacquet
 */
public class FingerprintSimilarityMultiplePosition {

    private FingerprintProperties fingerprintProperties = FingerprintProperties.getInstance();
    private int[] mostSimilarFramePositions;
    private float[] scores;
    private float[] similarities;

    /**
     * Get the most similar position in terms of frame number
     *
     * @return most similar frame position
     */
    public int[] getMostSimilarFramePositions() {
        return mostSimilarFramePositions;
    }

    /**
     * Set the most similar position in terms of frame number
     *
     * @param mostSimilarFramePositions
     */
    public void setMostSimilarFramePositions(int[] mostSimilarFramePositions) {
        this.mostSimilarFramePositions = mostSimilarFramePositions;
    }

    /**
     * Get the similarities of the fingerprints
     * similarities from 0~1, which 0 means no similar feature is found and 1 means in average there is at least one match in every frame
     *
     * @return fingerprints similarities
     */
    public float[] getSimilarities() {
        return similarities;
    }

    /**
     * Set the similarities of the fingerprints
     *
     * @param fingerprints similarities
     */
    public void setSimilarities(float[] similarities) {
        this.similarities = similarities;
    }

    /**
     * Get the similarities scores of the fingerprints
     * Number of features found in the fingerprints per frame
     *
     * @return fingerprints similarities scores
     */
    public float[] getScores() {
        return scores;
    }

    /**
     * Set the similarities scores of the fingerprints
     *
     * @param scores
     */
    public void setScores(float[] scores) {
        this.scores = scores;
    }

    /**
     * Get the most similar position in terms of time in second
     *
     * @return most similar starting time
     */
    public float[] getsetMostSimilarTimePositions() {
        float[] ret = new float[mostSimilarFramePositions.length];
        for (int i = 0, length = mostSimilarFramePositions.length; i < length; i++) {
            ret[i] = (float) mostSimilarFramePositions[i] / fingerprintProperties.getNumRobustPointsPerFrame() / fingerprintProperties.getFps();
        }
        return ret;
    }
}