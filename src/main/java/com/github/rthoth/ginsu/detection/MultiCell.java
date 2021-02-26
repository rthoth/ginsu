package com.github.rthoth.ginsu.detection;

import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class MultiCell {

    public static final MultiCell EMPTY = new MultiCell(TreePVector.empty());

    private final PVector<Cell> cells;

    public MultiCell(PVector<Cell> cells) {
        this.cells = cells;
    }

    public static MultiCell empty() {
        return EMPTY;
    }

    public MultiCell plus(Cell cell) {
        return cell.nonEmpty() ? new MultiCell(cells.plus(cell)) : this;
    }
}
