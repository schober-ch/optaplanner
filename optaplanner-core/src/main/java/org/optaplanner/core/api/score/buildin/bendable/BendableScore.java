/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.api.score.buildin.bendable;

import java.util.Arrays;

import org.optaplanner.core.api.score.AbstractBendableScore;
import org.optaplanner.core.api.score.FeasibilityScore;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.impl.score.buildin.bendable.BendableScoreDefinition;

/**
 * This {@link Score} is based on n levels of int constraints.
 * The number of levels is bendable at configuration time.
 * <p>
 * This class is immutable.
 * <p>
 * The {@link #getHardLevelsSize()} and {@link #getSoftLevelsSize()} must be the same as in the
 * {@link BendableScoreDefinition} used.
 * @see Score
 */
public final class BendableScore extends AbstractBendableScore<BendableScore>
        implements FeasibilityScore<BendableScore> {

    /**
     * @param scoreString never null
     * @return never null
     */
    public static BendableScore parseScore(String scoreString) {
        String[][] scoreTokens = parseBendableScoreTokens(BendableScore.class, scoreString);
        int initScore = parseInitScore(BendableScore.class, scoreString, scoreTokens[0][0]);
        int[] hardScores = new int[scoreTokens[1].length];
        for (int i = 0; i < hardScores.length; i++) {
            hardScores[i] = parseLevelAsInt(BendableScore.class, scoreString, scoreTokens[1][i]);
        }
        int[] softScores = new int[scoreTokens[2].length];
        for (int i = 0; i < softScores.length; i++) {
            softScores[i] = parseLevelAsInt(BendableScore.class, scoreString, scoreTokens[2][i]);
        }
        return valueOfUninitialized(initScore, hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     * @param initScore see {@link Score#getInitScore()}
     * @param hardScores never null, never change that array afterwards: it must be immutable
     * @param softScores never null, never change that array afterwards: it must be immutable
     * @return never null
     */
    public static BendableScore valueOfUninitialized(int initScore, int[] hardScores, int[] softScores) {
        return new BendableScore(initScore, hardScores, softScores);
    }

    /**
     * Creates a new {@link BendableScore}.
     * @param hardScores never null, never change that array afterwards: it must be immutable
     * @param softScores never null, never change that array afterwards: it must be immutable
     * @return never null
     */
    public static BendableScore valueOf(int[] hardScores, int[] softScores) {
        return new BendableScore(0, hardScores, softScores);
    }

    public static BendableScore zero(int hardLevelsSize, int softLevelsSize) {
        return new BendableScore(0, new int[hardLevelsSize], new int[softLevelsSize]);
    }

    // ************************************************************************
    // Fields
    // ************************************************************************

    private final int[] hardScores;
    private final int[] softScores;

    /**
     * Private default constructor for default marshalling/unmarshalling of unknown frameworks that use reflection.
     * Such integration is always inferior to the specialized integration modules, such as
     * optaplanner-persistence-jpa, optaplanner-persistence-xstream, optaplanner-persistence-jaxb, ...
     */
    @SuppressWarnings("unused")
    private BendableScore() {
        super(Integer.MIN_VALUE);
        hardScores = null;
        softScores = null;
    }

    /**
     * @param initScore see {@link Score#getInitScore()}
     * @param hardScores never null
     * @param softScores never null
     */
    protected BendableScore(int initScore, int[] hardScores, int[] softScores) {
        super(initScore);
        this.hardScores = hardScores;
        this.softScores = softScores;
    }

    /**
     * @return not null, array copy because this class is immutable
     */
    public int[] getHardScores() {
        return Arrays.copyOf(hardScores, hardScores.length);
    }

    /**
     * @return not null, array copy because this class is immutable
     */
    public int[] getSoftScores() {
        return Arrays.copyOf(softScores, softScores.length);
    }

    @Override
    public int getHardLevelsSize() {
        return hardScores.length;
    }

    /**
     * @param hardLevel {@code 0 <= hardLevel <} {@link #getHardLevelsSize()}.
     * The {@code scoreLevel} is {@code hardLevel} for hard levels and {@code softLevel + hardLevelSize} for soft levels.
     * @return higher is better
     */
    public int getHardScore(int hardLevel) {
        return hardScores[hardLevel];
    }

    @Override
    public int getSoftLevelsSize() {
        return softScores.length;
    }

    /**
     * @param softLevel {@code 0 <= softLevel <} {@link #getSoftLevelsSize()}.
     * The {@code scoreLevel} is {@code hardLevel} for hard levels and {@code softLevel + hardLevelSize} for soft levels.
     * @return higher is better
     */
    public int getSoftScore(int softLevel) {
        return softScores[softLevel];
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public BendableScore toInitializedScore() {
        return initScore == 0 ? this : new BendableScore(0, hardScores, softScores);
    }

    @Override
    public BendableScore withInitScore(int newInitScore) {
        assertNoInitScore();
        return new BendableScore(newInitScore, hardScores, softScores);
    }

    @Override
    public int getLevelsSize() {
        return hardScores.length + softScores.length;
    }

    /**
     * @param level {@code 0 <= level <} {@link #getLevelsSize()}
     * @return higher is better
     */
    public int getHardOrSoftScore(int level) {
        if (level < hardScores.length) {
            return hardScores[level];
        } else {
            return softScores[level - hardScores.length];
        }
    }

    @Override
    public boolean isFeasible() {
        if (initScore < 0) {
            return false;
        }
        for (int hardScore : hardScores) {
            if (hardScore < 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public BendableScore add(BendableScore augment) {
        validateCompatible(augment);
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] + augment.getHardScore(i);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] + augment.getSoftScore(i);
        }
        return new BendableScore(
                initScore + augment.getInitScore(),
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore subtract(BendableScore subtrahend) {
        validateCompatible(subtrahend);
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = hardScores[i] - subtrahend.getHardScore(i);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = softScores[i] - subtrahend.getSoftScore(i);
        }
        return new BendableScore(
                initScore - subtrahend.getInitScore(),
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore multiply(double multiplicand) {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(hardScores[i] * multiplicand);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(softScores[i] * multiplicand);
        }
        return new BendableScore(
                (int) Math.floor(initScore * multiplicand),
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore divide(double divisor) {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(hardScores[i] / divisor);
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(softScores[i] / divisor);
        }
        return new BendableScore(
                (int) Math.floor(initScore / divisor),
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore power(double exponent) {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = (int) Math.floor(Math.pow(hardScores[i], exponent));
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = (int) Math.floor(Math.pow(softScores[i], exponent));
        }
        return new BendableScore(
                (int) Math.floor(Math.pow(initScore, exponent)),
                newHardScores, newSoftScores);
    }

    @Override
    public BendableScore negate() {
        int[] newHardScores = new int[hardScores.length];
        int[] newSoftScores = new int[softScores.length];
        for (int i = 0; i < newHardScores.length; i++) {
            newHardScores[i] = - hardScores[i];
        }
        for (int i = 0; i < newSoftScores.length; i++) {
            newSoftScores[i] = - softScores[i];
        }
        return new BendableScore(-initScore, newHardScores, newSoftScores);
    }

    @Override
    public Number[] toLevelNumbers() {
        Number[] levelNumbers = new Number[hardScores.length + softScores.length];
        for (int i = 0; i < hardScores.length; i++) {
            levelNumbers[i] = hardScores[i];
        }
        for (int i = 0; i < softScores.length; i++) {
            levelNumbers[hardScores.length + i] = softScores[i];
        }
        return levelNumbers;
    }

    @Override
    public boolean equals(Object o) {
        // A direct implementation (instead of EqualsBuilder) to avoid dependencies
        if (this == o) {
            return true;
        } else if (o instanceof BendableScore) {
            BendableScore other = (BendableScore) o;
            if (getHardLevelsSize() != other.getHardLevelsSize()
                    || getSoftLevelsSize() != other.getSoftLevelsSize()) {
                return false;
            }
            if (initScore != other.getInitScore()) {
                return false;
            }
            for (int i = 0; i < hardScores.length; i++) {
                if (hardScores[i] != other.getHardScore(i)) {
                    return false;
                }
            }
            for (int i = 0; i < softScores.length; i++) {
                if (softScores[i] != other.getSoftScore(i)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        // A direct implementation (instead of HashCodeBuilder) to avoid dependencies
        int hashCode = (17 * 37) + initScore;
        hashCode = (37 * hashCode) + Arrays.hashCode(hardScores);
        hashCode = (37 * hashCode) + Arrays.hashCode(softScores);
        return hashCode;
    }

    @Override
    public int compareTo(BendableScore other) {
        // A direct implementation (instead of CompareToBuilder) to avoid dependencies
        validateCompatible(other);
        if (initScore != other.getInitScore()) {
            return initScore < other.getInitScore() ? -1 : 1;
        }
        for (int i = 0; i < hardScores.length; i++) {
            if (hardScores[i] != other.getHardScore(i)) {
                return hardScores[i] < other.getHardScore(i) ? -1 : 1;
            }
        }
        for (int i = 0; i < softScores.length; i++) {
            if (softScores[i] != other.getSoftScore(i)) {
                return softScores[i] < other.getSoftScore(i) ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public String toShortString() {
        return buildBendableShortString((n) -> ((Integer) n).intValue() != 0);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(((hardScores.length + softScores.length) * 4) + 13);
        s.append(getInitPrefix());
        s.append("[");
        boolean first = true;
        for (int hardScore : hardScores) {
            if (first) {
                first = false;
            } else {
                s.append("/");
            }
            s.append(hardScore);
        }
        s.append("]hard/[");
        first = true;
        for (int softScore : softScores) {
            if (first) {
                first = false;
            } else {
                s.append("/");
            }
            s.append(softScore);
        }
        s.append("]soft");
        return s.toString();
    }

    public void validateCompatible(BendableScore other) {
        if (getHardLevelsSize() != other.getHardLevelsSize()) {
            throw new IllegalArgumentException("The score (" + this
                    + ") with hardScoreSize (" + getHardLevelsSize()
                    + ") is not compatible with the other score (" + other
                    + ") with hardScoreSize (" + other.getHardLevelsSize() + ").");
        }
        if (getSoftLevelsSize() != other.getSoftLevelsSize()) {
            throw new IllegalArgumentException("The score (" + this
                    + ") with softScoreSize (" + getSoftLevelsSize()
                    + ") is not compatible with the other score (" + other
                    + ") with softScoreSize (" + other.getSoftLevelsSize() + ").");
        }
    }

    @Override
    public boolean isCompatibleArithmeticArgument(Score otherScore) {
        if (!(otherScore instanceof BendableScore)) {
            return false;
        }
        BendableScore otherBendableScore = (BendableScore) otherScore;
        return hardScores.length == otherBendableScore.hardScores.length
                && softScores.length == otherBendableScore.softScores.length;
    }

}
