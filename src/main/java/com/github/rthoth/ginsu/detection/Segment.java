package com.github.rthoth.ginsu.detection;

import com.github.rthoth.ginsu.Slice;

final class Segment {
    final ProtoEvent origin;
    final ProtoEvent target;

    Segment(ProtoEvent origin, ProtoEvent target) {
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

    public void update(ProtoEvent.Type type) {
        origin.type = type;
        target.type = type;
    }
}
