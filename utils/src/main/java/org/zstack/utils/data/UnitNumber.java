package org.zstack.utils.data;

import java.util.Objects;

/**
 * An integer number with unit. example, 50GB (number=50, units="GB")
 * 
 * Created by Wenhao.Zhang on 23/05/18
 */
public class UnitNumber {
    public final long number;
    public final Double doubleFormatNumber;
    public final String units;

    public UnitNumber(long number, String units) {
        this.number = number;
        this.doubleFormatNumber = null;
        this.units = units;
    }

    public UnitNumber(double doubleFormatNumber, String units) {
        this.number = 0L;
        this.doubleFormatNumber = doubleFormatNumber;
        this.units = units;
    }

    /**
     * @param text example: 50G / -60TB ...
     */
    public static UnitNumber valueOf(String text) throws NumberFormatException {
        UnitNumber numeric = valueOfOrNull(text);
        if (numeric == null) {
            throw new NumberFormatException(text + "can not parsed to UnitNumeric");
        }
        return numeric;
    }

    public static UnitNumber valueOfWithFloatResultOrNull(String text) {
        return valueOfOrNull(text, true);
    }

    /**
     * @param text example: 50G / -60TB ...
     */
    public static UnitNumber valueOfOrNull(String text) {
        return valueOfOrNull(text, false);
    }

    public static UnitNumber valueOfOrNull(String text, boolean withFloat) {
        if (text.isEmpty()) {
            return null;
        }

        char[] chars = text.toCharArray();
        int i = 0;
        if (chars[0] == '-') {
            i++;
        }

        for (; i < chars.length; i++) {
            char ch = chars[i];
            if (!Character.isDigit(ch) && ch != '.') {
                break;
            }
        }

        if (i == 0 || chars[0] == '-' && i == 1) {
            return null;
        }

        if (withFloat) {
            return new UnitNumber(Double.parseDouble(text.substring(0, i)), text.substring(i).trim());
        } else {
            return new UnitNumber(Long.parseLong(text.substring(0, i)), text.substring(i).trim());
        }
    }

    public UnitNumber setNumber(long number) {
        return new UnitNumber(number, this.units);
    }

    @Override
    public String toString() {
        return number + units;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitNumber numeric = (UnitNumber) o;
        return number == numeric.number && Objects.equals(units, numeric.units);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, units);
    }
}
