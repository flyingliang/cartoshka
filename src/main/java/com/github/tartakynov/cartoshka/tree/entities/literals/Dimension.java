package com.github.tartakynov.cartoshka.tree.entities.literals;

import com.github.tartakynov.cartoshka.scanners.TokenType;
import com.github.tartakynov.cartoshka.tree.entities.Literal;

public class Dimension extends Literal {
    private final double value;

    private final String unit;

    public Dimension(double value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public double getValue() {
        return value;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public Literal operate(TokenType operator) {
        return new Dimension(-value, unit);
    }

    @Override
    public Literal operate(TokenType operator, Literal operand) {
        if (operand.isDimension() || operand.isNumeric()) {
            Double left = this.getValue();
            Double right = null;
            if (operand.isDimension()) {
                Dimension dimension = (Dimension) operand;
                if (unit.equals(dimension.getUnit())) {
                    right = dimension.getValue();
                }
            } else {
                right = operand.toNumber();
            }

            if (right != null) {
                switch (operator) {
                    case ADD:
                        return new Dimension(left + right, unit);
                    case SUB:
                        return new Dimension(left - right, unit);
                    case MUL:
                        return new Dimension(left * right, unit);
                    case DIV:
                        return new Dimension(left / right, unit);
                    case MOD:
                        return new Dimension(left % right, unit);
                }
            }
        }

        return super.operate(operator, operand);
    }

    @Override
    public boolean isDimension() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("%s%s", Double.toString(value), unit);
    }
}