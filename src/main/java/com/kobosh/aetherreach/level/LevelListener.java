package com.kobosh.aetherreach.level;

public interface LevelListener {
    void tileChanged(int x, int y, int z);
    void lightColumnChanged(int x, int z, int yMin, int yMax);
    void allChanged();
}
