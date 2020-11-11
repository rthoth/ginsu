package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Slice;

final class Segment {
    final EventInfo origin;
    final EventInfo target;

    Segment(EventInfo origin, EventInfo target) {
        this.origin = origin;
        this.target = target;
    }

    public int product() {
        return origin.position * target.position;
    }

    public void update(Slice slice) {
        origin.update(slice);
        target.update(slice);
    }

    public void update(EventInfo.Type type) {
        origin.type = type;
        target.type = type;
    }
}
